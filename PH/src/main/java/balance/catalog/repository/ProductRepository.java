package balance.catalog.repository;

import balance.catalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<Product> findByStoreIdAndTypeOrderByNameAsc(Long storeId, String type);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM Product p WHERE LOWER(p.sku) = LOWER(:sku) AND p.store.id = :storeId")
    boolean existsBySkuAndStoreId(@Param("sku") String sku, @Param("storeId") Long storeId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM Product p WHERE LOWER(p.sku) = LOWER(:sku) AND p.store.id = :storeId AND p.id <> :id")
    boolean existsBySkuAndStoreIdAndIdNot(@Param("sku") String sku, @Param("storeId") Long storeId, @Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM Product p WHERE LOWER(p.name) = LOWER(:name) AND p.store.id = :storeId")
    boolean existsByNameIgnoreCaseAndStoreId(@Param("name") String name, @Param("storeId") Long storeId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM Product p WHERE LOWER(p.name) = LOWER(:name) AND p.store.id = :storeId AND p.id <> :id")
    boolean existsByNameIgnoreCaseAndStoreIdAndIdNot(@Param("name") String name, @Param("storeId") Long storeId, @Param("id") Long id);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countByCategoryId(Long categoryId);
}
