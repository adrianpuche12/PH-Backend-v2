package balance.repository;

import balance.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByDateBetweenOrderByDateDesc(LocalDate startDate, LocalDate endDate);
    List<Transaction> findByTypeOrderByDateDesc(String type);
    List<Transaction> findByDateBetweenAndStoreIdOrderByDateDesc(LocalDate startDate, LocalDate endDate, Long storeId);
    List<Transaction> findByStoreIdOrderByDateDesc(Long storeId);
    
    @Query("SELECT t FROM Transaction t ORDER BY t.date DESC")
    List<Transaction> findAllOrderByDateDesc();
    
    List<Transaction> findByDateBetween(LocalDate startDate, LocalDate endDate);
    List<Transaction> findByType(String type);
    List<Transaction> findByDateBetweenAndStoreId(LocalDate startDate, LocalDate endDate, Long storeId);
    List<Transaction> findByStoreId(Long storeId);
    List<Transaction> findByGastoAdminId(Long gastoAdminId);
    void deleteByGastoAdminId(Long gastoAdminId);

    @Query("SELECT t FROM Transaction t WHERE t.date BETWEEN :start AND :end AND t.gastoAdminId IS NOT NULL ORDER BY t.date DESC")
    List<Transaction> findGastoAdminTransactionsByDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT t FROM Transaction t WHERE t.date BETWEEN :start AND :end AND t.store.id = :storeId AND t.gastoAdminId IS NOT NULL ORDER BY t.date DESC")
    List<Transaction> findGastoAdminTransactionsByDateBetweenAndStoreId(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("storeId") Long storeId);
}
