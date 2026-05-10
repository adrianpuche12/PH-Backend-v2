package balance.catalog.service;

import balance.catalog.dto.ProductRequestDTO;
import balance.catalog.dto.ProductResponseDTO;
import balance.catalog.model.Category;
import balance.catalog.model.Product;
import balance.catalog.repository.CategoryRepository;
import balance.catalog.repository.ProductRepository;
import balance.model.Store;
import balance.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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

        Product product = buildProduct(dto, store.get());
        return Optional.of(ProductResponseDTO.from(productRepository.save(product)));
    }

    @Transactional
    public Optional<ProductResponseDTO> update(Long id, ProductRequestDTO dto) {
        return productRepository.findById(id).map(product -> {
            if (dto.getSku() != null && !dto.getSku().isBlank()
                    && productRepository.existsBySkuAndStoreIdAndIdNot(
                        dto.getSku().trim(), product.getStore().getId(), id)) {
                throw new IllegalArgumentException("Ya existe un producto con ese SKU en este local");
            }
            applyDTO(product, dto);
            return ProductResponseDTO.from(productRepository.save(product));
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
