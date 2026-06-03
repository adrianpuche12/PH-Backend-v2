package balance.dashboard.controller;

import balance.dashboard.dto.DashboardDTO;
import balance.dashboard.service.DashboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired private MockMvc           mockMvc;
    @Autowired private ObjectMapper      objectMapper;
    @MockBean  private DashboardService  service;

    // ── GET /api/v2/dashboard ─────────────────────────────────────────────────

    @Test
    void getDashboard_returns200WithDashboardData() throws Exception {
        DashboardDTO dto = new DashboardDTO(List.of(), 5L, new BigDecimal("1200.00"));

        when(service.getDashboard()).thenReturn(dto);

        mockMvc.perform(get("/api/v2/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSalesToday").value(5))
                .andExpect(jsonPath("$.totalAmountToday").value(1200.00));
    }

    @Test
    void getDashboard_returns200WithEmptyStores() throws Exception {
        DashboardDTO dto = new DashboardDTO(List.of(), 0L, BigDecimal.ZERO);

        when(service.getDashboard()).thenReturn(dto);

        mockMvc.perform(get("/api/v2/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stores").isArray())
                .andExpect(jsonPath("$.totalSalesToday").value(0));
    }

    @Test
    void getDashboard_callsServiceOnce() throws Exception {
        when(service.getDashboard())
                .thenReturn(new DashboardDTO(List.of(), 0L, BigDecimal.ZERO));

        mockMvc.perform(get("/api/v2/dashboard"));

        verify(service, times(1)).getDashboard();
    }
}
