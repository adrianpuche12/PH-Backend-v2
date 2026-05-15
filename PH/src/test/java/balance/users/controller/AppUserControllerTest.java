package balance.users.controller;

import balance.users.dto.AppUserResponseDTO;
import balance.users.service.AppUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    value = AppUserController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
    }
)
class AppUserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AppUserService userService;

    // ── GET /api/v2/users ─────────────────────────────────────────────────────

    @Test
    void findAll_returns200WithList() throws Exception {
        when(userService.findAll()).thenReturn(List.of(buildUserResponse("cajero01", "ACTIVE")));

        mockMvc.perform(get("/api/v2/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("cajero01"));
    }

    @Test
    void findAll_returns200WithEmptyList() throws Exception {
        when(userService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /api/v2/users/by-username/{username} ──────────────────────────────

    @Test
    void findByUsername_returns200WhenFound() throws Exception {
        when(userService.findByUsername("cajero01")).thenReturn(buildUserResponse("cajero01", "ACTIVE"));

        mockMvc.perform(get("/api/v2/users/by-username/cajero01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("cajero01"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void findByUsername_returns404WhenNotFound() throws Exception {
        when(userService.findByUsername("desconocido"))
                .thenThrow(new IllegalArgumentException("Usuario no encontrado: desconocido"));

        mockMvc.perform(get("/api/v2/users/by-username/desconocido"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/v2/users/store/{storeId} ─────────────────────────────────────

    @Test
    void findByStore_returns200WithFilteredList() throws Exception {
        when(userService.findByStore(1L))
                .thenReturn(List.of(
                        buildUserResponse("cajero01", "ACTIVE"),
                        buildUserResponse("cajero02", "ACTIVE")));

        mockMvc.perform(get("/api/v2/users/store/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── POST /api/v2/users ────────────────────────────────────────────────────

    @Test
    void create_returns200WhenSuccessful() throws Exception {
        when(userService.create(any())).thenReturn(buildUserResponse("cajero01", "ACTIVE"));

        String body = objectMapper.writeValueAsString(Map.of(
                "fullName", "Cajero Uno",
                "username", "cajero01",
                "password", "pass123",
                "storeId", 1
        ));

        mockMvc.perform(post("/api/v2/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("cajero01"));
    }

    @Test
    void create_returns400WhenUsernameAlreadyExists() throws Exception {
        when(userService.create(any()))
                .thenThrow(new IllegalArgumentException("El username 'cajero01' ya está en uso"));

        String body = objectMapper.writeValueAsString(Map.of(
                "fullName", "Cajero Uno",
                "username", "cajero01",
                "password", "pass123",
                "storeId", 1
        ));

        mockMvc.perform(post("/api/v2/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El username 'cajero01' ya está en uso"));
    }

    @Test
    void create_returns400WhenValidationFails() throws Exception {
        // fullName es @NotBlank — omitirlo → 400 sin llamar al servicio
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "cajero01",
                "password", "pass123",
                "storeId", 1
        ));

        mockMvc.perform(post("/api/v2/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void create_returns500WhenKeycloakFails() throws Exception {
        when(userService.create(any()))
                .thenThrow(new RuntimeException("Keycloak no disponible"));

        String body = objectMapper.writeValueAsString(Map.of(
                "fullName", "Cajero",
                "username", "cajero01",
                "password", "pass123",
                "storeId", 1
        ));

        mockMvc.perform(post("/api/v2/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── PUT /api/v2/users/{id}/suspend ────────────────────────────────────────

    @Test
    void suspend_returns200WhenSuccessful() throws Exception {
        when(userService.suspend(1L)).thenReturn(buildUserResponse("cajero01", "SUSPENDED"));

        mockMvc.perform(put("/api/v2/users/1/suspend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void suspend_returns400WhenAlreadySuspended() throws Exception {
        when(userService.suspend(1L))
                .thenThrow(new IllegalStateException("El usuario ya está suspendido"));

        mockMvc.perform(put("/api/v2/users/1/suspend"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void suspend_returns400WhenUserNotFound() throws Exception {
        when(userService.suspend(99L))
                .thenThrow(new IllegalArgumentException("Usuario no encontrado"));

        mockMvc.perform(put("/api/v2/users/99/suspend"))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/v2/users/{id}/activate ──────────────────────────────────────

    @Test
    void activate_returns200WhenSuccessful() throws Exception {
        when(userService.activate(1L)).thenReturn(buildUserResponse("cajero01", "ACTIVE"));

        mockMvc.perform(put("/api/v2/users/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void activate_returns400WhenAlreadyActive() throws Exception {
        when(userService.activate(1L))
                .thenThrow(new IllegalStateException("El usuario ya está activo"));

        mockMvc.perform(put("/api/v2/users/1/activate"))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/v2/users/{id}/reassign ──────────────────────────────────────

    @Test
    void reassign_returns200WhenSuccessful() throws Exception {
        when(userService.reassign(1L, 2L)).thenReturn(buildUserResponse("cajero01", "ACTIVE"));

        mockMvc.perform(put("/api/v2/users/1/reassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("storeId", 2))))
                .andExpect(status().isOk());
    }

    @Test
    void reassign_returns400WhenStoreIdMissing() throws Exception {
        mockMvc.perform(put("/api/v2/users/1/reassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("storeId es obligatorio"));

        verifyNoInteractions(userService);
    }

    // ── PUT /api/v2/users/{id}/reset-password ─────────────────────────────────

    @Test
    void resetPassword_returns200WhenSuccessful() throws Exception {
        doNothing().when(userService).resetPassword(1L, "nuevaPass123");

        mockMvc.perform(put("/api/v2/users/1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "nuevaPass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contraseña actualizada correctamente"));
    }

    @Test
    void resetPassword_returns400WhenPasswordEmpty() throws Exception {
        mockMvc.perform(put("/api/v2/users/1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La nueva contraseña es obligatoria"));

        verifyNoInteractions(userService);
    }

    // ── DELETE /api/v2/users/{id} ─────────────────────────────────────────────

    @Test
    void delete_returns204WhenSuccessful() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/api/v2/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns400WhenUserNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Usuario no encontrado"))
                .when(userService).delete(99L);

        mockMvc.perform(delete("/api/v2/users/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private AppUserResponseDTO buildUserResponse(String username, String status) {
        AppUserResponseDTO dto = new AppUserResponseDTO();
        ReflectionTestUtils.setField(dto, "fullName",  "Empleado Test");
        ReflectionTestUtils.setField(dto, "username",  username);
        ReflectionTestUtils.setField(dto, "status",    status);
        ReflectionTestUtils.setField(dto, "storeId",   1L);
        ReflectionTestUtils.setField(dto, "storeName", "Danli");
        return dto;
    }
}
