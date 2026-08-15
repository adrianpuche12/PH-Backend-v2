package balance.repository;
import balance.model.ClosingDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
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

    // Cierre vinculado a un turno específico
    List<ClosingDeposit> findByShiftId(Long shiftId);

    // Actualiza directamente via SQL para evitar problemas de dirty-checking JPA
    @Modifying
    @Query(value = "UPDATE closing_deposits SET deposit_status = :status, bank_deposit_id = :bankDepositId, image_uri = :imageUri WHERE id = :id", nativeQuery = true)
    void updateDepositInfo(Long id, String status, Long bankDepositId, String imageUri);

    // Actualiza solo la imagen de un cierre pendiente
    @Modifying
    @Query(value = "UPDATE closing_deposits SET image_uri = :imageUri WHERE id = :id", nativeQuery = true)
    void updateImageUri(Long id, String imageUri);
}