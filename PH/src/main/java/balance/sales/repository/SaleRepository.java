package balance.sales.repository;

import balance.sales.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByShiftIdOrderByCreatedAtDesc(Long shiftId);
    List<Sale> findByStoreIdAndSaleDateOrderByCreatedAtDesc(Long storeId, LocalDate date);
    List<Sale> findByShiftIdAndStatus(Long shiftId, String status);

    @Query("SELECT s FROM Sale s WHERE s.shift.id = :shiftId AND s.status = 'OPEN'")
    List<Sale> findOpenByShiftId(@Param("shiftId") Long shiftId);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.shift.id = :shiftId AND s.status = 'OPEN'")
    long countOpenByShiftId(@Param("shiftId") Long shiftId);
}
