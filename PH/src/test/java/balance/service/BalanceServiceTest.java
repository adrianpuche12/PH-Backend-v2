package balance.service;

import balance.model.Transaction;
import balance.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @InjectMocks private BalanceService service;

    // ── calculateBalance ──────────────────────────────────────────────────────

    @Test
    void calculateBalance_returnsZeroWhenNoTransactions() {
        when(transactionRepository.findByDateBetweenOrderByDateDesc(any(), any()))
            .thenReturn(List.of());

        BigDecimal result = service.calculateBalance(
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5));

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateBalance_sumsIncomeAndSubtractsExpense() {
        List<Transaction> txs = List.of(
            buildTransaction("income",  new BigDecimal("500.00")),
            buildTransaction("income",  new BigDecimal("300.00")),
            buildTransaction("expense", new BigDecimal("200.00"))
        );
        when(transactionRepository.findByDateBetweenOrderByDateDesc(any(), any())).thenReturn(txs);

        BigDecimal result = service.calculateBalance(
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5));

        assertThat(result).isEqualByComparingTo("600.00"); // 800 - 200
    }

    @Test
    void calculateBalance_returnsNegativeWhenExpensesExceedIncome() {
        List<Transaction> txs = List.of(
            buildTransaction("income",  new BigDecimal("100.00")),
            buildTransaction("expense", new BigDecimal("300.00"))
        );
        when(transactionRepository.findByDateBetweenOrderByDateDesc(any(), any())).thenReturn(txs);

        BigDecimal result = service.calculateBalance(
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5));

        assertThat(result).isEqualByComparingTo("-200.00");
    }

    @Test
    void calculateBalance_adjustsEndDateByOneDayToIncludeFullDay() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end   = LocalDate.of(2026, 6, 5);
        when(transactionRepository.findByDateBetweenOrderByDateDesc(any(), any()))
            .thenReturn(List.of());

        service.calculateBalance(start, end);

        verify(transactionRepository).findByDateBetweenOrderByDateDesc(start, end.plusDays(1));
    }

    @Test
    void calculateBalance_isCaseInsensitiveForTransactionType() {
        List<Transaction> txs = List.of(
            buildTransaction("INCOME",  new BigDecimal("400.00")),
            buildTransaction("EXPENSE", new BigDecimal("100.00"))
        );
        when(transactionRepository.findByDateBetweenOrderByDateDesc(any(), any())).thenReturn(txs);

        BigDecimal result = service.calculateBalance(
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5));

        assertThat(result).isEqualByComparingTo("300.00");
    }

    @Test
    void calculateBalance_returnsZeroWhenOnlyUnknownTypes() {
        List<Transaction> txs = List.of(buildTransaction("transfer", new BigDecimal("500.00")));
        when(transactionRepository.findByDateBetweenOrderByDateDesc(any(), any())).thenReturn(txs);

        BigDecimal result = service.calculateBalance(
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5));

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── deleteTransaction ─────────────────────────────────────────────────────

    @Test
    void deleteTransaction_returnsTrueWhenFound() {
        Transaction tx = buildTransaction("income", BigDecimal.TEN);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        boolean result = service.deleteTransaction(1L);

        assertThat(result).isTrue();
        verify(transactionRepository).delete(tx);
    }

    @Test
    void deleteTransaction_returnsFalseWhenNotFound() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = service.deleteTransaction(99L);

        assertThat(result).isFalse();
        verify(transactionRepository, never()).delete(any());
    }

    // ── saveTransaction ───────────────────────────────────────────────────────

    @Test
    void saveTransaction_delegatesToRepository() {
        Transaction tx = buildTransaction("income", BigDecimal.TEN);
        when(transactionRepository.save(tx)).thenReturn(tx);

        Transaction result = service.saveTransaction(tx);

        assertThat(result).isEqualTo(tx);
        verify(transactionRepository).save(tx);
    }

    // ── getTransactionById ────────────────────────────────────────────────────

    @Test
    void getTransactionById_returnsEmptyWhenNotFound() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Transaction> result = service.getTransactionById(99L);

        assertThat(result).isEmpty();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Transaction buildTransaction(String type, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setType(type);
        tx.setAmount(amount);
        tx.setDate(LocalDate.now());
        ReflectionTestUtils.setField(tx, "id", 1L);
        return tx;
    }
}
