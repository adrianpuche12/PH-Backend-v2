package balance.dashboard.service;

import balance.catalog.repository.ProductRepository;
import balance.dashboard.dto.DashboardDTO;
import balance.dashboard.dto.StoreDashboardDTO;
import balance.inventory.repository.InventoryStockRepository;
import balance.model.Store;
import balance.repository.StoreRepository;
import balance.sales.model.Sale;
import balance.sales.model.Shift;
import balance.sales.repository.SaleRepository;
import balance.sales.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @InjectMocks private DashboardService service;

    @Mock private StoreRepository          storeRepository;
    @Mock private ShiftRepository          shiftRepository;
    @Mock private SaleRepository           saleRepository;
    @Mock private InventoryStockRepository stockRepository;
    @Mock private ProductRepository        productRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Store buildStore(Long id, String name) {
        Store s = new Store();
        s.setId(id);
        s.setName(name);
        s.setActive(true);
        return s;
    }

    private Shift buildShift(String code) {
        Shift shift = new Shift();
        shift.setCode(code);
        shift.setUsername("cajero01");
        shift.setStatus("OPEN");
        return shift;
    }

    private Sale buildSale(BigDecimal total) {
        Sale sale = new Sale();
        sale.setTotal(total);
        sale.setStatus("OPEN");
        return sale;
    }

    // ── getDashboard — stores vacíos ──────────────────────────────────────────

    @Test
    void getDashboard_returnsEmptyWhenNoActiveStores() {
        when(storeRepository.findAll()).thenReturn(List.of());

        DashboardDTO result = service.getDashboard();

        assertThat(result.getStores()).isEmpty();
        assertThat(result.getTotalSalesToday()).isZero();
        assertThat(result.getTotalAmountToday()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── getDashboard — tienda sin turno activo ────────────────────────────────

    @Test
    void getDashboard_buildsDTOForStoreWithoutActiveShift() {
        Store store = buildStore(1L, "Danlí");
        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(shiftRepository.findByStoreIdAndStatusOrderByOpenedAtDesc(1L, "OPEN")).thenReturn(List.of());
        when(saleRepository.findByStoreIdAndSaleDateOrderByCreatedAtDesc(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of());
        when(stockRepository.countLowStockByStoreId(1L)).thenReturn(2L);
        when(productRepository.findByStoreIdOrderByNameAsc(1L)).thenReturn(List.of());
        when(stockRepository.findByStoreIdOrderByProductNameAsc(1L)).thenReturn(List.of());

        DashboardDTO result = service.getDashboard();

        assertThat(result.getStores()).hasSize(1);
        assertThat(result.getStores().get(0).isHasActiveShift()).isFalse();
        assertThat(result.getStores().get(0).getActiveShifts()).isEmpty();
        assertThat(result.getStores().get(0).getLowStockCount()).isEqualTo(2L);
    }

    // ── getDashboard — tienda con turno activo y ventas ───────────────────────

    @Test
    void getDashboard_includesActiveShiftDataInStoreDTO() {
        Store store = buildStore(1L, "Danlí");
        Shift shift = buildShift("T-20260603-0900-DAN");
        ReflectionTestUtils.setField(shift, "id", 10L);
        Sale sale = buildSale(new BigDecimal("90.00"));

        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(shiftRepository.findByStoreIdAndStatusOrderByOpenedAtDesc(1L, "OPEN")).thenReturn(List.of(shift));
        when(saleRepository.findOpenByShiftId(10L)).thenReturn(List.of(sale));
        when(saleRepository.findByStoreIdAndSaleDateOrderByCreatedAtDesc(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(sale));
        when(stockRepository.countLowStockByStoreId(1L)).thenReturn(0L);
        when(productRepository.findByStoreIdOrderByNameAsc(1L)).thenReturn(List.of());
        when(stockRepository.findByStoreIdOrderByProductNameAsc(1L)).thenReturn(List.of());

        DashboardDTO result = service.getDashboard();

        assertThat(result.getStores().get(0).isHasActiveShift()).isTrue();
        assertThat(result.getStores().get(0).getActiveShifts()).hasSize(1);
        assertThat(result.getStores().get(0).getActiveShifts().get(0).getCode()).isEqualTo("T-20260603-0900-DAN");
        assertThat(result.getStores().get(0).getShiftSalesCount()).isEqualTo(1L);
        assertThat(result.getStores().get(0).getShiftSalesTotal()).isEqualByComparingTo("90.00");
    }

    // ── getDashboard — múltiples turnos abiertos en el mismo local ────────────

    @Test
    void getDashboard_handlesMultipleActiveShiftsForSameStore() {
        Store store = buildStore(1L, "Danlí");
        Shift shift1 = buildShift("T-20260603-0900-DAN");
        ReflectionTestUtils.setField(shift1, "id", 10L);
        shift1.setUsername("cajero01");
        Shift shift2 = buildShift("T-20260603-0905-DAN");
        ReflectionTestUtils.setField(shift2, "id", 11L);
        shift2.setUsername("cajero02");

        Sale sale1 = buildSale(new BigDecimal("50.00"));
        Sale sale2 = buildSale(new BigDecimal("30.00"));

        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(shiftRepository.findByStoreIdAndStatusOrderByOpenedAtDesc(1L, "OPEN")).thenReturn(List.of(shift1, shift2));
        when(saleRepository.findOpenByShiftId(10L)).thenReturn(List.of(sale1));
        when(saleRepository.findOpenByShiftId(11L)).thenReturn(List.of(sale2));
        when(saleRepository.findByStoreIdAndSaleDateOrderByCreatedAtDesc(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(sale1, sale2));
        when(stockRepository.countLowStockByStoreId(1L)).thenReturn(0L);
        when(productRepository.findByStoreIdOrderByNameAsc(1L)).thenReturn(List.of());
        when(stockRepository.findByStoreIdOrderByProductNameAsc(1L)).thenReturn(List.of());

        DashboardDTO result = service.getDashboard();

        StoreDashboardDTO storeDto = result.getStores().get(0);
        assertThat(storeDto.isHasActiveShift()).isTrue();
        assertThat(storeDto.getActiveShifts()).hasSize(2);
        assertThat(storeDto.getActiveShifts())
                .extracting(StoreDashboardDTO.ActiveShiftDTO::getUsername)
                .containsExactlyInAnyOrder("cajero01", "cajero02");
        assertThat(storeDto.getShiftSalesCount()).isEqualTo(2L);
        assertThat(storeDto.getShiftSalesTotal()).isEqualByComparingTo("80.00");
        assertThat(result.getTotalActiveShifts()).isEqualTo(2L);
    }

    // ── getDashboard — múltiples tiendas ──────────────────────────────────────

    @Test
    void getDashboard_aggregatesTotalsAcrossAllStores() {
        Store s1 = buildStore(1L, "Danlí");
        Store s2 = buildStore(2L, "El Paraíso");
        Sale sale1 = buildSale(new BigDecimal("100.00"));
        Sale sale2 = buildSale(new BigDecimal("50.00"));

        when(storeRepository.findAll()).thenReturn(List.of(s1, s2));
        when(shiftRepository.findByStoreIdAndStatusOrderByOpenedAtDesc(anyLong(), anyString())).thenReturn(List.of());
        when(saleRepository.findByStoreIdAndSaleDateOrderByCreatedAtDesc(eq(1L), any())).thenReturn(List.of(sale1));
        when(saleRepository.findByStoreIdAndSaleDateOrderByCreatedAtDesc(eq(2L), any())).thenReturn(List.of(sale2));
        when(stockRepository.countLowStockByStoreId(anyLong())).thenReturn(0L);
        when(productRepository.findByStoreIdOrderByNameAsc(anyLong())).thenReturn(List.of());
        when(stockRepository.findByStoreIdOrderByProductNameAsc(anyLong())).thenReturn(List.of());

        DashboardDTO result = service.getDashboard();

        assertThat(result.getTotalSalesToday()).isEqualTo(2L);
        assertThat(result.getTotalAmountToday()).isEqualByComparingTo("150.00");
    }

    // ── getDashboard — solo locales activos ───────────────────────────────────

    @Test
    void getDashboard_ignoresInactiveStores() {
        Store active   = buildStore(1L, "Activo");
        Store inactive = buildStore(2L, "Inactivo");
        inactive.setActive(false);

        when(storeRepository.findAll()).thenReturn(List.of(active, inactive));
        when(shiftRepository.findByStoreIdAndStatusOrderByOpenedAtDesc(1L, "OPEN")).thenReturn(List.of());
        when(saleRepository.findByStoreIdAndSaleDateOrderByCreatedAtDesc(eq(1L), any())).thenReturn(List.of());
        when(stockRepository.countLowStockByStoreId(1L)).thenReturn(0L);
        when(productRepository.findByStoreIdOrderByNameAsc(1L)).thenReturn(List.of());
        when(stockRepository.findByStoreIdOrderByProductNameAsc(1L)).thenReturn(List.of());

        DashboardDTO result = service.getDashboard();

        assertThat(result.getStores()).hasSize(1);
        assertThat(result.getStores().get(0).getStoreName()).isEqualTo("Activo");
    }
}
