package balance.service;

import balance.model.SalaryPayment;
import balance.repository.SalaryPaymentRepository;
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
class SalaryPaymentServiceTest {

    @Mock private SalaryPaymentRepository salaryPaymentRepository;
    @InjectMocks private SalaryPaymentService service;

    @Test
    void findAll_delegatesToRepository() {
        when(salaryPaymentRepository.findAllOrderBySalaryDateDesc()).thenReturn(List.of());
        service.findAll();
        verify(salaryPaymentRepository).findAllOrderBySalaryDateDesc();
    }

    @Test
    void findById_returnsPaymentWhenFound() {
        SalaryPayment sp = buildPayment();
        when(salaryPaymentRepository.findById(1L)).thenReturn(Optional.of(sp));
        assertThat(service.findById(1L)).contains(sp);
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(salaryPaymentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.findById(99L)).isEmpty();
    }

    @Test
    void save_delegatesToRepository() {
        SalaryPayment sp = buildPayment();
        when(salaryPaymentRepository.save(sp)).thenReturn(sp);
        assertThat(service.save(sp)).isEqualTo(sp);
    }

    @Test
    void deleteById_delegatesToRepository() {
        service.deleteById(1L);
        verify(salaryPaymentRepository).deleteById(1L);
    }

    @Test
    void findByStoreId_delegatesToRepository() {
        when(salaryPaymentRepository.findByStoreIdOrderBySalaryDateDesc(1L)).thenReturn(List.of());
        service.findByStoreId(1L);
        verify(salaryPaymentRepository).findByStoreIdOrderBySalaryDateDesc(1L);
    }

    private SalaryPayment buildPayment() {
        SalaryPayment sp = new SalaryPayment();
        sp.setAmount(new BigDecimal("5000.00"));
        sp.setSalaryDate(LocalDate.now());
        sp.setUsername("cajero01");
        return sp;
    }
}
