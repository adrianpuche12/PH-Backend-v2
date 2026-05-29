package balance.sales.repository;

import balance.sales.model.Shift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    Optional<Shift> findByStoreIdAndStatus(Long storeId, String status);
    List<Shift> findByStoreIdOrderByOpenedAtDesc(Long storeId);
    Page<Shift> findByStoreIdOrderByOpenedAtDesc(Long storeId, Pageable pageable);
    Page<Shift> findByStoreIdAndUsernameOrderByOpenedAtDesc(Long storeId, String username, Pageable pageable);
    boolean existsByStoreIdAndStatus(Long storeId, String status);

    /**
     * Consulta unificada con filtros opcionales: username, rango de fechas (openedAt).
     * Pasar null para omitir cualquier filtro.
     */
    @Query("SELECT s FROM Shift s WHERE s.store.id = :storeId " +
           "AND (:username IS NULL OR s.username = :username) " +
           "AND (:from IS NULL OR s.openedAt >= :from) " +
           "AND (:to   IS NULL OR s.openedAt <= :to) " +
           "ORDER BY s.openedAt DESC")
    Page<Shift> findByFilters(
        @Param("storeId")  Long          storeId,
        @Param("username") String        username,
        @Param("from")     LocalDateTime from,
        @Param("to")       LocalDateTime to,
        Pageable pageable
    );
}
