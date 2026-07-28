package balance.catalog.service;

import balance.catalog.dto.ProductRequestDTO;
import balance.catalog.dto.ProductResponseDTO;
import balance.catalog.dto.RecipeItemDTO;
import balance.catalog.model.Product;
import balance.catalog.model.ProductRecipeItem;
import balance.catalog.repository.CategoryRepository;
import balance.catalog.repository.ProductRecipeRepository;
import balance.catalog.repository.ProductRepository;
import balance.inventory.repository.InventoryMovementRepository;
import balance.inventory.repository.InventoryStockRepository;
import balance.inventory.service.InventoryService;
import balance.model.Store;
import balance.repository.StoreRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Lazy
    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryStockRepository inventoryStockRepository;

    @Autowired
    private ProductRecipeRepository recipeRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @PersistenceContext
    private EntityManager em;

    public List<ProductResponseDTO> findByStore(Long storeId, Boolean active, Long categoryId, String search) {
        List<Product> products;

        if (search != null && !search.isBlank()) {
            products = productRepository.searchByStoreId(storeId, search.trim());
        } else if (categoryId != null) {
            products = productRepository.findByStoreIdAndCategoryIdOrderByNameAsc(storeId, categoryId);
        } else if (active != null) {
            products = productRepository.findByStoreIdAndActiveOrderByNameAsc(storeId, active);
        } else {
            products = productRepository.findByStoreIdOrderByNameAsc(storeId);
        }

        return products.stream().map(ProductResponseDTO::from).toList();
    }

    public Optional<ProductResponseDTO> findById(Long id) {
        return productRepository.findById(id).map(ProductResponseDTO::from);
    }

    @Transactional
    public Optional<ProductResponseDTO> create(Long storeId, ProductRequestDTO dto) {
        Optional<Store> store = storeRepository.findById(storeId);
        if (store.isEmpty()) return Optional.empty();

        if (dto.getSku() != null && !dto.getSku().isBlank()
                && productRepository.existsBySkuAndStoreId(dto.getSku().trim(), storeId)) {
            throw new IllegalArgumentException("Ya existe un producto con ese SKU en este local");
        }

        if (productRepository.existsByNameIgnoreCaseAndStoreId(dto.getName().trim(), storeId)) {
            throw new IllegalArgumentException(
                "Ya existe un producto llamado «" + dto.getName().trim() + "» en este local. Por favor, usá un nombre diferente.");
        }

        Product product = buildProduct(dto, store.get());
        Product saved = productRepository.save(product);
        inventoryService.initStock(saved, store.get());
        return Optional.of(ProductResponseDTO.from(saved));
    }

    @Transactional
    public Optional<ProductResponseDTO> update(Long id, ProductRequestDTO dto) {
        return productRepository.findById(id).map(product -> {
            if (dto.getSku() != null && !dto.getSku().isBlank()
                    && productRepository.existsBySkuAndStoreIdAndIdNot(
                        dto.getSku().trim(), product.getStore().getId(), id)) {
                throw new IllegalArgumentException("Ya existe un producto con ese SKU en este local");
            }
            if (productRepository.existsByNameIgnoreCaseAndStoreIdAndIdNot(
                    dto.getName().trim(), product.getStore().getId(), id)) {
                throw new IllegalArgumentException(
                    "Ya existe un producto llamado «" + dto.getName().trim() + "» en este local. Por favor, usá un nombre diferente.");
            }
            String oldType = product.getType();
            String newType = dto.getType() != null ? dto.getType() : "SIMPLE";
            applyDTO(product, dto);
            Product saved = productRepository.save(product);
            if (!oldType.equals(newType)) {
                if ("FABRICATED".equals(newType)) {
                    inventoryStockRepository.deleteByProductId(saved.getId());
                } else if ("FABRICATED".equals(oldType)) {
                    inventoryService.initStock(saved, saved.getStore());
                }
            }
            return ProductResponseDTO.from(saved);
        });
    }

    @Transactional
    public Optional<ProductResponseDTO> toggle(Long id) {
        return productRepository.findById(id).map(product -> {
            product.setActive(!Boolean.TRUE.equals(product.getActive()));
            return ProductResponseDTO.from(productRepository.save(product));
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!productRepository.existsById(id)) return false;
        em.createNativeQuery("UPDATE sale_items SET product_id = NULL WHERE product_id = :id")
                .setParameter("id", id)
                .executeUpdate();
        recipeRepository.deleteByProductId(id);
        inventoryMovementRepository.deleteByProductId(id);
        inventoryStockRepository.deleteByProductId(id);
        productRepository.deleteById(id);
        return true;
    }

    private Product buildProduct(ProductRequestDTO dto, Store store) {
        Product product = new Product();
        applyDTO(product, dto);
        product.setStore(store);
        product.setActive(true);
        return product;
    }

    @Transactional
    public List<RecipeItemDTO> saveRecipe(Long productId, List<RecipeItemDTO> items) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productId));
        if (!"FABRICATED".equals(product.getType())) {
            throw new IllegalArgumentException("Solo los productos FABRICATED pueden tener receta");
        }
        // SQL nativo: bypassa Hibernate por completo, DELETE garantizado antes de INSERTs
        em.createNativeQuery("DELETE FROM product_recipe_items WHERE product_id = :productId")
                .setParameter("productId", productId)
                .executeUpdate();
        em.flush();
        em.clear();
        Product freshProduct = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productId));
        for (RecipeItemDTO dto : items) {
            Product ingredient = productRepository.findById(dto.getIngredientId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Ingrediente no encontrado: " + dto.getIngredientId()));
            ProductRecipeItem item = new ProductRecipeItem();
            item.setProduct(freshProduct);
            item.setIngredient(ingredient);
            item.setQuantity(dto.getQuantity());
            recipeRepository.save(item);
        }
        return recipeRepository.findByProductIdWithIngredient(productId)
                .stream().map(RecipeItemDTO::from).toList();
    }

    @Transactional
    public boolean deleteRecipeItem(Long productId, Long itemId) {
        return recipeRepository.findById(itemId)
                .filter(r -> r.getProduct().getId().equals(productId))
                .map(r -> { recipeRepository.delete(r); return true; })
                .orElse(false);
    }

    private void applyDTO(Product product, ProductRequestDTO dto) {
        product.setName(dto.getName().trim());
        product.setSku(dto.getSku() != null ? dto.getSku().trim() : null);
        product.setType(dto.getType() != null ? dto.getType() : "SIMPLE");
        product.setPrice(dto.getPrice());
        product.setMinStock(dto.getMinStock() != null ? dto.getMinStock() : 0);
        product.setDescription(dto.getDescription());

        if (dto.getCategoryId() != null) {
            categoryRepository.findById(dto.getCategoryId())
                    .ifPresent(product::setCategory);
        } else {
            product.setCategory(null);
        }
    }
}
