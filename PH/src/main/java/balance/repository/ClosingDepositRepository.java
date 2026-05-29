package balance.repository;
import balance.model.ClosingDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClosingDepositRepository extends JpaRepository<ClosingDeposit, Long> {

     List<ClosingDeposit> findByDepositDateBetweenOrderByDepositDateDesc(LocalDate startDate, LocalDate endDate);
    List<ClosingDeposit> findByStoreIdOrderByDepositDateDesc(Long storeId);
    List<ClosingDeposit> findByDepositDateBetweenAndStoreIdOrderByDepositDateDesc(LocalDate startDate, LocalDate endDate, Long storeId);
  
    @Query("SELECT c FROM ClosingDeposit c ORDER BY c.depositDate DESC")
    List<ClosingDeposit> findAllOrderByDepositDateDesc();

    List<ClosingDeposit> findByDepositDateBetween(LocalDate startDate, LocalDate endDate);
    List<ClosingDeposit> findByStoreId(Long storeId);
    List<ClosingDeposit> findByDepositDateBetweenAndStoreId(LocalDate startDate, LocalDate endDate, Long storeId);
    List<ClosingDeposit> findByUsernameOrderByDepositDateDesc(String username);

    // Cierres pendientes de depósito bancario — por local y rango de fechas
    List<ClosingDeposit> findByStoreIdAndDepositStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByDepositDateDesc(
            Long storeId, String depositStatus, LocalDate from, LocalDate to);

    // Todos los pendientes de un local
    List<ClosingDeposit> findByStoreIdAndDepositStatusOrderByDepositDateDesc(Long storeId, String depositStatus);
}