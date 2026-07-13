package balance.inventory.service;

import balance.catalog.model.Product;
import balance.catalog.model.ProductRecipeItem;
import balance.catalog.repository.CategoryRepository;
import balance.catalog.repository.ProductRecipeRepository;
import balance.catalog.repository.ProductRepository;
import balance.inventory.dto.*;
import balance.inventory.model.InventoryMovement;
import balance.inventory.model.InventoryStock;
import balance.inventory.repository.InventoryMovementRepository;
import balance.inventory.repository.InventoryStockRepository;
import balance.model.Store;
import balance.repository.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    @Autowired private InventoryStockRepository stockRepository;
    @Autowired private InventoryMovementRepository movementRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRecipeRepository recipeRepository;

    public List<StockItemDTO> getStock(Long storeId) {
        List<StockItemDTO> items = stockRepository.findSimpleByStoreIdOrderByProductNameAsc(storeId)
                .stream().map(StockItemDTO::from).collect(java.util.stream.Collectors.toList());

        productRepository.findByStoreIdAndTypeOrderByNameAsc(storeId, "FABRICATED").forEach(p -> {
            int qty = computeFabricatedQty(p.getId(), storeId);
            items.add(StockItemDTO.fromFabricated(p, qty, storeId));
        });

        items.sort(java.util.Comparator.comparing(StockItemDTO::getProductName,
                String.CASE_INSENSITIVE_ORDER));
        return items;
    }

    private int computeFabricatedQty(Long productId, Long storeId) {
        List<ProductRecipeItem> recipe = recipeRepository.findByProductIdWithIngredient(productId);
        if (recipe.isEmpty()) return 0;
        int min = Integer.MAX_VALUE;
        for (ProductRecipeItem ri : recipe) {
            InventoryStock ing = stockRepository
                    .findByProductIdAndStoreId(ri.getIngredient().getId(), storeId).orElse(null);
            BigDecimal available = (ing != null) ? ing.getQuantity() : BigDecimal.ZERO;
            int canMake = available.divide(ri.getQuantity(), 0, RoundingMode.FLOOR).intValue();
            min = Math.min(min, canMake);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
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
                .map(s -> s.getProduct().getPrice().multiply(s.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StockSummaryDTO(total, active, lowStock, cats, value);
    }

    @Transactional
    public StockItemDTO adjust(Long storeId, StockAdjustmentDTO dto) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Local no encontrado"));
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        if ("FABRICATED".equals(product.getType())) {
            throw new IllegalArgumentException("No se puede ajustar el stock de un producto fabricado");
        }

        InventoryStock stock = stockRepository
                .findByProductIdAndStoreId(dto.getProductId(), storeId)
                .orElseGet(() -> {
                    InventoryStock s = new InventoryStock();
                    s.setProduct(product);
                    s.setStore(store);
                    s.setQuantity(BigDecimal.ZERO);
                    return s;
                });

        BigDecimal delta = "SALIDA".equals(dto.getType()) ? dto.getQuantity().negate() : dto.getQuantity();
        BigDecimal newQty = stock.getQuantity().add(delta);
        if (newQty.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Stock insuficiente para realizar la salida");
        stock.setQuantity(newQty);
        stockRepository.save(stock);

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

    @Transactional
    public void adjustSilent(Long storeId, StockAdjustmentDTO dto) {
        try {
            adjust(storeId, dto);
        } catch (IllegalArgumentException e) {
            log.warn("Stock insuficiente (storeId={}, productId={}, qty={}): {}",
                    storeId, dto.getProductId(), dto.getQuantity(), e.getMessage());
        }
    }

    public List<MovementDTO> getMovements(Long storeId) {
        return movementRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
                .stream().map(MovementDTO::from).toList();
    }

    @Transactional
    public void initStock(Product product, Store store) {
        if ("FABRICATED".equals(product.getType())) return;
        if (stockRepository.findByProductIdAndStoreId(product.getId(), store.getId()).isEmpty()) {
            InventoryStock stock = new InventoryStock();
            stock.setProduct(product);
            stock.setStore(store);
            stock.setQuantity(BigDecimal.ZERO);
            stockRepository.save(stock);
        }
    }
}
