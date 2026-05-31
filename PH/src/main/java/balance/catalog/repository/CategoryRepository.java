package balance.catalog.repository;

import balance.catalog.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Trae solo las categorías raíz de un local (parent = null)
    @Query("SELECT c FROM Category c WHERE c.store.id = :storeId AND c.parent IS NULL ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findRootsByStoreId(Long storeId);

    // Trae TODAS las categorías del local en una sola query con JOIN FETCH
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.store.id = :storeId ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findAllByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countProductsByCategoryId(Long categoryId);

    // Cuenta productos por categoría en una sola query — evita N+1
    @Query("SELECT p.category.id, COUNT(p) FROM Product p WHERE p.category.id IN :categoryIds GROUP BY p.category.id")
    List<Object[]> countProductsByCategoryIds(@Param("categoryIds") List<Long> categoryIds);

    boolean existsByStoreIdAndParentIsNull(Long storeId);
}
