package balance.catalog.service;

import balance.catalog.dto.StoreRequestDTO;
import balance.catalog.dto.StoreResponseDTO;
import balance.catalog.model.Category;
import balance.catalog.model.Product;
import balance.catalog.repository.CategoryRepository;
import balance.catalog.repository.ProductRepository;
import balance.inventory.model.InventoryStock;
import balance.inventory.repository.InventoryMovementRepository;
import balance.inventory.repository.InventoryStockRepository;
import balance.model.Store;
import balance.repository.ClosingDepositRepository;
import balance.repository.SalaryPaymentRepository;
import balance.repository.StoreRepository;
import balance.repository.SupplierPaymentRepository;
import balance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StoreV2Service {

    @Autowired private StoreRepository storeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private InventoryStockRepository inventoryStockRepository;
    @Autowired private InventoryMovementRepository inventoryMovementRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private ClosingDepositRepository closingDepositRepository;
    @Autowired private SupplierPaymentRepository supplierPaymentRepository;
    @Autowired private SalaryPaymentRepository salaryPaymentRepository;

    public List<StoreResponseDTO> findAll() {
        return storeRepository.findAll().stream().map(StoreResponseDTO::from).toList();
    }

    public List<StoreResponseDTO> findAllActive() {
        return storeRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .map(StoreResponseDTO::from).toList();
    }

    public Optional<StoreResponseDTO> findById(Long id) {
        return storeRepository.findById(id).map(StoreResponseDTO::from);
    }

    @Transactional
    public StoreResponseDTO create(StoreRequestDTO dto) {
        Store store = new Store();
        store.setName(dto.getName().trim());
        store.setAddress(dto.getAddress());
        store.setPhone(dto.getPhone());
        store.setActive(true);
        Store saved = storeRepository.save(store);

        if (dto.getSourceStoreId() != null) {
            cloneCatalog(dto.getSourceStoreId(), saved);
        }

        return StoreResponseDTO.from(saved);
    }

    /**
     * Copia toda la estructura de categorías y productos del local origen al local destino.
     * Los stocks se crean en cero — no se copian cantidades ni movimientos.
     */
    private void cloneCatalog(Long sourceStoreId, Store targetStore) {
        // 1. Clonar árbol de categorías; construir mapa oldId → newCategory
        Map<Long, Category> categoryMap = new HashMap<>();
        List<Category> roots = categoryRepository.findRootsByStoreId(sourceStoreId);
        for (Category root : roots) {
            cloneCategoryTree(root, null, targetStore, categoryMap);
        }

        // 2. Clonar productos con stock inicial en cero
        List<Product> sourceProducts = productRepository.findByStoreIdOrderByNameAsc(sourceStoreId);
        for (Product src : sourceProducts) {
            Product p = new Product();
            p.setName(src.getName());
            p.setSku(src.getSku());
            p.setType(src.getType());
            p.setPrice(src.getPrice());
            p.setMinStock(src.getMinStock());
            p.setActive(src.getActive());
            p.setDescription(src.getDescription());
            p.setStore(targetStore);
            if (src.getCategory() != null) {
                p.setCategory(categoryMap.get(src.getCategory().getId()));
            }
            Product savedProduct = productRepository.save(p);

            InventoryStock stock = new InventoryStock();
            stock.setProduct(savedProduct);
            stock.setStore(targetStore);
            stock.setQuantity(0);
            inventoryStockRepository.save(stock);
        }
    }

    /** Clona una categoría y sus hijos recursivamente; registra el mapeo oldId → newCategory. */
    private void cloneCategoryTree(Category source, Category newParent, Store targetStore,
                                   Map<Long, Category> categoryMap) {
        Category c = new Category();
        c.setName(source.getName());
        c.setDescription(source.getDescription());
        c.setActive(source.getActive());
        c.setDisplayOrder(source.getDisplayOrder());
        c.setParent(newParent);
        c.setStore(targetStore);
        Category saved = categoryRepository.save(c);
        categoryMap.put(source.getId(), saved);

        for (Category child : source.getChildren()) {
            cloneCategoryTree(child, saved, targetStore, categoryMap);
        }
    }

    @Transactional
    public Optional<StoreResponseDTO> update(Long id, StoreRequestDTO dto) {
        return storeRepository.findById(id).map(store -> {
            store.setName(dto.getName().trim());
            store.setAddress(dto.getAddress());
            store.setPhone(dto.getPhone());
            return StoreResponseDTO.from(storeRepository.save(store));
        });
    }

    @Transactional
    public Optional<StoreResponseDTO> toggle(Long id) {
        return storeRepository.findById(id).map(store -> {
            store.setActive(!Boolean.TRUE.equals(store.getActive()));
            return StoreResponseDTO.from(storeRepository.save(store));
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!storeRepository.existsById(id)) return false;

        // Bloquear si el local tiene historial operativo del sistema V1
        boolean hasHistory =
            !transactionRepository.findByStoreId(id).isEmpty() ||
            !closingDepositRepository.findByStoreIdOrderByDepositDateDesc(id).isEmpty() ||
            !supplierPaymentRepository.findByStoreIdOrderByPaymentDateDesc(id).isEmpty() ||
            !salaryPaymentRepository.findByStoreIdOrderBySalaryDateDesc(id).isEmpty();

        if (hasHistory) {
            throw new IllegalStateException("No se puede eliminar un local con historial de operaciones. Desactivalo en su lugar.");
        }

        // 1. Obtener todos los productos del local
        var products = productRepository.findByStoreIdOrderByNameAsc(id);

        // 2. Limpiar inventario de cada producto
        for (var product : products) {
            inventoryMovementRepository.deleteByProductId(product.getId());
            inventoryStockRepository.deleteByProductId(product.getId());
        }

        // 3. Eliminar productos
        productRepository.deleteAll(products);

        // 4. Eliminar categorías (cascade elimina subcategorías)
        var rootCategories = categoryRepository.findRootsByStoreId(id);
        categoryRepository.deleteAll(rootCategories);

        // 5. Eliminar el local
        storeRepository.deleteById(id);
        return true;
    }
}
