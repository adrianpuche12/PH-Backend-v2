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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private static final ZoneId HONDURAS_TZ = ZoneId.of("America/Tegucigalpa");

    @Autowired private StoreRepository          storeRepository;
    @Autowired private ShiftRepository          shiftRepository;
    @Autowired private SaleRepository           saleRepository;
    @Autowired private InventoryStockRepository stockRepository;
    @Autowired private ProductRepository        productRepository;

    public DashboardDTO getDashboard() {
        LocalDate today = LocalDate.now(HONDURAS_TZ);

        List<Store> activeStores = storeRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .toList();

        List<Long> activeStoreIds = activeStores.stream().map(Store::getId).toList();

        List<StoreDashboardDTO> storeDTOs = activeStores.stream()
                .map(s -> buildStoreDTO(s, today))
                .toList();

        // Totales globales: 2 queries (COUNT + SUM) en vez de 2×N cargando objetos
        long totalSalesToday = activeStoreIds.isEmpty() ? 0
                : saleRepository.countByStoreIdsAndSaleDate(activeStoreIds, today);
        BigDecimal totalAmountToday = activeStoreIds.isEmpty() ? BigDecimal.ZERO
                : saleRepository.sumTotalByStoreIdsAndSaleDate(activeStoreIds, today);

        return new DashboardDTO(storeDTOs, totalSalesToday, totalAmountToday);
    }

    private StoreDashboardDTO buildStoreDTO(Store store, LocalDate today) {
        StoreDashboardDTO dto = new StoreDashboardDTO();
        dto.setStoreId(store.getId());
        dto.setStoreName(store.getName());

        List<Shift> openShifts = shiftRepository.findByStoreIdAndStatusOrderByOpenedAtDesc(store.getId(), "OPEN");
        dto.setHasActiveShift(!openShifts.isEmpty());

        List<StoreDashboardDTO.ActiveShiftDTO> activeShifts = new ArrayList<>();
        long totalSalesCount = 0;
        BigDecimal totalSalesAmount = BigDecimal.ZERO;
        for (Shift shift : openShifts) {
            // COUNT + SUM por turno en vez de cargar todos los objetos Sale
            long salesCount  = saleRepository.countOpenByShiftId(shift.getId());
            BigDecimal salesTotal = saleRepository.sumTotalOpenByShiftId(shift.getId());

            activeShifts.add(new StoreDashboardDTO.ActiveShiftDTO(
                    shift.getCode(), shift.getUsername(), shift.getOpenedAt(), salesCount, salesTotal));

            totalSalesCount  += salesCount;
            totalSalesAmount  = totalSalesAmount.add(salesTotal);
        }
        dto.setActiveShifts(activeShifts);
        dto.setShiftSalesCount(totalSalesCount);
        dto.setShiftSalesTotal(totalSalesAmount);

        // COUNT en DB — no carga productos en memoria
        long lowStock    = stockRepository.countLowStockByStoreId(store.getId());
        long totalProd   = productRepository.countByStoreId(store.getId());
        // SUM en DB — no carga stocks en memoria
        BigDecimal estimatedValue = stockRepository.sumEstimatedValueByStoreId(store.getId());

        dto.setLowStockCount(lowStock);
        dto.setTotalProducts(totalProd);
        dto.setEstimatedValue(estimatedValue);

        return dto;
    }
}
