package balance.controller;

import balance.model.Transaction;
import balance.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private TransactionService transactionService;

    // ── GET /api/transactions/store/{storeId} ─────────────────────────────────

    @Test
    void getByStoreId_returns200WithList() throws Exception {
        Transaction tx = buildTransaction("income", new BigDecimal("500.00"));
        when(transactionService.findByStoreId(1L)).thenReturn(List.of(tx));

        mockMvc.perform(get("/api/transactions/store/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByStoreId_returns200WithEmptyList() throws Exception {
        when(transactionService.findByStoreId(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions/store/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /api/transactions/date-range-store ────────────────────────────────

    @Test
    void getByDateRangeAndStore_returns200WithList() throws Exception {
        when(transactionService.findByDateBetweenAndStoreId(any(), any(), eq(1L)))
            .thenReturn(List.of(buildTransaction("income", new BigDecimal("300.00"))));

        mockMvc.perform(get("/api/transactions/date-range-store")
                .param("startDate", "2026-06-01")
                .param("endDate",   "2026-06-05")
                .param("storeId",   "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByDateRangeAndStore_passesCorrectDatesToService() throws Exception {
        when(transactionService.findByDateBetweenAndStoreId(any(), any(), any()))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/transactions/date-range-store")
                .param("startDate", "2026-06-01")
                .param("endDate",   "2026-06-05")
                .param("storeId",   "2"));

        verify(transactionService).findByDateBetweenAndStoreId(
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 5),
            2L);
    }

    @Test
    void getByDateRangeAndStore_returns400WhenParamMissing() throws Exception {
        mockMvc.perform(get("/api/transactions/date-range-store")
                .param("startDate", "2026-06-01"))
            .andExpect(status().isBadRequest());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Transaction buildTransaction(String type, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setType(type);
        tx.setAmount(amount);
        tx.setDate(LocalDate.now());
        return tx;
    }
}
