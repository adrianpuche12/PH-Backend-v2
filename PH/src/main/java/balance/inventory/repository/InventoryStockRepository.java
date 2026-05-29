package balance.inventory.repository;

import balance.inventory.model.InventoryStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {

    @Query("SELECT s FROM InventoryStock s JOIN FETCH s.product p JOIN FETCH s.store LEFT JOIN FETCH p.category c LEFT JOIN FETCH c.parent WHERE s.store.id = :storeId ORDER BY p.name ASC")
    List<InventoryStock> findByStoreIdOrderByProductNameAsc(@Param("storeId") Long storeId);

    Optional<InventoryStock> findByProductIdAndStoreId(Long productId, Long storeId);

    // minStock > 0 evita falsos positivos cuando el producto no tiene mínimo definido
    @Query("SELECT s FROM InventoryStock s JOIN FETCH s.product p JOIN FETCH s.store WHERE s.store.id = :storeId AND p.minStock > 0 AND s.quantity <= p.minStock")
    List<InventoryStock> findLowStockByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT COUNT(s) FROM InventoryStock s WHERE s.store.id = :storeId AND s.product.minStock > 0 AND s.quantity <= s.product.minStock")
    long countLowStockByStoreId(@Param("storeId") Long storeId);

    void deleteByProductId(Long productId);
}
