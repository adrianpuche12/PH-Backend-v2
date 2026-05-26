package balance.catalog.repository;

import balance.catalog.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Trae solo las categorías raíz de un local (parent = null)
    @Query("SELECT c FROM Category c WHERE c.store.id = :storeId AND c.parent IS NULL ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findRootsByStoreId(Long storeId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countProductsByCategoryId(Long categoryId);

    boolean existsByStoreIdAndParentIsNull(Long storeId);
}
