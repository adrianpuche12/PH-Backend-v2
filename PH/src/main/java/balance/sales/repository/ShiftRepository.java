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
    // Turnos abiertos de un local — puede haber varios (uno por cajero)
    List<Shift> findByStoreIdAndStatusOrderByOpenedAtDesc(Long storeId, String status);

    // Turno abierto de un usuario puntual en un local (uno por usuario)
    Optional<Shift> findByStoreIdAndUsernameAndStatus(Long storeId, String username, String status);
    boolean existsByStoreIdAndUsernameAndStatus(Long storeId, String username, String status);

    List<Shift> findByStoreIdOrderByOpenedAtDesc(Long storeId);
    Page<Shift> findByStoreIdOrderByOpenedAtDesc(Long storeId, Pageable pageable);
    Page<Shift> findByStoreIdAndUsernameOrderByOpenedAtDesc(Long storeId, String username, Pageable pageable);

    // Queries para filtro de fechas — sin null params (Hibernate 6 compatible)
    Page<Shift> findByStoreIdAndOpenedAtBetweenOrderByOpenedAtDesc(Long storeId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    Page<Shift> findByStoreIdAndUsernameAndOpenedAtBetweenOrderByOpenedAtDesc(Long storeId, String username, LocalDateTime from, LocalDateTime to, Pageable pageable);


    // Para "Mis ventas": shifts de un usuario en todos los locales
    Page<Shift> findByUsernameOrderByOpenedAtDesc(String username, Pageable pageable);

    /**
     * Consulta unificada con filtros opcionales: username, rango de fechas (openedAt).
     * Pasar null para omitir cualquier filtro.
     */
    @Query(value = "SELECT s FROM Shift s WHERE s.store.id = :storeId " +
                   "AND (:username IS NULL OR s.username = :username) " +
                   "AND (:from IS NULL OR s.openedAt >= :from) " +
                   "AND (:to   IS NULL OR s.openedAt <= :to) " +
                   "ORDER BY s.openedAt DESC",
           countQuery = "SELECT COUNT(s) FROM Shift s WHERE s.store.id = :storeId " +
                        "AND (:username IS NULL OR s.username = :username) " +
                        "AND (:from IS NULL OR s.openedAt >= :from) " +
                        "AND (:to   IS NULL OR s.openedAt <= :to)")
    Page<Shift> findByFilters(
        @Param("storeId")  Long          storeId,
        @Param("username") String        username,
        @Param("from")     LocalDateTime from,
        @Param("to")       LocalDateTime to,
        Pageable pageable
    );
}
