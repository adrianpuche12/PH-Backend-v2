package balance.inventory.repository;

import balance.inventory.model.InventoryStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {

    List<InventoryStock> findByStoreIdOrderByProductNameAsc(Long storeId);

    Optional<InventoryStock> findByProductIdAndStoreId(Long productId, Long storeId);

    @Query("SELECT s FROM InventoryStock s WHERE s.store.id = :storeId AND s.quantity <= s.product.minStock")
    List<InventoryStock> findLowStockByStoreId(Long storeId);

    @Query("SELECT COUNT(s) FROM InventoryStock s WHERE s.store.id = :storeId AND s.quantity <= s.product.minStock")
    long countLowStockByStoreId(Long storeId);

    void deleteByProductId(Long productId);
}
