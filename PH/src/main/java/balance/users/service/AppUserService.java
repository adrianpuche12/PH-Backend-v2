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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AppUserService {

    private static final String PASSWORD_CHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz!@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    // Todas las secciones conocidas por el backend (SECTION_RULES + legado)
    static final Set<String> VALID_SECTIONS = Set.of(
        "POS", "SALES_HISTORY", "INVENTORY", "TRANSACTIONS",
        "BANK_DEPOSITS", "SALARY_PAYMENTS", "SUPPLIER_PAYMENTS", "FORMS",
        "CATALOG", "DASHBOARD"
    );

    private static void validatePermissions(List<String> permissions) {
        if (permissions == null) return;
        List<String> invalid = permissions.stream()
            .filter(p -> !VALID_SECTIONS.contains(p))
            .toList();
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Permisos inválidos: " + invalid);
        }
    }

    @Autowired private AppUserRepository    userRepository;
    @Autowired private StoreRepository      storeRepository;
    @Autowired private KeycloakAdminService keycloakAdmin;
    @Autowired private EmailService         emailService;

    // ── Listar ───────────────────────────────────────────────────────────────

    public List<AppUserResponseDTO> findAll() {
        return userRepository.findAllByOrderByFullNameAsc()
                .stream().map(AppUserResponseDTO::from).toList();
    }

    public List<AppUserResponseDTO> findByStore(Long storeId) {
        return userRepository.findByStoreIdOrderByFullNameAsc(storeId)
                .stream().map(AppUserResponseDTO::from).toList();
    }

    // ── Crear usuario ─────────────────────────────────────────────────────────

    @Transactional
    public AppUserResponseDTO create(AppUserRequestDTO dto) {
        String username = dto.getUsername().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("El username '" + username + "' ya está en uso");
        }

        Store primaryStore = null;
        if (dto.getStoreId() != null) {
            primaryStore = storeRepository.findById(dto.getStoreId())
                    .orElseThrow(() -> new IllegalArgumentException("Local no encontrado: " + dto.getStoreId()));
        }

        List<Store> accessibleStores = new ArrayList<>();
        for (Long sid : dto.getStoreIds()) {
            accessibleStores.add(
                storeRepository.findById(sid)
                    .orElseThrow(() -> new IllegalArgumentException("Local no encontrado: " + sid))
            );
        }

        String password = dto.getPassword();
        String role     = dto.getRole() != null ? dto.getRole().toUpperCase() : "ENCARGADO";
        String email    = dto.getEmail() != null && !dto.getEmail().isBlank()
                          ? dto.getEmail().trim()
                          : username + "@lospolloshermanos.hn";
        String kcRole   = "ADMIN".equalsIgnoreCase(role) ? "admin" : "user";

        String keycloakId = keycloakAdmin.createUser(username, dto.getFullName(), email, password, kcRole);

        AppUser user = new AppUser();
        user.setKeycloakId(keycloakId);
        user.setFullName(dto.getFullName().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setFirstLogin(true);
        user.setStore(primaryStore);
        user.setAccessibleStores(accessibleStores);
        validatePermissions(dto.getPermissions());
        user.setPermissions(dto.getPermissions());
        user.setStatus("ACTIVE");

        AppUser saved = userRepository.save(user);

        AppUserResponseDTO response = AppUserResponseDTO.from(saved);
        response.setTempPassword(password);
        return response;
    }

    // ── Cambio de contraseña (primer login) ──────────────────────────────────

    @Transactional
    public void changePassword(String username, String newPassword) {
        AppUser user = userRepository.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        keycloakAdmin.resetPassword(user.getKeycloakId(), newPassword);
        user.setFirstLogin(false);
        userRepository.save(user);
    }

    // ── Actualizar permisos de secciones ─────────────────────────────────────

    @Transactional
    public AppUserResponseDTO updatePermissions(Long id, List<String> permissions) {
        AppUser user = findOrThrow(id);
        validatePermissions(permissions);
        user.setPermissions(permissions != null ? permissions : new ArrayList<>());
        return AppUserResponseDTO.from(userRepository.save(user));
    }

    // ── Actualizar locales accesibles ─────────────────────────────────────────

    @Transactional
    public AppUserResponseDTO updateStoreAccess(Long id, List<Long> storeIds) {
        AppUser user = findOrThrow(id);
        List<Store> stores = new ArrayList<>();
        for (Long sid : storeIds) {
            stores.add(
                storeRepository.findById(sid)
                    .orElseThrow(() -> new IllegalArgumentException("Local no encontrado: " + sid))
            );
        }
        user.setAccessibleStores(stores);
        return AppUserResponseDTO.from(userRepository.save(user));
    }

    // ── Suspender / Activar ───────────────────────────────────────────────────

    @Transactional
    public AppUserResponseDTO suspend(Long id) {
        AppUser user = findOrThrow(id);
        if ("SUSPENDED".equals(user.getStatus())) {
            throw new IllegalStateException("El usuario ya está suspendido");
        }
        keycloakAdmin.setUserEnabled(user.getKeycloakId(), false);
        keycloakAdmin.logoutUser(user.getKeycloakId());
        user.setStatus("SUSPENDED");
        return AppUserResponseDTO.from(userRepository.save(user));
    }

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

    // ── Reasignar local principal ─────────────────────────────────────────────

    @Transactional
    public AppUserResponseDTO reassign(Long id, Long newStoreId) {
        AppUser user = findOrThrow(id);
        Store store = storeRepository.findById(newStoreId)
                .orElseThrow(() -> new IllegalArgumentException("Local no encontrado"));
        user.setStore(store);
        return AppUserResponseDTO.from(userRepository.save(user));
    }

    // ── Reset contraseña por admin ────────────────────────────────────────────

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        AppUser user = findOrThrow(id);
        keycloakAdmin.resetPassword(user.getKeycloakId(), newPassword);
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        AppUser user = findOrThrow(id);
        keycloakAdmin.deleteUser(user.getKeycloakId());
        userRepository.delete(user);
    }

    // ── Buscar ────────────────────────────────────────────────────────────────

    public AppUserResponseDTO findByUsername(String username) {
        return userRepository.findByUsername(username.toLowerCase())
                .map(AppUserResponseDTO::from)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }

    public AppUserResponseDTO findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .map(AppUserResponseDTO::from)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    // ── Locales accesibles (para no-admins en el dashboard) ───────────────────

    public List<java.util.Map<String, Object>> getAccessibleStores(String keycloakId) {
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        List<Store> stores = user.getAccessibleStores().isEmpty()
            ? storeRepository.findByActiveTrueOrderByNameAsc()
            : user.getAccessibleStores().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .toList();
        return stores.stream()
            .map(s -> java.util.Map.<String, Object>of("id", s.getId(), "name", s.getName()))
            .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AppUser findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
