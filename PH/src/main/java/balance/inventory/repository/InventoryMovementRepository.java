package balance.inventory.repository;

import balance.inventory.model.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    List<InventoryMovement> findByProductIdAndStoreIdOrderByCreatedAtDesc(Long productId, Long storeId);

    void deleteByProductId(Long productId);
}
