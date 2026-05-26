package balance.sales.repository;

import balance.sales.model.Shift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    Optional<Shift> findByStoreIdAndStatus(Long storeId, String status);
    List<Shift> findByStoreIdOrderByOpenedAtDesc(Long storeId);
    Page<Shift> findByStoreIdOrderByOpenedAtDesc(Long storeId, Pageable pageable);
    boolean existsByStoreIdAndStatus(Long storeId, String status);
}
