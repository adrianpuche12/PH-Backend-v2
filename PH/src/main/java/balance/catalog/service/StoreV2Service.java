package balance.catalog.service;

import balance.catalog.dto.StoreRequestDTO;
import balance.catalog.dto.StoreResponseDTO;
import balance.catalog.repository.CategoryRepository;
import balance.catalog.repository.ProductRepository;
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

import java.util.List;
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
        return StoreResponseDTO.from(storeRepository.save(store));
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
