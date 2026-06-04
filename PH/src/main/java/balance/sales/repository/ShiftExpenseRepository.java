package balance.sales.repository;

import balance.sales.model.ShiftExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ShiftExpenseRepository extends JpaRepository<ShiftExpense, Long> {

    List<ShiftExpense> findByShiftIdOrderByCreatedAtAsc(Long shiftId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ShiftExpense e WHERE e.shift.id = :shiftId")
    BigDecimal sumAmountByShiftId(@Param("shiftId") Long shiftId);
}
