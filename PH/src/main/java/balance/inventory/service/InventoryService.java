package balance.inventory.service;

import balance.catalog.model.Product;
import balance.catalog.repository.CategoryRepository;
import balance.catalog.repository.ProductRepository;
import balance.inventory.dto.*;
import balance.inventory.model.InventoryMovement;
import balance.inventory.model.InventoryStock;
import balance.inventory.repository.InventoryMovementRepository;
import balance.inventory.repository.InventoryStockRepository;
import balance.model.Store;
import balance.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    @Autowired private InventoryStockRepository stockRepository;
    @Autowired private InventoryMovementRepository movementRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private CategoryRepository categoryRepository;

    // ── Stock ──────────────────────────────────────────────────────────────

    public List<StockItemDTO> getStock(Long storeId) {
        return stockRepository.findByStoreIdOrderByProductNameAsc(storeId)
                .stream().map(StockItemDTO::from).toList();
    }

    public List<StockItemDTO> getLowStock(Long storeId) {
        return stockRepository.findLowStockByStoreId(storeId)
                .stream().map(StockItemDTO::from).toList();
    }

    public StockSummaryDTO getSummary(Long storeId) {
        List<InventoryStock> stocks = stockRepository.findByStoreIdOrderByProductNameAsc(storeId);

        long total    = stocks.size();
        long active   = stocks.stream().filter(s -> Boolean.TRUE.equals(s.getProduct().getActive())).count();
        long lowStock = stockRepository.countLowStockByStoreId(storeId);
        long cats     = categoryRepository.findRootsByStoreId(storeId).size();

        BigDecimal value = stocks.stream()
                .map(s -> s.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(s.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StockSummaryDTO(total, active, lowStock, cats, value);
    }

    // ── Ajuste de stock ────────────────────────────────────────────────────

    @Transactional
    public StockItemDTO adjust(Long storeId, StockAdjustmentDTO dto) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Local no encontrado"));
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        InventoryStock stock = stockRepository
                .findByProductIdAndStoreId(dto.getProductId(), storeId)
                .orElseGet(() -> {
                    InventoryStock s = new InventoryStock();
                    s.setProduct(product);
                    s.setStore(store);
                    s.setQuantity(0);
                    return s;
                });

        int delta = "SALIDA".equals(dto.getType()) ? -dto.getQuantity() : dto.getQuantity();
        int newQty = stock.getQuantity() + delta;
        if (newQty < 0) throw new IllegalArgumentException("Stock insuficiente para realizar la salida");
        stock.setQuantity(newQty);
        stockRepository.save(stock);

        // Registrar movimiento
        InventoryMovement movement = new InventoryMovement();
        movement.setType(dto.getType());
        movement.setQuantity(dto.getQuantity());
        movement.setReason(dto.getReason());
        movement.setNotes(dto.getNotes());
        movement.setUsername(dto.getUsername());
        movement.setProduct(product);
        movement.setStore(store);
        movementRepository.save(movement);

        return StockItemDTO.from(stock);
    }

    // ── Movimientos ────────────────────────────────────────────────────────

    public List<MovementDTO> getMovements(Long storeId) {
        return movementRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
                .stream().map(MovementDTO::from).toList();
    }

    // ── Auto-crear stock al crear producto ─────────────────────────────────

    @Transactional
    public void initStock(Product product, Store store) {
        if (stockRepository.findByProductIdAndStoreId(product.getId(), store.getId()).isEmpty()) {
            InventoryStock stock = new InventoryStock();
            stock.setProduct(product);
            stock.setStore(store);
            stock.setQuantity(0);
            stockRepository.save(stock);
        }
    }
}
