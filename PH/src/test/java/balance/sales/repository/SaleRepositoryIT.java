package balance.sales.repository;

import balance.model.Store;
import balance.repository.StoreRepository;
import balance.sales.model.Sale;
import balance.sales.model.Shift;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(locations = "classpath:application-test.properties")
class SaleRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired SaleRepository  saleRepository;
    @Autowired ShiftRepository shiftRepository;
    @Autowired StoreRepository storeRepository;

    private Store store;
    private Shift shift;

    @BeforeEach
    void setup() {
        saleRepository.deleteAll();
        shiftRepository.deleteAll();
        storeRepository.deleteAll();

        store = new Store();
        store.setName("Danli Test");
        store = storeRepository.save(store);

        shift = new Shift();
        shift.setStore(store);
        shift.setUsername("cajero01");
        shift.setStatus("OPEN");
        shift.setCode("T-20260514-0900-DAN");
        shift = shiftRepository.save(shift);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Sale saveSale(String status, BigDecimal total) {
        Sale sale = new Sale();
        sale.setShift(shift);
        sale.setStore(store);
        sale.setUsername("cajero01");
        sale.setSaleDate(LocalDate.now());
        sale.setStatus(status);
        sale.setSubtotal(total);
        sale.setIsv(total.multiply(new BigDecimal("0.15")).setScale(2, java.math.RoundingMode.HALF_UP));
        sale.setTotal(total.add(sale.getIsv()));
        return saleRepository.save(sale);
    }

    // ── findOpenByShiftId (@Query custom) ─────────────────────────────────────

    @Test
    void findOpenByShiftId_returnsOnlyOpenSales() {
        saveSale("OPEN",      new BigDecimal("200.00"));
        saveSale("OPEN",      new BigDecimal("150.00"));
        saveSale("CONFIRMED", new BigDecimal("100.00")); // no debe aparecer

        List<Sale> result = saleRepository.findOpenByShiftId(shift.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s -> "OPEN".equals(s.getStatus()));
    }

    @Test
    void findOpenByShiftId_returnsEmptyWhenAllSalesConfirmed() {
        saveSale("CONFIRMED", new BigDecimal("200.00"));
        saveSale("CONFIRMED", new BigDecimal("150.00"));

        List<Sale> result = saleRepository.findOpenByShiftId(shift.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findOpenByShiftId_returnsEmptyWhenNoSales() {
        List<Sale> result = saleRepository.findOpenByShiftId(shift.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findOpenByShiftId_onlyReturnsFromRequestedShift() {
        // Crear segundo turno con venta OPEN
        Shift otroTurno = new Shift();
        otroTurno.setStore(store);
        otroTurno.setUsername("cajero02");
        otroTurno.setStatus("OPEN");
        otroTurno.setCode("T-20260514-1000-DAN");
        otroTurno = shiftRepository.save(otroTurno);

        Sale saleOtro = new Sale();
        saleOtro.setShift(otroTurno);
        saleOtro.setStore(store);
        saleOtro.setUsername("cajero02");
        saleOtro.setSaleDate(LocalDate.now());
        saleOtro.setStatus("OPEN");
        saleOtro.setSubtotal(new BigDecimal("100.00"));
        saleOtro.setIsv(new BigDecimal("15.00"));
        saleOtro.setTotal(new BigDecimal("115.00"));
        saleRepository.save(saleOtro);

        // El turno principal no tiene ventas
        List<Sale> result = saleRepository.findOpenByShiftId(shift.getId());

        assertThat(result).isEmpty();
    }

    // ── countOpenByShiftId ────────────────────────────────────────────────────

    @Test
    void countOpenByShiftId_returnsCorrectCount() {
        saveSale("OPEN",      new BigDecimal("200.00"));
        saveSale("OPEN",      new BigDecimal("150.00"));
        saveSale("CONFIRMED", new BigDecimal("100.00"));

        long count = saleRepository.countOpenByShiftId(shift.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countOpenByShiftId_returnsZeroWhenNoOpenSales() {
        saveSale("CONFIRMED", new BigDecimal("200.00"));

        long count = saleRepository.countOpenByShiftId(shift.getId());

        assertThat(count).isZero();
    }

    // ── findByShiftIdOrderByCreatedAtDesc ─────────────────────────────────────

    @Test
    void findByShiftIdOrderByCreatedAtDesc_returnsAllSalesRegardlessOfStatus() {
        saveSale("OPEN",      new BigDecimal("200.00"));
        saveSale("CONFIRMED", new BigDecimal("150.00"));

        List<Sale> result = saleRepository.findByShiftIdOrderByCreatedAtDesc(shift.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    void findByShiftIdOrderByCreatedAtDesc_returnsEmptyForUnknownShift() {
        List<Sale> result = saleRepository.findByShiftIdOrderByCreatedAtDesc(9999L);

        assertThat(result).isEmpty();
    }

    // ── findByShiftIdAndStatus ────────────────────────────────────────────────

    @Test
    void findByShiftIdAndStatus_filtersCorrectly() {
        saveSale("OPEN",      new BigDecimal("200.00"));
        saveSale("OPEN",      new BigDecimal("150.00"));
        saveSale("CONFIRMED", new BigDecimal("100.00"));

        List<Sale> open      = saleRepository.findByShiftIdAndStatus(shift.getId(), "OPEN");
        List<Sale> confirmed = saleRepository.findByShiftIdAndStatus(shift.getId(), "CONFIRMED");

        assertThat(open).hasSize(2);
        assertThat(confirmed).hasSize(1);
    }
}
