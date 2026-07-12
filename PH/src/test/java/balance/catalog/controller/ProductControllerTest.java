package balance.catalog.controller;

import balance.catalog.dto.ProductRequestDTO;
import balance.catalog.dto.ProductResponseDTO;
import balance.catalog.repository.ProductRecipeRepository;
import balance.catalog.repository.ProductRepository;
import balance.catalog.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired private MockMvc                  mockMvc;
    @Autowired private ObjectMapper             objectMapper;
    @MockBean  private ProductService           service;
    @MockBean  private ProductRecipeRepository  recipeRepository;
    @MockBean  private ProductRepository        productRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ProductResponseDTO buildResponse(Long id, String name, String sku) {
        ProductResponseDTO dto = new ProductResponseDTO();
        ReflectionTestUtils.setField(dto, "id",     id);
        ReflectionTestUtils.setField(dto, "name",   name);
        ReflectionTestUtils.setField(dto, "sku",    sku);
        ReflectionTestUtils.setField(dto, "type",   "SIMPLE");
        ReflectionTestUtils.setField(dto, "price",  new BigDecimal("45.00"));
        ReflectionTestUtils.setField(dto, "active", true);
        return dto;
    }

    private ProductRequestDTO buildRequest(String name) {
        ProductRequestDTO req = new ProductRequestDTO();
        req.setName(name);
        req.setPrice(new BigDecimal("45.00"));
        req.setType("SIMPLE");
        return req;
    }

    // ── GET /api/v2/stores/{storeId}/products ─────────────────────────────────

    @Test
    void getByStore_returns200WithList() throws Exception {
        when(service.findByStore(1L, null, null, null))
                .thenReturn(List.of(buildResponse(1L, "Pieza de Pollo", "POL-001")));

        mockMvc.perform(get("/api/v2/stores/1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pieza de Pollo"));
    }

    @Test
    void getByStore_acceptsOptionalFilters() throws Exception {
        when(service.findByStore(1L, true, 5L, "pollo"))
                .thenReturn(List.of(buildResponse(1L, "Pollo Entero", "POL-010")));

        mockMvc.perform(get("/api/v2/stores/1/products")
                        .param("active", "true")
                        .param("categoryId", "5")
                        .param("search", "pollo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("POL-010"));
    }

    // ── GET /api/v2/products/{id} ─────────────────────────────────────────────

    @Test
    void getById_returns200WhenFound() throws Exception {
        when(service.findById(1L)).thenReturn(Optional.of(buildResponse(1L, "Pieza", "P-001")));

        mockMvc.perform(get("/api/v2/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pieza"));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        when(service.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v2/products/99"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/v2/stores/{storeId}/products ────────────────────────────────

    @Test
    void create_returns201WhenValid() throws Exception {
        when(service.create(eq(1L), any())).thenReturn(Optional.of(buildResponse(10L, "Nuevo", "N-001")));

        mockMvc.perform(post("/api/v2/stores/1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Nuevo"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void create_returns404WhenStoreNotFound() throws Exception {
        when(service.create(eq(99L), any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v2/stores/99/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("X"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns400WhenSkuDuplicated() throws Exception {
        when(service.create(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Ya existe un producto con ese SKU en este local"));

        mockMvc.perform(post("/api/v2/stores/1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Duplicado"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void create_returns400WhenNameBlank() throws Exception {
        ProductRequestDTO req = new ProductRequestDTO();
        req.setName("");
        req.setPrice(new BigDecimal("10.00"));

        mockMvc.perform(post("/api/v2/stores/1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/v2/products/{id} ─────────────────────────────────────────────

    @Test
    void update_returns200WhenFound() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(Optional.of(buildResponse(1L, "Editado", "E-001")));

        mockMvc.perform(put("/api/v2/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Editado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Editado"));
    }

    @Test
    void update_returns404WhenNotFound() throws Exception {
        when(service.update(eq(99L), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v2/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("X"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returns400WhenSkuConflicts() throws Exception {
        when(service.update(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Ya existe un producto con ese SKU en este local"));

        mockMvc.perform(put("/api/v2/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Conflicto"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── PUT /api/v2/products/{id}/toggle ──────────────────────────────────────

    @Test
    void toggle_returns200WithToggledProduct() throws Exception {
        ProductResponseDTO inactive = buildResponse(1L, "Pollo", "P-001");
        ReflectionTestUtils.setField(inactive, "active", false);
        when(service.toggle(1L)).thenReturn(Optional.of(inactive));

        mockMvc.perform(put("/api/v2/products/1/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void toggle_returns404WhenNotFound() throws Exception {
        when(service.toggle(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v2/products/99/toggle"))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/v2/products/{id} ──────────────────────────────────────────

    @Test
    void delete_returns204WhenDeleted() throws Exception {
        when(service.delete(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v2/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404WhenNotFound() throws Exception {
        when(service.delete(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/v2/products/99"))
                .andExpect(status().isNotFound());
    }
}
