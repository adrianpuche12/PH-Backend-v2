package balance.users.controller;

import balance.users.dto.AppUserRequestDTO;
import balance.users.dto.AppUserResponseDTO;
import balance.users.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/users")
public class AppUserController {

    @Autowired private AppUserService userService;

    @GetMapping
    public ResponseEntity<List<AppUserResponseDTO>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/accessible-stores")
    public ResponseEntity<?> getAccessibleStores(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String keycloakId = extractSub(authHeader.substring(7));
            if (keycloakId == null) return ResponseEntity.badRequest().body(Map.of("error", "Token inválido"));
            return ResponseEntity.ok(userService.getAccessibleStores(keycloakId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al cargar locales"));
        }
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<?> findByUsername(@PathVariable String username) {
        try {
            AppUserResponseDTO user = userService.findByUsername(username);
            if ("SUSPENDED".equals(user.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "ACCOUNT_SUSPENDED",
                                     "message", "Tu cuenta fue suspendida. Contacta al encargado."));
            }
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/by-keycloak/{keycloakId}")
    public ResponseEntity<?> findByKeycloakId(@PathVariable String keycloakId) {
        try {
            AppUserResponseDTO user = userService.findByKeycloakId(keycloakId);
            if ("SUSPENDED".equals(user.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "ACCOUNT_SUSPENDED",
                                     "message", "Tu cuenta fue suspendida. Contacta al encargado."));
            }
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<AppUserResponseDTO>> findByStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(userService.findByStore(storeId));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AppUserRequestDTO dto) {
        try {
            return ResponseEntity.ok(userService.create(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al crear usuario: " + e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        String username    = body.get("username");
        String newPassword = body.get("newPassword");
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username es obligatorio"));
        }
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "newPassword es obligatorio"));
        }
        try {
            userService.changePassword(username, newPassword);
            return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<?> updatePermissions(
            @PathVariable Long id,
            @RequestBody Map<String, List<String>> body) {
        if (!body.containsKey("permissions")) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo 'permissions' es obligatorio"));
        }
        try {
            return ResponseEntity.ok(userService.updatePermissions(id, body.get("permissions")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/store-access")
    public ResponseEntity<?> updateStoreAccess(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> body) {
        try {
            return ResponseEntity.ok(userService.updateStoreAccess(id, body.get("storeIds")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<?> suspend(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.suspend(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.activate(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reassign")
    public ResponseEntity<?> reassign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        try {
            Long newStoreId = body.get("storeId");
            if (newStoreId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "storeId es obligatorio"));
            }
            return ResponseEntity.ok(userService.reassign(id, newStoreId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String newPassword = body.get("password");
            if (newPassword == null || newPassword.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La nueva contraseña es obligatoria"));
            }
            userService.resetPassword(id, newPassword);
            return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractSub(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payload = parts[1];
            int mod = payload.length() % 4;
            if (mod == 2) payload += "==";
            else if (mod == 3) payload += "=";
            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            Map<String, Object> claims = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(decoded, Map.class);
            Object sub = claims.get("sub");
            return sub != null ? sub.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
