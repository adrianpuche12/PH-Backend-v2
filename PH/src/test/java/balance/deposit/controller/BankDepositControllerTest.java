package balance.deposit.controller;

import balance.config.GlobalExceptionHandler;
import balance.deposit.dto.CreateDepositRequest;
import balance.deposit.dto.DepositResponse;
import balance.deposit.dto.PendingClosingResponse;
import balance.deposit.service.BankDepositService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankDepositController.class)
class BankDepositControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private BankDepositService service;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ── GET /api/v2/deposits/pending-closings ─────────────────────────────────

    @Test
    void getPendingClosings_returns200WithList() throws Exception {
        when(service.getPendingClosings(eq(1L), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/deposits/pending-closings").param("storeId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPendingClosings_acceptsOptionalDateParams() throws Exception {
        when(service.getPendingClosings(eq(1L), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/v2/deposits/pending-closings")
                .param("storeId", "1")
                .param("from", "2026-06-01")
                .param("to",   "2026-06-05"))
            .andExpect(status().isOk());

        verify(service).getPendingClosings(eq(1L),
            eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 5)));
    }

    // ── POST /api/v2/deposits ─────────────────────────────────────────────────

    @Test
    void create_returns200WhenSuccessful() throws Exception {
        DepositResponse resp = buildDepositResponse("CONFIRMED");
        when(service.createDeposit(eq("cajero01"), any())).thenReturn(resp);

        CreateDepositRequest req = buildRequest();
        mockMvc.perform(post("/api/v2/deposits")
                .param("username", "cajero01")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void create_returns400WhenClosingAlreadyDeposited() throws Exception {
        when(service.createDeposit(any(), any()))
            .thenThrow(new IllegalArgumentException("ya fue depositado"));

        mockMvc.perform(post("/api/v2/deposits")
                .param("username", "cajero01")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(buildRequest())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400WhenValidationFails() throws Exception {
        // Request sin campos requeridos
        mockMvc.perform(post("/api/v2/deposits")
                .param("username", "cajero01")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    // ── GET /api/v2/deposits ──────────────────────────────────────────────────

    @Test
    void getAll_returns200WithPage() throws Exception {
        when(service.getAll(any(), anyInt(), anyInt()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v2/deposits"))
            .andExpect(status().isOk());
    }

    @Test
    void getAll_acceptsStatusFilter() throws Exception {
        when(service.getAll(eq("DISCREPANCY"), anyInt(), anyInt()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v2/deposits").param("status", "DISCREPANCY"))
            .andExpect(status().isOk());

        verify(service).getAll("DISCREPANCY", 0, 20);
    }

    // ── GET /api/v2/deposits/{id} ─────────────────────────────────────────────

    @Test
    void getById_returns200WhenFound() throws Exception {
        when(service.getById(1L)).thenReturn(buildDepositResponse("CONFIRMED"));

        mockMvc.perform(get("/api/v2/deposits/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        when(service.getById(99L)).thenThrow(new IllegalArgumentException("no encontrado"));

        mockMvc.perform(get("/api/v2/deposits/99"))
            .andExpect(status().isNotFound());
    }

    // ── PUT /api/v2/deposits/{id}/confirm ─────────────────────────────────────

    @Test
    void confirm_returns200WhenSuccessful() throws Exception {
        when(service.confirmDeposit(1L)).thenReturn(buildDepositResponse("CONFIRMED"));

        mockMvc.perform(put("/api/v2/deposits/1/confirm"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void confirm_returns404WhenNotFound() throws Exception {
        when(service.confirmDeposit(99L)).thenThrow(new IllegalArgumentException("no encontrado"));

        mockMvc.perform(put("/api/v2/deposits/99/confirm"))
            .andExpect(status().isNotFound());
    }

    // ── GET /api/v2/deposits/mine ─────────────────────────────────────────────

    @Test
    void getMine_returns200WithUserDeposits() throws Exception {
        when(service.getByUser("cajero01")).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/deposits/mine").param("username", "cajero01"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DepositResponse buildDepositResponse(String status) {
        DepositResponse r = new DepositResponse();
        ReflectionTestUtils.setField(r, "id",             1L);
        ReflectionTestUtils.setField(r, "status",         status);
        ReflectionTestUtils.setField(r, "createdBy",      "cajero01");
        ReflectionTestUtils.setField(r, "depositDate",    LocalDate.now());
        ReflectionTestUtils.setField(r, "expectedCash",   new BigDecimal("500.00"));
        ReflectionTestUtils.setField(r, "declaredAmount", new BigDecimal("500.00"));
        ReflectionTestUtils.setField(r, "difference",     BigDecimal.ZERO);
        ReflectionTestUtils.setField(r, "storeIds",       List.of(1L));
        ReflectionTestUtils.setField(r, "shiftIds",       List.of(1L));
        return r;
    }

    private CreateDepositRequest buildRequest() {
        CreateDepositRequest req = new CreateDepositRequest();
        req.setShiftIds(List.of(1L, 2L));
        req.setDeclaredAmount(new BigDecimal("500.00"));
        req.setDepositDate(LocalDate.now());
        return req;
    }
}
