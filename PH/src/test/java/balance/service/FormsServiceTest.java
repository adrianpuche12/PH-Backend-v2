package balance.service;

import balance.dto.GastoAdminRequestDTO;
import balance.dto.GastoAdminResponseDTO;
import balance.model.*;
import balance.repository.*;
import balance.sales.repository.SaleRepository;
import balance.sales.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormsServiceTest {

    @Mock private ClosingDepositRepository  closingDepositRepository;
    @Mock private SupplierPaymentRepository supplierPaymentRepository;
    @Mock private SalaryPaymentRepository  salaryPaymentRepository;
    @Mock private GastoAdminRepository     gastoAdminRepository;
    @Mock private TransactionRepository    transactionRepository;
    @Mock private StoreRepository          storeRepository;
    @Mock private ShiftRepository          shiftRepository;
    @Mock private SaleRepository           saleRepository;
    @InjectMocks private FormsService service;

    // ── saveClosingDeposit ────────────────────────────────────────────────────

    @Test
    void saveClosingDeposit_setsDepositDateWhenNull() {
        ClosingDeposit deposit = new ClosingDeposit();
        deposit.setAmount(new BigDecimal("500.00"));
        when(closingDepositRepository.save(any())).thenReturn(deposit);

        service.saveClosingDeposit(deposit);

        assertThat(deposit.getDepositDate()).isEqualTo(LocalDate.now(ZoneId.of("America/Tegucigalpa")));
    }

    @Test
    void saveClosingDeposit_keepsExistingDepositDate() {
        LocalDate specific = LocalDate.of(2026, 6, 1);
        ClosingDeposit deposit = new ClosingDeposit();
        deposit.setDepositDate(specific);
        when(closingDepositRepository.save(any())).thenReturn(deposit);

        service.saveClosingDeposit(deposit);

        assertThat(deposit.getDepositDate()).isEqualTo(specific);
    }

    // ── saveSupplierPayment ───────────────────────────────────────────────────

    @Test
    void saveSupplierPayment_setsPaymentDateWhenNull() {
        SupplierPayment payment = new SupplierPayment();
        payment.setAmount(new BigDecimal("1000.00"));
        when(supplierPaymentRepository.save(any())).thenReturn(payment);

        service.saveSupplierPayment(payment);

        assertThat(payment.getPaymentDate()).isEqualTo(LocalDate.now(ZoneId.of("America/Tegucigalpa")));
    }

    // ── saveSalaryPayment ─────────────────────────────────────────────────────

    @Test
    void saveSalaryPayment_setsSalaryDateWhenNull() {
        SalaryPayment payment = new SalaryPayment();
        payment.setAmount(new BigDecimal("5000.00"));
        when(salaryPaymentRepository.save(any())).thenReturn(payment);

        service.saveSalaryPayment(payment);

        assertThat(payment.getSalaryDate()).isEqualTo(LocalDate.now(ZoneId.of("America/Tegucigalpa")));
    }

    // ── updateClosingDeposit ──────────────────────────────────────────────────

    @Test
    void updateClosingDeposit_updatesAmountAndUsername() {
        ClosingDeposit existing = buildClosingDeposit(1L, new BigDecimal("300.00"));
        when(closingDepositRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(closingDepositRepository.save(any())).thenReturn(existing);

        ClosingDeposit update = new ClosingDeposit();
        update.setAmount(new BigDecimal("400.00"));
        update.setUsername("cajero-nuevo");

        service.updateClosingDeposit(1L, update);

        assertThat(existing.getAmount()).isEqualByComparingTo("400.00");
        assertThat(existing.getUsername()).isEqualTo("cajero-nuevo");
    }

    @Test
    void updateClosingDeposit_throwsWhenNotFound() {
        when(closingDepositRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateClosingDeposit(99L, new ClosingDeposit()))
            .isInstanceOf(ResponseStatusException.class);
    }

    // ── deleteClosingDeposit ──────────────────────────────────────────────────

    @Test
    void deleteClosingDeposit_deletesWhenExists() {
        when(closingDepositRepository.existsById(1L)).thenReturn(true);

        service.deleteClosingDeposit(1L);

        verify(closingDepositRepository).deleteById(1L);
    }

    @Test
    void deleteClosingDeposit_throwsWhenNotFound() {
        when(closingDepositRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteClosingDeposit(99L))
            .isInstanceOf(ResponseStatusException.class);
        verify(closingDepositRepository, never()).deleteById(any());
    }

    // ── deleteSupplierPayment ─────────────────────────────────────────────────

    @Test
    void deleteSupplierPayment_throwsWhenNotFound() {
        when(supplierPaymentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteSupplierPayment(99L))
            .isInstanceOf(ResponseStatusException.class);
    }

    // ── deleteSalaryPayment ───────────────────────────────────────────────────

    @Test
    void deleteSalaryPayment_throwsWhenNotFound() {
        when(salaryPaymentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteSalaryPayment(99L))
            .isInstanceOf(ResponseStatusException.class);
    }

    // ── saveGastoAdmin ────────────────────────────────────────────────────────

    @Test
    void saveGastoAdmin_throwsWhenPercentagesDoNotSumTo100() {
        GastoAdminRequestDTO req = buildGastoRequest(new BigDecimal("1000"), 60, 30); // suma 90%

        assertThatThrownBy(() -> service.saveGastoAdmin(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("100%");
    }

    @Test
    void saveGastoAdmin_createsOneTransactionPerStore() {
        GastoAdminRequestDTO req = buildGastoRequest(new BigDecimal("1000"), 60, 40);
        GastoAdmin savedGA = new GastoAdmin();
        ReflectionTestUtils.setField(savedGA, "id", 1L);
        when(gastoAdminRepository.save(any())).thenReturn(savedGA);

        Store store1 = buildStore(1L, "Danli");
        Store store2 = buildStore(2L, "El Paraiso");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store1));
        when(storeRepository.findById(2L)).thenReturn(Optional.of(store2));

        Transaction tx = new Transaction();
        ReflectionTestUtils.setField(tx, "id", 1L);
        tx.setAmount(new BigDecimal("600.00"));
        tx.setType("expense");
        tx.setDate(LocalDate.now());
        tx.setDescription("test");
        when(transactionRepository.save(any())).thenReturn(tx);

        GastoAdminResponseDTO result = service.saveGastoAdmin(req);

        assertThat(result.getTransaccionesCreadas()).isEqualTo(2);
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    void saveGastoAdmin_calculatesCorrectAmountsPerStore() {
        GastoAdminRequestDTO req = buildGastoRequest(new BigDecimal("1000"), 60, 40);
        GastoAdmin savedGA = new GastoAdmin();
        ReflectionTestUtils.setField(savedGA, "id", 1L);
        when(gastoAdminRepository.save(any())).thenReturn(savedGA);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(storeRepository.findById(2L)).thenReturn(Optional.of(buildStore(2L, "El Paraiso")));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        Transaction tx = new Transaction();
        ReflectionTestUtils.setField(tx, "id", 1L);
        tx.setAmount(BigDecimal.TEN);
        tx.setType("expense");
        tx.setDate(LocalDate.now());
        tx.setDescription("test");
        when(transactionRepository.save(any())).thenReturn(tx);

        service.saveGastoAdmin(req);

        verify(transactionRepository, times(2)).save(captor.capture());
        List<Transaction> saved = captor.getAllValues();
        BigDecimal total = saved.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("1000.00"); // 600 + 400
    }

    @Test
    void saveGastoAdmin_throwsWhenStoreNotFound() {
        GastoAdminRequestDTO req = buildGastoRequest(new BigDecimal("1000"), 60, 40);
        GastoAdmin savedGA = new GastoAdmin();
        ReflectionTestUtils.setField(savedGA, "id", 1L);
        when(gastoAdminRepository.save(any())).thenReturn(savedGA);
        when(storeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveGastoAdmin(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Local no encontrado");
    }

    // ── getOperationsByUsername ───────────────────────────────────────────────

    @Test
    void getOperationsByUsername_aggregatesAllOperationTypes() {
        ClosingDeposit cd = buildClosingDeposit(1L, new BigDecimal("200.00"));
        SupplierPayment sp = new SupplierPayment();
        sp.setAmount(new BigDecimal("100.00"));
        sp.setPaymentDate(LocalDate.now());

        when(closingDepositRepository.findByUsernameOrderByDepositDateDesc("cajero01"))
            .thenReturn(List.of(cd));
        when(supplierPaymentRepository.findByUsernameOrderByPaymentDateDesc("cajero01"))
            .thenReturn(List.of(sp));
        when(salaryPaymentRepository.findByUsernameOrderBySalaryDateDesc("cajero01"))
            .thenReturn(List.of());

        var result = service.getOperationsByUsername("cajero01");

        assertThat(result).hasSize(2);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ClosingDeposit buildClosingDeposit(Long id, BigDecimal amount) {
        ClosingDeposit c = new ClosingDeposit();
        ReflectionTestUtils.setField(c, "id", id);
        c.setAmount(amount);
        c.setDepositDate(LocalDate.now());
        c.setUsername("cajero01");
        return c;
    }

    private Store buildStore(Long id, String name) {
        Store s = new Store();
        ReflectionTestUtils.setField(s, "id", id);
        s.setName(name);
        return s;
    }

    private GastoAdminRequestDTO buildGastoRequest(BigDecimal monto, int pct1, int pct2) {
        GastoAdminRequestDTO req = new GastoAdminRequestDTO();
        req.setMonto(monto);
        req.setFecha(LocalDate.now());
        req.setDescripcion("Gasto test");
        req.setTipo("expense");

        GastoAdminRequestDTO.StoreDistribucion d1 = new GastoAdminRequestDTO.StoreDistribucion();
        d1.setStoreId(1L);
        d1.setPorcentaje(pct1);

        GastoAdminRequestDTO.StoreDistribucion d2 = new GastoAdminRequestDTO.StoreDistribucion();
        d2.setStoreId(2L);
        d2.setPorcentaje(pct2);

        req.setDistribuciones(List.of(d1, d2));
        return req;
    }
}
