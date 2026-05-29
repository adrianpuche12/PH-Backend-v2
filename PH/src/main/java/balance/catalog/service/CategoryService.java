package balance.catalog.service;

import balance.catalog.dto.CategoryRequestDTO;
import balance.catalog.dto.CategoryResponseDTO;
import balance.catalog.model.Category;
import balance.catalog.repository.CategoryRepository;
import balance.model.Store;
import balance.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StoreRepository storeRepository;

    // Árbol completo de categorías raíz de un local (recursivo sin límite)
    public List<CategoryResponseDTO> getTree(Long storeId) {
        // 1 query: todas las categorías del local
        List<Category> all = categoryRepository.findAllByStoreId(storeId);

        // 1 query: conteo de productos por categoría
        List<Long> catIds = all.stream().map(Category::getId).collect(Collectors.toList());
        Map<Long, Long> productCounts = new HashMap<>();
        if (!catIds.isEmpty()) {
            categoryRepository.countProductsByCategoryIds(catIds)
                    .forEach(row -> productCounts.put((Long) row[0], (Long) row[1]));
        }

        // Construir árbol en memoria (sin más queries)
        Map<Long, CategoryResponseDTO> dtoMap = new HashMap<>();
        for (Category c : all) {
            long count = productCounts.getOrDefault(c.getId(), 0L);
            dtoMap.put(c.getId(), CategoryResponseDTO.from(c, count));
        }

        List<CategoryResponseDTO> roots = new ArrayList<>();
        for (Category c : all) {
            CategoryResponseDTO dto = dtoMap.get(c.getId());
            if (c.getParent() == null) {
                roots.add(dto);
            } else {
                CategoryResponseDTO parent = dtoMap.get(c.getParent().getId());
                if (parent != null) parent.getChildren().add(dto);
            }
        }
        return roots;
    }

    private static final int MAX_CATEGORY_DEPTH = 10;

    // toDTO para operaciones de una sola categoría (create, update, findById)
    private CategoryResponseDTO toDTO(Category category) {
        return toDTO(category, 0);
    }

    private CategoryResponseDTO toDTO(Category category, int depth) {
        long productCount = categoryRepository.countProductsByCategoryId(category.getId());
        CategoryResponseDTO dto = CategoryResponseDTO.from(category, productCount);
        if (depth < MAX_CATEGORY_DEPTH) {
            List<CategoryResponseDTO> childDTOs = category.getChildren()
                    .stream()
                    .map(c -> toDTO(c, depth + 1))
                    .toList();
            dto.setChildren(childDTOs);
        }
        return dto;
    }

    public Optional<CategoryResponseDTO> findById(Long id) {
        return categoryRepository.findById(id).map(this::toDTO);
    }

    @Transactional
    public Optional<CategoryResponseDTO> createRoot(Long storeId, CategoryRequestDTO dto) {
        return storeRepository.findById(storeId).map(store -> {
            Category category = buildCategory(dto, store, null);
            return toDTO(categoryRepository.save(category));
        });
    }

    @Transactional
    public Optional<CategoryResponseDTO> createChild(Long storeId, Long parentId, CategoryRequestDTO dto) {
        Optional<Store> store = storeRepository.findById(storeId);
        Optional<Category> parent = categoryRepository.findById(parentId);

        if (store.isEmpty() || parent.isEmpty()) return Optional.empty();

        Category category = buildCategory(dto, store.get(), parent.get());
        return Optional.of(toDTO(categoryRepository.save(category)));
    }

    @Transactional
    public Optional<CategoryResponseDTO> update(Long id, CategoryRequestDTO dto) {
        return categoryRepository.findById(id).map(category -> {
            category.setName(dto.getName().trim());
            category.setDescription(dto.getDescription());
            if (dto.getDisplayOrder() != null) {
                category.setDisplayOrder(dto.getDisplayOrder());
            }
            return toDTO(categoryRepository.save(category));
        });
    }

    @Transactional
    public Optional<CategoryResponseDTO> toggle(Long id) {
        return categoryRepository.findById(id).map(category -> {
            category.setActive(!Boolean.TRUE.equals(category.getActive()));
            return toDTO(categoryRepository.save(category));
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!categoryRepository.existsById(id)) return false;
        categoryRepository.deleteById(id);
        return true;
    }

    private Category buildCategory(CategoryRequestDTO dto, Store store, Category parent) {
        Category category = new Category();
        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription());
        category.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        category.setActive(true);
        category.setStore(store);
        category.setParent(parent);
        return category;
    }
}
