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
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks private ProductService service;

    @Mock private ProductRepository           productRepository;
    @Mock private StoreRepository             storeRepository;
    @Mock private CategoryRepository          categoryRepository;
    @Mock private InventoryService            inventoryService;
    @Mock private InventoryStockRepository    inventoryStockRepository;
    @Mock private InventoryMovementRepository inventoryMovementRepository;
    @Mock private ProductRecipeRepository     recipeRepository;
    @Mock private EntityManager               em;
    @Mock private Query                       nativeQuery;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Store buildStore(Long id) {
        Store s = new Store();
        s.setId(id);
        s.setName("Danlí");
        return s;
    }

    private Product buildProduct(Long id, String name, String sku) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setSku(sku);
        p.setType("SIMPLE");
        p.setPrice(new BigDecimal("45.00"));
        p.setMinStock(5);
        p.setActive(true);
        p.setStore(buildStore(1L));
        return p;
    }

    private ProductRequestDTO buildRequest(String name, String sku) {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setName(name);
        dto.setSku(sku);
        dto.setType("SIMPLE");
        dto.setPrice(new BigDecimal("45.00"));
        dto.setMinStock(5);
        return dto;
    }

    // ── findByStore ───────────────────────────────────────────────────────────

    @Test
    void findByStore_returnsAllProductsWhenNoFilter() {
        when(productRepository.findByStoreIdOrderByNameAsc(1L))
                .thenReturn(List.of(buildProduct(1L, "Pieza de Pollo", "POL-001")));

        List<ProductResponseDTO> result = service.findByStore(1L, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Pieza de Pollo");
    }

    @Test
    void findByStore_filtersActiveProducts() {
        when(productRepository.findByStoreIdAndActiveOrderByNameAsc(1L, true))
                .thenReturn(List.of(buildProduct(1L, "Activo", "A-001")));

        List<ProductResponseDTO> result = service.findByStore(1L, true, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Activo");
    }

    @Test
    void findByStore_searchesByNameOrSku() {
        when(productRepository.searchByStoreId(1L, "pollo"))
                .thenReturn(List.of(buildProduct(1L, "Pollo Entero", "POL-010")));

        List<ProductResponseDTO> result = service.findByStore(1L, null, null, "pollo");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("POL-010");
    }

    @Test
    void findByStore_filtersByCategory() {
        when(productRepository.findByStoreIdAndCategoryIdOrderByNameAsc(1L, 5L))
                .thenReturn(List.of(buildProduct(1L, "Coca Cola", "BEB-001")));

        List<ProductResponseDTO> result = service.findByStore(1L, null, 5L, null);

        assertThat(result).hasSize(1);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_returnsDTOWhenFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(buildProduct(1L, "Pieza", "P-001")));

        Optional<ProductResponseDTO> result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Pieza");
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.findById(99L)).isEmpty();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_savesProductAndInitializesStock() {
        Store store = buildStore(1L);
        Product saved = buildProduct(10L, "Nuevo Producto", "NEW-001");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.existsBySkuAndStoreId("NEW-001", 1L)).thenReturn(false);
        when(productRepository.save(any())).thenReturn(saved);

        Optional<ProductResponseDTO> result = service.create(1L, buildRequest("Nuevo Producto", "NEW-001"));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Nuevo Producto");
        verify(inventoryService).initStock(any(), any());
    }

    @Test
    void create_returnsEmptyWhenStoreNotFound() {
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.create(99L, buildRequest("X", "X-001"))).isEmpty();
    }

    @Test
    void create_throwsWhenSkuAlreadyExistsInStore() {
        Store store = buildStore(1L);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.existsBySkuAndStoreId("DUP-001", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, buildRequest("Duplicado", "DUP-001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU");
    }

    @Test
    void create_throwsWhenNameAlreadyExistsInStore() {
        Store store = buildStore(1L);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.existsByNameIgnoreCaseAndStoreId("Pollo Entero", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, buildRequest("Pollo Entero", "POL-001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre diferente");
    }

    @Test
    void update_throwsWhenNameConflictsWithAnotherProduct() {
        Product existing = buildProduct(1L, "Pollo Frito", "POL-002");
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuAndStoreIdAndIdNot("POL-002", 1L, 1L)).thenReturn(false);
        when(productRepository.existsByNameIgnoreCaseAndStoreIdAndIdNot("Pollo Entero", 1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, buildRequest("Pollo Entero", "POL-002")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre diferente");
    }

    @Test
    void create_allowsNullSku() {
        Store store = buildStore(1L);
        Product saved = buildProduct(10L, "Sin SKU", null);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.save(any())).thenReturn(saved);

        ProductRequestDTO req = new ProductRequestDTO();
        req.setName("Sin SKU");
        req.setPrice(new BigDecimal("10.00"));

        Optional<ProductResponseDTO> result = service.create(1L, req);

        assertThat(result).isPresent();
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_updatesProductFields() {
        Product existing = buildProduct(1L, "Viejo", "OLD-001");
        Product updated  = buildProduct(1L, "Nuevo", "NEW-001");
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuAndStoreIdAndIdNot("NEW-001", 1L, 1L)).thenReturn(false);
        when(productRepository.save(any())).thenReturn(updated);

        Optional<ProductResponseDTO> result = service.update(1L, buildRequest("Nuevo", "NEW-001"));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Nuevo");
    }

    @Test
    void update_returnsEmptyWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.update(99L, buildRequest("X", "X-001"))).isEmpty();
    }

    @Test
    void update_throwsWhenSkuConflictsWithAnotherProduct() {
        Product existing = buildProduct(1L, "Producto", "OLD-001");
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuAndStoreIdAndIdNot("DUP-001", 1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, buildRequest("Producto", "DUP-001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU");
    }

    // ── toggle ────────────────────────────────────────────────────────────────

    @Test
    void toggle_deactivatesActiveProduct() {
        Product product = buildProduct(1L, "Pollo", "P-001");
        product.setActive(true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<ProductResponseDTO> result = service.toggle(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getActive()).isFalse();
    }

    @Test
    void toggle_activatesInactiveProduct() {
        Product product = buildProduct(1L, "Pollo", "P-001");
        product.setActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<ProductResponseDTO> result = service.toggle(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getActive()).isTrue();
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_deletesStockAndMovementsBeforeProduct() {
        when(productRepository.existsById(1L)).thenReturn(true);

        boolean result = service.delete(1L);

        assertThat(result).isTrue();
        verify(inventoryMovementRepository).deleteByProductId(1L);
        verify(inventoryStockRepository).deleteByProductId(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void delete_returnsFalseWhenNotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);
        assertThat(service.delete(99L)).isFalse();
        verify(productRepository, never()).deleteById(any());
    }

    // ── saveRecipe ────────────────────────────────────────────────────────────

    @Test
    void saveRecipe_deleteOccursBeforeInserts() {
        Product fabricated = buildProduct(1L, "Pollo con Papas", "FAB-001");
        fabricated.setType("FABRICATED");

        Product ingredient = buildProduct(2L, "Pollo Crudo", "ING-001");

        when(productRepository.findById(1L)).thenReturn(Optional.of(fabricated));
        when(productRepository.findById(2L)).thenReturn(Optional.of(ingredient));
        when(em.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(recipeRepository.findByProductIdWithIngredient(1L)).thenReturn(List.of());

        RecipeItemDTO dto = new RecipeItemDTO();
        dto.setIngredientId(2L);
        dto.setQuantity(new BigDecimal("1.000"));

        service.saveRecipe(1L, List.of(dto));

        // Verifica que native DELETE → flush → clear ocurren ANTES de save
        InOrder order = inOrder(em, recipeRepository);
        order.verify(em).createNativeQuery(contains("DELETE FROM product_recipe_items"));
        order.verify(em).flush();
        order.verify(em).clear();
        order.verify(recipeRepository).save(any(ProductRecipeItem.class));
    }

    @Test
    void saveRecipe_throwsWhenProductNotFabricated() {
        Product simple = buildProduct(1L, "Pieza Simple", "SIM-001");
        when(productRepository.findById(1L)).thenReturn(Optional.of(simple));

        assertThatThrownBy(() -> service.saveRecipe(1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FABRICATED");
    }
}
