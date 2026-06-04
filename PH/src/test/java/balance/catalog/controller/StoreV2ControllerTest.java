package balance.catalog.controller;

import balance.catalog.dto.StoreRequestDTO;
import balance.catalog.dto.StoreResponseDTO;
import balance.catalog.service.StoreV2Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StoreV2Controller.class)
class StoreV2ControllerTest {

    @Autowired private MockMvc       mockMvc;
    @Autowired private ObjectMapper  objectMapper;
    @MockBean  private StoreV2Service service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StoreResponseDTO buildResponse(Long id, String name, boolean active) {
        StoreResponseDTO dto = new StoreResponseDTO();
        org.springframework.test.util.ReflectionTestUtils.setField(dto, "id",     id);
        org.springframework.test.util.ReflectionTestUtils.setField(dto, "name",   name);
        org.springframework.test.util.ReflectionTestUtils.setField(dto, "active", active);
        return dto;
    }

    private StoreRequestDTO buildRequest(String name) {
        StoreRequestDTO req = new StoreRequestDTO();
        req.setName(name);
        return req;
    }

    // ── GET /api/v2/stores ────────────────────────────────────────────────────

    @Test
    void getAll_returns200WithList() throws Exception {
        when(service.findAll()).thenReturn(List.of(buildResponse(1L, "Danlí", true)));

        mockMvc.perform(get("/api/v2/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Danlí"));
    }

    // ── GET /api/v2/stores/active ─────────────────────────────────────────────

    @Test
    void getActive_returns200WithActiveStores() throws Exception {
        when(service.findAllActive()).thenReturn(List.of(buildResponse(1L, "Danlí", true)));

        mockMvc.perform(get("/api/v2/stores/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    // ── GET /api/v2/stores/{id} ───────────────────────────────────────────────

    @Test
    void getById_returns200WhenFound() throws Exception {
        when(service.findById(1L)).thenReturn(Optional.of(buildResponse(1L, "Danlí", true)));

        mockMvc.perform(get("/api/v2/stores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Danlí"));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        when(service.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v2/stores/99"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/v2/stores ───────────────────────────────────────────────────

    @Test
    void create_returns201WhenValid() throws Exception {
        when(service.create(any())).thenReturn(buildResponse(10L, "Nueva", true));

        mockMvc.perform(post("/api/v2/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Nueva"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void create_returns400WhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/v2/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(""))))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/v2/stores/{id} ───────────────────────────────────────────────

    @Test
    void update_returns200WhenFound() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(Optional.of(buildResponse(1L, "Editado", true)));

        mockMvc.perform(put("/api/v2/stores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Editado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Editado"));
    }

    @Test
    void update_returns404WhenNotFound() throws Exception {
        when(service.update(eq(99L), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v2/stores/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("X"))))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/v2/stores/{id}/toggle ────────────────────────────────────────

    @Test
    void toggle_returns200WhenFound() throws Exception {
        when(service.toggle(1L)).thenReturn(Optional.of(buildResponse(1L, "Danlí", false)));

        mockMvc.perform(put("/api/v2/stores/1/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void toggle_returns404WhenNotFound() throws Exception {
        when(service.toggle(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v2/stores/99/toggle"))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/v2/stores/{id} ────────────────────────────────────────────

    @Test
    void delete_returns204WhenDeleted() throws Exception {
        when(service.delete(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v2/stores/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404WhenNotFound() throws Exception {
        when(service.delete(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/v2/stores/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns400WhenStoreHasHistory() throws Exception {
        when(service.delete(1L)).thenThrow(new IllegalStateException("historial de operaciones"));

        mockMvc.perform(delete("/api/v2/stores/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
