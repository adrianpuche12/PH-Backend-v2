package balance.controller;

import balance.model.SupplierPayment;
import balance.service.SupplierPaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupplierPaymentController.class)
class SupplierPaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private SupplierPaymentService supplierPaymentService;

    @Test
    void getByStoreId_returns200WithList() throws Exception {
        SupplierPayment sp = new SupplierPayment();
        sp.setAmount(new BigDecimal("1500.00"));
        sp.setPaymentDate(LocalDate.now());
        sp.setSupplier("Proveedor Test");
        when(supplierPaymentService.findByStoreId(1L)).thenReturn(List.of(sp));

        mockMvc.perform(get("/api/supplier-payments/store/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByStoreId_returns200WithEmptyListWhenNoPayments() throws Exception {
        when(supplierPaymentService.findByStoreId(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/supplier-payments/store/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getByStoreId_delegatesToService() throws Exception {
        when(supplierPaymentService.findByStoreId(2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/supplier-payments/store/2"));

        verify(supplierPaymentService).findByStoreId(2L);
    }
}
