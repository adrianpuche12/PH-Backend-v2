package balance.dashboard.service;

import balance.catalog.repository.ProductRepository;
import balance.dashboard.dto.DashboardDTO;
import balance.dashboard.dto.StoreDashboardDTO;
import balance.inventory.repository.InventoryStockRepository;
import balance.model.Store;
import balance.repository.StoreRepository;
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

    private Shift buildShift(Long id, String code, String username) {
        Shift shift = new Shift();
        ReflectionTestUtils.setField(shift, "id", id);
        shift.setCode(code);
        shift.setUsername(username);
        shift.setStatus("OPEN");
        return shift;
    }

    private void stubEmptyStore(Long storeId) {
        when(shiftRepository.findByStoreIdAndStatusOrderByOpenedAtDesc(storeId, "OPEN")).thenReturn(List.of());
        when(stockRepository.countLowStockByStoreId(storeId)).thenReturn(0L);
        when(productRepository.countByStoreId(storeId)).thenReturn(0L);
        when(stockRepository.sumEstimatedValueByStoreId(storeId)).thenReturn(BigDecimal.ZERO);
    }

    // ── Sin locales activos ───────────────────────────────────────────────────

    @Test
    void getDashboard_returnsEmptyWhenNoActiveStores() {
        when(storeRepository.findAll()).thenReturn(List.of());

        DashboardDTO result = service.getDashboard();

        assertThat(result.getStores()).isEmpty();
        assertThat(result.getTotalSalesToday()).isZero();
        assertThat(result.getTotalAmountToday()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Local sin turno activo ────────────────────────────────────────────────

    @Test
    void getDashboard_buildsDTOForStoreWithoutActiveShift() {
        Store store = buildStore(1L, "Danlí");
        when(storeRepository.findAll()).thenReturn(List.of(store));
        stubEmptyStore(1L);
        when(stockRepository.countLowStockByStoreId(1L)).thenReturn(2L);
        when(saleRepository.countByStoreIdsAndSaleDate(anyList(), any(LocalDate.class))).thenReturn(0L);
        when(saleRepository.sumTotalByStoreIdsAndSaleDate(anyList(), any(LocalDate.class))).thenReturn(BigDecimal.ZERO);

        DashboardDTO result = service.getDashboard();

        assertThat(result.getStores()).hasSize(1);
        assertThat(result.getStores().get(0).isHasActiveShift()).isFalse();
        assertThat(result.getStores().get(0).getActiveShifts()).isEmpty();
        assertThat(result.getStores().get(0).getLowStockCount()).isEqualTo(2L);
    }

    // ── Local con turno activo y ventas ───────────────────────────────────────

    @Test
    void getDashboard_includesActiveShiftDataInStoreDTO() {
        Store store = buildStore(1L, "Danlí");
        Shift shift = buildShift(10L, "T-20260603-0900-DAN", "cajero01");

        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(shiftRepository.findByStoreIdAndStatusOrderByOpenedAtDesc(1L, "OPEN")).thenReturn(List.of(shift));
        when(saleRepository.countOpenByShiftId(10L)).thenReturn(1L);
        when(saleRepository.sumTotalOpenByShiftId(10L)).thenReturn(new BigDecimal("90.00"));
        when(stockRepository.countLowStockByStoreId(1L)).thenReturn(0L);
        when(productRepository.countByStoreId(1L)).thenReturn(5L);
        when(stockRepository.sumEstimatedValueByStoreId(1L)).thenReturn(new BigDecimal("1000.00"));
        when(saleRepository.countByStoreIdsAndSaleDate(anyList(), any(LocalDate.class))).thenReturn(1L);
        when(saleRepository.sumTotalByStoreIdsAndSaleDate(anyList(), any(LocalDate.class))).thenReturn(new BigDecimal("90.00"));

        DashboardDTO result = service.getDashboard();

        StoreDashboardDTO storeDto = result.getStores().get(0);
        assertThat(storeDto.isHasActiveShift()).isTrue();
        assertThat(storeDto.getActiveShifts()).hasSize(1);
        assertThat(storeDto.getActiveShifts().get(0).getCode()).isEqualTo("T-20260603-0900-DAN");
        assertThat(storeDto.getShiftSalesCount()).isEqualTo(1L);
        assertThat(storeDto.getShiftSalesTotal()).isEqualByComparingTo("90.00");
    }

    // ── Múltiples turnos en el mismo local ───────────────────────────────────

    @Test
    void getDashboard_handlesMultipleActiveShiftsForSameStore() {
        Store store = buildStore(1L, "Danlí");
        Shift shift1 = buildShift(10L, "T-20260603-0900-DAN", "cajero01");
        Shift shift2 = buildShift(11L, "T-20260603-0905-DAN", "cajero02");

        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(shiftRepository.findByStoreIdAndStatusOrderByOpenedAtDesc(1L, "OPEN")).thenReturn(List.of(shift1, shift2));
        when(saleRepository.countOpenByShiftId(10L)).thenReturn(1L);
        when(saleRepository.sumTotalOpenByShiftId(10L)).thenReturn(new BigDecimal("50.00"));
        when(saleRepository.countOpenByShiftId(11L)).thenReturn(1L);
        when(saleRepository.sumTotalOpenByShiftId(11L)).thenReturn(new BigDecimal("30.00"));
        when(stockRepository.countLowStockByStoreId(1L)).thenReturn(0L);
        when(productRepository.countByStoreId(1L)).thenReturn(0L);
        when(stockRepository.sumEstimatedValueByStoreId(1L)).thenReturn(BigDecimal.ZERO);
        when(saleRepository.countByStoreIdsAndSaleDate(anyList(), any(LocalDate.class))).thenReturn(2L);
        when(saleRepository.sumTotalByStoreIdsAndSaleDate(anyList(), any(LocalDate.class))).thenReturn(new BigDecimal("80.00"));

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

    // ── Múltiples locales — totales globales ──────────────────────────────────

    @Test
    void getDashboard_aggregatesTotalsAcrossAllStores() {
        Store s1 = buildStore(1L, "Danlí");
        Store s2 = buildStore(2L, "El Paraíso");

        when(storeRepository.findAll()).thenReturn(List.of(s1, s2));
        stubEmptyStore(1L);
        stubEmptyStore(2L);
        when(saleRepository.countByStoreIdsAndSaleDate(anyList(), any(LocalDate.class))).thenReturn(3L);
        when(saleRepository.sumTotalByStoreIdsAndSaleDate(anyList(), any(LocalDate.class))).thenReturn(new BigDecimal("150.00"));

        DashboardDTO result = service.getDashboard();

        assertThat(result.getTotalSalesToday()).isEqualTo(3L);
        assertThat(result.getTotalAmountToday()).isEqualByComparingTo("150.00");
    }

    // ── Solo locales activos ──────────────────────────────────────────────────

    @Test
    void getDashboard_ignoresInactiveStores() {
        Store active   = buildStore(1L, "Activo");
        Store inactive = buildStore(2L, "Inactivo");
        inactive.setActive(false);

        when(storeRepository.findAll()).thenReturn(List.of(active, inactive));
        stubEmptyStore(1L);
        when(saleRepository.countByStoreIdsAndSaleDate(anyList(), any(LocalDate.class))).thenReturn(0L);
        when(saleRepository.sumTotalByStoreIdsAndSaleDate(anyList(), any(LocalDate.class))).thenReturn(BigDecimal.ZERO);

        DashboardDTO result = service.getDashboard();

        assertThat(result.getStores()).hasSize(1);
        assertThat(result.getStores().get(0).getStoreName()).isEqualTo("Activo");
    }

    // ── sumTotalOpenByShiftId null (SUM sin filas) → no debe explotar ─────────

    @Test
    void getDashboard_handlesNullSumFromShiftWithNoSales() {
        Store store = buildStore(1L, "Danlí");
        Shift shift = buildShift(10L, "T-20260603-0900-DAN", "cajero01");

        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(shiftRepository.findByStoreIdAndStatusOrderByOpenedAtDesc(1L, "OPEN")).thenReturn(List.of(shift));
        when(saleRepository.countOpenByShiftId(10L)).thenReturn(0L);
        when(saleRepository.sumTotalOpenByShiftId(10L)).thenReturn(null);
        when(stockRepository.countLowStockByStoreId(1L)).thenReturn(0L);
        when(productRepository.countByStoreId(1L)).thenReturn(0L);
        when(stockRepository.sumEstimatedValueByStoreId(1L)).thenReturn(BigDecimal.ZERO);
        when(saleRepository.countByStoreIdsAndSaleDate(anyList(), any(LocalDate.class))).thenReturn(0L);
        when(saleRepository.sumTotalByStoreIdsAndSaleDate(anyList(), any(LocalDate.class))).thenReturn(BigDecimal.ZERO);

        DashboardDTO result = service.getDashboard();

        assertThat(result.getStores().get(0).getShiftSalesTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
