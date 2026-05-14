package balance.users.service;

import balance.model.Store;
import balance.repository.StoreRepository;
import balance.users.dto.AppUserRequestDTO;
import balance.users.dto.AppUserResponseDTO;
import balance.users.model.AppUser;
import balance.users.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppUserService {

    @Autowired private AppUserRepository     userRepository;
    @Autowired private StoreRepository       storeRepository;
    @Autowired private KeycloakAdminService  keycloakAdmin;

    // ── Listar usuarios ───────────────────────────────────────────────────────

    public List<AppUserResponseDTO> findAll() {
        return userRepository.findAllByOrderByFullNameAsc()
                .stream().map(AppUserResponseDTO::from).toList();
    }

    public List<AppUserResponseDTO> findByStore(Long storeId) {
        return userRepository.findByStoreIdOrderByFullNameAsc(storeId)
                .stream().map(AppUserResponseDTO::from).toList();
    }

    // ── Crear usuario ─────────────────────────────────────────────────────────

    /**
     * Crea el usuario en nuestra BD y en Keycloak con rol 'user'.
     * Si falla Keycloak, la transacción se revierte.
     */
    @Transactional
    public AppUserResponseDTO create(AppUserRequestDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername().trim().toLowerCase())) {
            throw new IllegalArgumentException("El username '" + dto.getUsername() + "' ya está en uso");
        }

        Store store = storeRepository.findById(dto.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Local no encontrado"));

        // 1. Crear en Keycloak y obtener el keycloakId
        String keycloakId = keycloakAdmin.createUser(
            dto.getUsername(), dto.getFullName(), dto.getPassword()
        );

        // 2. Guardar en nuestra BD
        AppUser user = new AppUser();
        user.setKeycloakId(keycloakId);
        user.setFullName(dto.getFullName().trim());
        user.setUsername(dto.getUsername().trim().toLowerCase());
        user.setStore(store);
        user.setStatus("ACTIVE");

        return AppUserResponseDTO.from(userRepository.save(user));
    }

    // ── Suspender / Activar ───────────────────────────────────────────────────

    /** Suspende el usuario — no puede iniciar sesión hasta que se reactive. */
    @Transactional
    public AppUserResponseDTO suspend(Long id) {
        AppUser user = findOrThrow(id);
        if ("SUSPENDED".equals(user.getStatus())) {
            throw new IllegalStateException("El usuario ya está suspendido");
        }
        keycloakAdmin.setUserEnabled(user.getKeycloakId(), false);
        user.setStatus("SUSPENDED");
        return AppUserResponseDTO.from(userRepository.save(user));
    }

    /** Reactiva el acceso del usuario. */
    @Transactional
    public AppUserResponseDTO activate(Long id) {
        AppUser user = findOrThrow(id);
        if ("ACTIVE".equals(user.getStatus())) {
            throw new IllegalStateException("El usuario ya está activo");
        }
        keycloakAdmin.setUserEnabled(user.getKeycloakId(), true);
        user.setStatus("ACTIVE");
        return AppUserResponseDTO.from(userRepository.save(user));
    }

    // ── Reasignar local ───────────────────────────────────────────────────────

    /** Cambia el local al que está asignado el usuario. */
    @Transactional
    public AppUserResponseDTO reassign(Long id, Long newStoreId) {
        AppUser user = findOrThrow(id);
        Store store = storeRepository.findById(newStoreId)
                .orElseThrow(() -> new IllegalArgumentException("Local no encontrado"));
        user.setStore(store);
        return AppUserResponseDTO.from(userRepository.save(user));
    }

    // ── Resetear contraseña ───────────────────────────────────────────────────

    /** El admin resetea la contraseña de un usuario. */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        AppUser user = findOrThrow(id);
        keycloakAdmin.resetPassword(user.getKeycloakId(), newPassword);
    }

    // ── Eliminar usuario ──────────────────────────────────────────────────────

    /** Elimina el usuario de nuestra BD y de Keycloak permanentemente. */
    @Transactional
    public void delete(Long id) {
        AppUser user = findOrThrow(id);
        keycloakAdmin.deleteUser(user.getKeycloakId());
        userRepository.delete(user);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private AppUser findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }
}
