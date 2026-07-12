package balance.catalog.repository;

import balance.catalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStoreIdOrderByNameAsc(Long storeId);

    long countByStoreId(Long storeId);

    List<Product> findByStoreIdAndActiveOrderByNameAsc(Long storeId, Boolean active);

    List<Product> findByStoreIdAndCategoryIdOrderByNameAsc(Long storeId, Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku)  LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Product> searchByStoreId(Long storeId, String search);

    boolean existsBySkuAndStoreId(String sku, Long storeId);

    boolean existsBySkuAndStoreIdAndIdNot(String sku, Long storeId, Long id);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countByCategoryId(Long categoryId);
}
