package balance.catalog.service;

import balance.catalog.dto.CategoryRequestDTO;
import balance.catalog.dto.CategoryResponseDTO;
import balance.catalog.model.Category;
import balance.catalog.repository.CategoryRepository;
import balance.model.Store;
import balance.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks private CategoryService service;

    @Mock private CategoryRepository categoryRepository;
    @Mock private StoreRepository    storeRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Store buildStore(Long id) {
        Store s = new Store();
        s.setId(id);
        s.setName("Danlí");
        return s;
    }

    private Category buildCategory(Long id, String name, Category parent, Store store) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setActive(true);
        c.setParent(parent);
        c.setStore(store);
        c.setChildren(new java.util.ArrayList<>());
        return c;
    }

    private CategoryRequestDTO buildRequest(String name) {
        CategoryRequestDTO dto = new CategoryRequestDTO();
        dto.setName(name);
        return dto;
    }

    // ── getTree ───────────────────────────────────────────────────────────────

    @Test
    void getTree_returnsEmptyListWhenNoCategoriesExist() {
        when(categoryRepository.findAllByStoreId(1L)).thenReturn(List.of());

        List<CategoryResponseDTO> result = service.getTree(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getTree_returnsFlatListForSingleRoot() {
        Store store = buildStore(1L);
        Category root = buildCategory(1L, "Bebidas", null, store);
        when(categoryRepository.findAllByStoreId(1L)).thenReturn(List.of(root));
        when(categoryRepository.countProductsByCategoryIds(List.of(1L))).thenReturn(List.of());

        List<CategoryResponseDTO> result = service.getTree(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Bebidas");
    }

    @Test
    void getTree_buildsParentChildRelationshipInMemory() {
        Store store = buildStore(1L);
        Category root  = buildCategory(1L, "Bebidas", null, store);
        Category child = buildCategory(2L, "Gaseosas", root, store);
        root.getChildren().add(child);

        when(categoryRepository.findAllByStoreId(1L)).thenReturn(List.of(root, child));
        when(categoryRepository.countProductsByCategoryIds(any())).thenReturn(List.of());

        List<CategoryResponseDTO> result = service.getTree(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Bebidas");
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_returnsDTOWhenFound() {
        Store store = buildStore(1L);
        Category cat = buildCategory(1L, "Pollos", null, store);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(categoryRepository.countProductsByCategoryId(1L)).thenReturn(5L);

        Optional<CategoryResponseDTO> result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Pollos");
        assertThat(result.get().getProductCount()).isEqualTo(5L);
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.findById(99L)).isEmpty();
    }

    // ── createRoot ────────────────────────────────────────────────────────────

    @Test
    void createRoot_savesAndReturnsCategoryWhenStoreExists() {
        Store store = buildStore(1L);
        Category saved = buildCategory(10L, "Nueva Cat", null, store);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(categoryRepository.save(any())).thenReturn(saved);
        when(categoryRepository.countProductsByCategoryId(10L)).thenReturn(0L);

        Optional<CategoryResponseDTO> result = service.createRoot(1L, buildRequest("Nueva Cat"));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Nueva Cat");
        verify(categoryRepository).save(any());
    }

    @Test
    void createRoot_returnsEmptyWhenStoreNotFound() {
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.createRoot(99L, buildRequest("X"))).isEmpty();
    }

    @Test
    void createRoot_setsActiveTrueByDefault() {
        Store store = buildStore(1L);
        Category saved = buildCategory(10L, "Cat", null, store);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(categoryRepository.save(any())).thenReturn(saved);
        when(categoryRepository.countProductsByCategoryId(anyLong())).thenReturn(0L);

        service.createRoot(1L, buildRequest("Cat"));

        verify(categoryRepository).save(argThat(c -> Boolean.TRUE.equals(c.getActive())));
    }

    // ── createChild ───────────────────────────────────────────────────────────

    @Test
    void createChild_savesSubcategoryWhenBothExist() {
        Store store = buildStore(1L);
        Category parent = buildCategory(1L, "Bebidas", null, store);
        Category child  = buildCategory(2L, "Gaseosas", parent, store);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any())).thenReturn(child);
        when(categoryRepository.countProductsByCategoryId(2L)).thenReturn(0L);

        Optional<CategoryResponseDTO> result = service.createChild(1L, 1L, buildRequest("Gaseosas"));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Gaseosas");
    }

    @Test
    void createChild_returnsEmptyWhenParentNotFound() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L)));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.createChild(1L, 99L, buildRequest("X"))).isEmpty();
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_updatesNameAndDescription() {
        Store store = buildStore(1L);
        Category existing = buildCategory(1L, "Viejo", null, store);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.countProductsByCategoryId(anyLong())).thenReturn(0L);

        CategoryRequestDTO req = buildRequest("Nuevo");
        req.setDescription("Descripción");
        Optional<CategoryResponseDTO> result = service.update(1L, req);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Nuevo");
    }

    @Test
    void update_returnsEmptyWhenNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.update(99L, buildRequest("X"))).isEmpty();
    }

    // ── toggle ────────────────────────────────────────────────────────────────

    @Test
    void toggle_deactivatesActiveCategory() {
        Store store = buildStore(1L);
        Category cat = buildCategory(1L, "Bebidas", null, store);
        cat.setActive(true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.countProductsByCategoryId(anyLong())).thenReturn(0L);

        Optional<CategoryResponseDTO> result = service.toggle(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getActive()).isFalse();
    }

    @Test
    void toggle_activatesInactiveCategory() {
        Store store = buildStore(1L);
        Category cat = buildCategory(1L, "Bebidas", null, store);
        cat.setActive(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.countProductsByCategoryId(anyLong())).thenReturn(0L);

        Optional<CategoryResponseDTO> result = service.toggle(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getActive()).isTrue();
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_returnsTrueAndDeletesWhenFound() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        boolean result = service.delete(1L);

        assertThat(result).isTrue();
        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void delete_returnsFalseWhenNotFound() {
        when(categoryRepository.existsById(99L)).thenReturn(false);
        assertThat(service.delete(99L)).isFalse();
        verify(categoryRepository, never()).deleteById(any());
    }
}
