package balance.controller;

import balance.model.SalaryPayment;
import balance.service.SalaryPaymentService;
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

@WebMvcTest(SalaryPaymentController.class)
class SalaryPaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private SalaryPaymentService salaryPaymentService;

    @Test
    void getByStoreId_returns200WithList() throws Exception {
        SalaryPayment sp = new SalaryPayment();
        sp.setAmount(new BigDecimal("5000.00"));
        sp.setSalaryDate(LocalDate.now());
        when(salaryPaymentService.findByStoreId(1L)).thenReturn(List.of(sp));

        mockMvc.perform(get("/api/salary-payments/store/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByStoreId_returns200WithEmptyListWhenNoPayments() throws Exception {
        when(salaryPaymentService.findByStoreId(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/salary-payments/store/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getByStoreId_delegatesToService() throws Exception {
        when(salaryPaymentService.findByStoreId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/salary-payments/store/1"));

        verify(salaryPaymentService).findByStoreId(1L);
    }
}
