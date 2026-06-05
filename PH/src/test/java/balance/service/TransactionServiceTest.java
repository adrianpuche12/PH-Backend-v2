package balance.service;

import balance.model.Transaction;
import balance.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @InjectMocks private TransactionService service;

    @Test
    void findAll_delegatesToRepository() {
        when(transactionRepository.findAllOrderByDateDesc()).thenReturn(List.of());
        service.findAll();
        verify(transactionRepository).findAllOrderByDateDesc();
    }

    @Test
    void findById_returnsTransactionWhenFound() {
        Transaction tx = buildTx();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        assertThat(service.findById(1L)).contains(tx);
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.findById(99L)).isEmpty();
    }

    @Test
    void save_delegatesToRepository() {
        Transaction tx = buildTx();
        when(transactionRepository.save(tx)).thenReturn(tx);
        assertThat(service.save(tx)).isEqualTo(tx);
    }

    @Test
    void deleteById_delegatesToRepository() {
        service.deleteById(1L);
        verify(transactionRepository).deleteById(1L);
    }

    @Test
    void findByStoreId_delegatesToRepository() {
        when(transactionRepository.findByStoreId(1L)).thenReturn(List.of());
        service.findByStoreId(1L);
        verify(transactionRepository).findByStoreId(1L);
    }

    @Test
    void findByDateBetweenAndStoreId_delegatesToRepository() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to   = LocalDate.of(2026, 6, 5);
        when(transactionRepository.findByDateBetweenAndStoreIdOrderByDateDesc(from, to, 1L)).thenReturn(List.of());
        service.findByDateBetweenAndStoreId(from, to, 1L);
        verify(transactionRepository).findByDateBetweenAndStoreIdOrderByDateDesc(from, to, 1L);
    }

    private Transaction buildTx() {
        Transaction tx = new Transaction();
        tx.setType("income");
        tx.setAmount(new BigDecimal("100.00"));
        tx.setDate(LocalDate.now());
        return tx;
    }
}
