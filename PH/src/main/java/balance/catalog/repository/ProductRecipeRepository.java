package balance.catalog.repository;

import balance.catalog.model.ProductRecipeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRecipeRepository extends JpaRepository<ProductRecipeItem, Long> {

    @Query("SELECT r FROM ProductRecipeItem r JOIN FETCH r.ingredient WHERE r.product.id = :productId")
    List<ProductRecipeItem> findByProductIdWithIngredient(@Param("productId") Long productId);

    void deleteByProductId(Long productId);
}
