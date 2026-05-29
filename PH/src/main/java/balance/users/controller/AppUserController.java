package balance.users.controller;

import balance.users.dto.AppUserRequestDTO;
import balance.users.dto.AppUserResponseDTO;
import balance.users.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/users")
public class AppUserController {

    @Autowired private AppUserService userService;

    /** Lista todos los usuarios del sistema. */
    @GetMapping
    public ResponseEntity<List<AppUserResponseDTO>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    /**
     * Retorna el perfil de un empleado por su username.
     * Devuelve HTTP 403 con error=ACCOUNT_SUSPENDED si el usuario esta suspendido.
     */
    @GetMapping("/by-username/{username}")
    public ResponseEntity<?> findByUsername(@PathVariable String username) {
        try {
            AppUserResponseDTO user = userService.findByUsername(username);
            if ("SUSPENDED".equals(user.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "error",   "ACCOUNT_SUSPENDED",
                            "message", "Tu cuenta fue suspendida. Contacta al encargado."
                        ));
            }
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Busca por keycloakId (sub del JWT). Mas confiable que by-username.
     * Devuelve 403 ACCOUNT_SUSPENDED si esta suspendido, 404 si no existe (usuario admin).
     */
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

    /** Lista usuarios por local. */
    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<AppUserResponseDTO>> findByStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(userService.findByStore(storeId));
    }

    /** Crea un usuario nuevo (crea en Keycloak + guarda en BD). */
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

    /** Suspende el acceso del usuario (no puede iniciar sesion). */
    @PutMapping("/{id}/suspend")
    public ResponseEntity<?> suspend(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.suspend(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Reactiva el acceso del usuario. */
    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.activate(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Reasigna el usuario a otro local. */
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

    /** Resetea la contrasena del usuario. */
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String newPassword = body.get("password");
            if (newPassword == null || newPassword.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La nueva contrasena es obligatoria"));
            }
            userService.resetPassword(id, newPassword);
            return ResponseEntity.ok(Map.of("message", "Contrasena actualizada correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Elimina el usuario permanentemente de Keycloak y de la BD. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
