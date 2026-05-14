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
import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    @Autowired private StoreRepository          storeRepository;
    @Autowired private ShiftRepository          shiftRepository;
    @Autowired private SaleRepository           saleRepository;
    @Autowired private InventoryStockRepository stockRepository;
    @Autowired private ProductRepository        productRepository;

    /**
     * Retorna el resumen global del sistema para el admin:
     * por cada local activo, muestra turno activo, ventas del turno y estado del inventario.
     */
    public DashboardDTO getDashboard() {
        List<Store> activeStores = storeRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .toList();

        List<StoreDashboardDTO> storeDTOs = activeStores.stream()
                .map(this::buildStoreDTO)
                .toList();

        return new DashboardDTO(storeDTOs);
    }

    private StoreDashboardDTO buildStoreDTO(Store store) {
        StoreDashboardDTO dto = new StoreDashboardDTO();
        dto.setStoreId(store.getId());
        dto.setStoreName(store.getName());

        // Turno activo
        Optional<Shift> activeShift = shiftRepository.findByStoreIdAndStatus(store.getId(), "OPEN");
        if (activeShift.isPresent()) {
            Shift shift = activeShift.get();
            dto.setHasActiveShift(true);
            dto.setShiftCode(shift.getCode());
            dto.setShiftUsername(shift.getUsername());
            dto.setShiftOpenedAt(shift.getOpenedAt());

            // Ventas del turno activo
            List<Sale> sales = saleRepository.findOpenByShiftId(shift.getId());
            dto.setShiftSalesCount(sales.size());
            BigDecimal total = sales.stream()
                    .map(Sale::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setShiftSalesTotal(total);
        } else {
            dto.setHasActiveShift(false);
            dto.setShiftSalesTotal(BigDecimal.ZERO);
        }

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
