package balance.deposit.repository;

import balance.deposit.model.BankDeposit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankDepositRepository extends JpaRepository<BankDeposit, Long> {

    Page<BankDeposit> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<BankDeposit> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    List<BankDeposit> findByCreatedByOrderByCreatedAtDesc(String createdBy);
}
