package balance.service;

import balance.model.SupplierPayment;
import balance.repository.SupplierPaymentRepository;
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
class SupplierPaymentServiceTest {

    @Mock private SupplierPaymentRepository supplierPaymentRepository;
    @InjectMocks private SupplierPaymentService service;

    @Test
    void findAll_delegatesToRepository() {
        when(supplierPaymentRepository.findAllOrderByPaymentDateDesc()).thenReturn(List.of());
        service.findAll();
        verify(supplierPaymentRepository).findAllOrderByPaymentDateDesc();
    }

    @Test
    void findById_returnsPaymentWhenFound() {
        SupplierPayment sp = buildPayment();
        when(supplierPaymentRepository.findById(1L)).thenReturn(Optional.of(sp));
        assertThat(service.findById(1L)).contains(sp);
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(supplierPaymentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.findById(99L)).isEmpty();
    }

    @Test
    void save_delegatesToRepository() {
        SupplierPayment sp = buildPayment();
        when(supplierPaymentRepository.save(sp)).thenReturn(sp);
        assertThat(service.save(sp)).isEqualTo(sp);
    }

    @Test
    void deleteById_delegatesToRepository() {
        service.deleteById(1L);
        verify(supplierPaymentRepository).deleteById(1L);
    }

    @Test
    void findByStoreId_delegatesToRepository() {
        when(supplierPaymentRepository.findByStoreIdOrderByPaymentDateDesc(1L)).thenReturn(List.of());
        service.findByStoreId(1L);
        verify(supplierPaymentRepository).findByStoreIdOrderByPaymentDateDesc(1L);
    }

    private SupplierPayment buildPayment() {
        SupplierPayment sp = new SupplierPayment();
        sp.setAmount(new BigDecimal("1500.00"));
        sp.setPaymentDate(LocalDate.now());
        sp.setSupplier("Proveedor Test");
        return sp;
    }
}
