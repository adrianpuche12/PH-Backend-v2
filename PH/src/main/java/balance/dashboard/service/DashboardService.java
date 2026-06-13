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

    /**
     * Retorna el resumen global del sistema para el admin.
     * Usa zona horaria Honduras (UTC-6) para el cálculo de "hoy".
     */
    public DashboardDTO getDashboard() {
        LocalDate today = LocalDate.now(HONDURAS_TZ);

        List<Store> activeStores = storeRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .toList();

        List<StoreDashboardDTO> storeDTOs = activeStores.stream()
                .map(this::buildStoreDTO)
                .toList();

        // Totales reales del día: todas las ventas de hoy (turno abierto O cerrado)
        // Nota: findByStoreIdAndSaleDateOrderByCreatedAtDesc evita el problema de inferencia
        // de tipos NULL en PostgreSQL que tiene findByStoreIdAndDateRange con parámetros opcionales
        long totalSalesToday = activeStores.stream()
                .mapToLong(s -> saleRepository.findByStoreIdAndSaleDateOrderByCreatedAtDesc(s.getId(), today).size())
                .sum();
        BigDecimal totalAmountToday = activeStores.stream()
                .flatMap(s -> saleRepository.findByStoreIdAndSaleDateOrderByCreatedAtDesc(s.getId(), today).stream())
                .map(Sale::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardDTO(storeDTOs, totalSalesToday, totalAmountToday);
    }

    private StoreDashboardDTO buildStoreDTO(Store store) {
        StoreDashboardDTO dto = new StoreDashboardDTO();
        dto.setStoreId(store.getId());
        dto.setStoreName(store.getName());

        // Turnos abiertos (puede haber varios, uno por cajero)
        List<Shift> openShifts = shiftRepository.findByStoreIdAndStatusOrderByOpenedAtDesc(store.getId(), "OPEN");
        dto.setHasActiveShift(!openShifts.isEmpty());

        List<StoreDashboardDTO.ActiveShiftDTO> activeShifts = new ArrayList<>();
        long totalSalesCount = 0;
        BigDecimal totalSalesAmount = BigDecimal.ZERO;
        for (Shift shift : openShifts) {
            List<Sale> sales = saleRepository.findOpenByShiftId(shift.getId());
            long salesCount = sales.size();
            BigDecimal salesTotal = sales.stream()
                    .map(Sale::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            activeShifts.add(new StoreDashboardDTO.ActiveShiftDTO(
                    shift.getCode(), shift.getUsername(), shift.getOpenedAt(), salesCount, salesTotal));

            totalSalesCount += salesCount;
            totalSalesAmount = totalSalesAmount.add(salesTotal);
        }
        dto.setActiveShifts(activeShifts);
        dto.setShiftSalesCount(totalSalesCount);
        dto.setShiftSalesTotal(totalSalesAmount);

        // Inventario
        long lowStock = stockRepository.countLowStockByStoreId(store.getId());
        long totalProd = productRepository.findByStoreIdOrderByNameAsc(store.getId()).size();
        BigDecimal estimatedValue = stockRepository.findByStoreIdOrderByProductNameAsc(store.getId())
                .stream()
                .map(s -> s.getProduct().getPrice().multiply(BigDecimal.valueOf(s.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setLowStockCount(lowStock);
        dto.setTotalProducts(totalProd);
        dto.setEstimatedValue(estimatedValue);

        return dto;
    }
}
