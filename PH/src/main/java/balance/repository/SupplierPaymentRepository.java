package balance.repository;
import balance.model.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

  List<SupplierPayment> findByPaymentDateBetweenOrderByPaymentDateDesc(LocalDate startDate, LocalDate endDate);
    List<SupplierPayment> findByPaymentDateBetweenAndStoreIdOrderByPaymentDateDesc(LocalDate startDate, LocalDate endDate, Long storeId);
    List<SupplierPayment> findByStoreIdOrderByPaymentDateDesc(Long storeId);
    
    @Query("SELECT s FROM SupplierPayment s ORDER BY s.paymentDate DESC")
    List<SupplierPayment> findAllOrderByPaymentDateDesc();

    List<SupplierPayment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);
    List<SupplierPayment> findByPaymentDateBetweenAndStoreId(LocalDate startDate, LocalDate endDate, Long storeId);
    List<SupplierPayment> findByStoreId(Long storeId);
    List<SupplierPayment> findByUsernameOrderByPaymentDateDesc(String username);
}