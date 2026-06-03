package balance.catalog.controller;

import balance.catalog.dto.CategoryRequestDTO;
import balance.catalog.dto.CategoryResponseDTO;
import balance.catalog.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired private MockMvc        mockMvc;
    @Autowired private ObjectMapper   objectMapper;
    @MockBean  private CategoryService service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CategoryResponseDTO buildResponse(Long id, String name) {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        org.springframework.test.util.ReflectionTestUtils.setField(dto, "id",     id);
        org.springframework.test.util.ReflectionTestUtils.setField(dto, "name",   name);
        org.springframework.test.util.ReflectionTestUtils.setField(dto, "active", true);
        org.springframework.test.util.ReflectionTestUtils.setField(dto, "children", new ArrayList<>());
        return dto;
    }

    private CategoryRequestDTO buildRequest(String name) {
        CategoryRequestDTO req = new CategoryRequestDTO();
        req.setName(name);
        return req;
    }

    // ── GET /api/v2/stores/{storeId}/categories ───────────────────────────────

    @Test
    void getTree_returns200WithList() throws Exception {
        when(service.getTree(1L)).thenReturn(List.of(buildResponse(1L, "Bebidas")));

        mockMvc.perform(get("/api/v2/stores/1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bebidas"));
    }

    @Test
    void getTree_returns200WithEmptyList() throws Exception {
        when(service.getTree(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/stores/1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/v2/categories/{id} ───────────────────────────────────────────

    @Test
    void getById_returns200WhenFound() throws Exception {
        when(service.findById(1L)).thenReturn(Optional.of(buildResponse(1L, "Pollos")));

        mockMvc.perform(get("/api/v2/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pollos"));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        when(service.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v2/categories/99"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/v2/stores/{storeId}/categories ──────────────────────────────

    @Test
    void createRoot_returns201WhenValid() throws Exception {
        when(service.createRoot(eq(1L), any())).thenReturn(Optional.of(buildResponse(10L, "Nueva")));

        mockMvc.perform(post("/api/v2/stores/1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Nueva"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void createRoot_returns404WhenStoreNotFound() throws Exception {
        when(service.createRoot(eq(99L), any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v2/stores/99/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("X"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRoot_returns400WhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/v2/stores/1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(""))))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/v2/stores/{storeId}/categories/{parentId}/children ──────────

    @Test
    void createChild_returns201WhenValid() throws Exception {
        when(service.createChild(eq(1L), eq(5L), any()))
                .thenReturn(Optional.of(buildResponse(20L, "Gaseosas")));

        mockMvc.perform(post("/api/v2/stores/1/categories/5/children")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Gaseosas"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Gaseosas"));
    }

    @Test
    void createChild_returns404WhenParentNotFound() throws Exception {
        when(service.createChild(eq(1L), eq(99L), any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v2/stores/1/categories/99/children")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("X"))))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/v2/categories/{id} ───────────────────────────────────────────

    @Test
    void update_returns200WhenFound() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(Optional.of(buildResponse(1L, "Editada")));

        mockMvc.perform(put("/api/v2/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Editada"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Editada"));
    }

    @Test
    void update_returns404WhenNotFound() throws Exception {
        when(service.update(eq(99L), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v2/categories/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("X"))))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/v2/categories/{id}/toggle ────────────────────────────────────

    @Test
    void toggle_returns200WithToggledCategory() throws Exception {
        CategoryResponseDTO inactive = buildResponse(1L, "Bebidas");
        org.springframework.test.util.ReflectionTestUtils.setField(inactive, "active", false);
        when(service.toggle(1L)).thenReturn(Optional.of(inactive));

        mockMvc.perform(put("/api/v2/categories/1/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void toggle_returns404WhenNotFound() throws Exception {
        when(service.toggle(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v2/categories/99/toggle"))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/v2/categories/{id} ────────────────────────────────────────

    @Test
    void delete_returns204WhenDeleted() throws Exception {
        when(service.delete(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v2/categories/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404WhenNotFound() throws Exception {
        when(service.delete(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/v2/categories/99"))
                .andExpect(status().isNotFound());
    }
}
