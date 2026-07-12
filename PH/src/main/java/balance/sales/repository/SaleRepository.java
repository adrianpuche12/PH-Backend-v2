package balance.sales.repository;

import balance.sales.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByShiftIdOrderByCreatedAtDesc(Long shiftId);
    List<Sale> findByStoreIdAndSaleDateOrderByCreatedAtDesc(Long storeId, LocalDate date);
    List<Sale> findByShiftIdAndStatus(Long shiftId, String status);

    // Aggregate queries — dashboard (evitan cargar objetos en memoria)
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.store.id IN :storeIds AND s.saleDate = :date")
    long countByStoreIdsAndSaleDate(@Param("storeIds") List<Long> storeIds, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.store.id IN :storeIds AND s.saleDate = :date")
    BigDecimal sumTotalByStoreIdsAndSaleDate(@Param("storeIds") List<Long> storeIds, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.shift.id = :shiftId AND s.status = 'OPEN'")
    BigDecimal sumTotalOpenByShiftId(@Param("shiftId") Long shiftId);

    // Historial de ventas por local con rango de fechas opcional
    @Query("SELECT s FROM Sale s WHERE s.store.id = :storeId AND (:from IS NULL OR s.saleDate >= :from) AND (:to IS NULL OR s.saleDate <= :to) ORDER BY s.createdAt DESC")
    List<Sale> findByStoreIdAndDateRange(@Param("storeId") Long storeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT s FROM Sale s WHERE s.shift.id = :shiftId AND s.status = 'OPEN'")
    List<Sale> findOpenByShiftId(@Param("shiftId") Long shiftId);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.shift.id = :shiftId AND s.status = 'OPEN'")
    long countOpenByShiftId(@Param("shiftId") Long shiftId);

    /** Total de ventas (cualquier estado) en un turno — para el resumen admin */
    long countByShiftId(Long shiftId);

    @Query("SELECT s FROM Sale s WHERE s.store.id = :storeId AND s.saleDate >= :from AND s.saleDate <= :to ORDER BY s.createdAt DESC")
    List<Sale> findByStoreIdAndDateRangeStrict(@Param("storeId") Long storeId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
