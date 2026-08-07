package balance.users.service;

import balance.model.Store;
import balance.repository.StoreRepository;
import balance.users.dto.AppUserRequestDTO;
import balance.users.dto.AppUserResponseDTO;
import balance.users.model.AppUser;
import balance.users.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @InjectMocks private AppUserService appUserService;

    @Mock private AppUserRepository    userRepository;
    @Mock private StoreRepository      storeRepository;
    @Mock private KeycloakAdminService keycloakAdmin;
    @Mock private EmailService         emailService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Store buildStore(Long id, String name) {
        Store s = new Store();
        s.setId(id);
        s.setName(name);
        return s;
    }

    private AppUser buildUser(Long id, String username, String status) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setFullName("Empleado Test");
        u.setKeycloakId("kc-uuid-" + id);
        u.setStatus(status);
        u.setStore(buildStore(1L, "Danli"));
        return u;
    }

    private AppUserRequestDTO buildRequest(String username, String fullName, Long storeId) {
        AppUserRequestDTO dto = new AppUserRequestDTO();
        dto.setUsername(username);
        dto.setFullName(fullName);
        dto.setPassword("pass123");
        dto.setRole("ENCARGADO");
        dto.setStoreId(storeId);
        return dto;
    }

    // ── create — normalización ────────────────────────────────────────────────

    @Test
    void create_normalizesUsernameToLowercase() {
        when(userRepository.existsByUsername("cajero01")).thenReturn(false);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(keycloakAdmin.createUser(any(), any(), any(), any(), any())).thenReturn("kc-uuid-nuevo");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appUserService.create(buildRequest("CAJERO01", "Cajero Uno", 1L));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("cajero01");
    }

    @Test
    void create_trimesUsernameWhitespace() {
        when(userRepository.existsByUsername("cajero01")).thenReturn(false);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(keycloakAdmin.createUser(any(), any(), any(), any(), any())).thenReturn("kc-uuid-nuevo");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appUserService.create(buildRequest("  cajero01  ", "Cajero Uno", 1L));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("cajero01");
    }

    @Test
    void create_savesKeycloakIdReturnedByKeycloak() {
        when(userRepository.existsByUsername("cajero01")).thenReturn(false);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(keycloakAdmin.createUser(any(), any(), any(), any(), any())).thenReturn("kc-uuid-abc123");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appUserService.create(buildRequest("cajero01", "Cajero", 1L));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getKeycloakId()).isEqualTo("kc-uuid-abc123");
    }

    @Test
    void create_setsStatusToActiveByDefault() {
        when(userRepository.existsByUsername("cajero01")).thenReturn(false);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(keycloakAdmin.createUser(any(), any(), any(), any(), any())).thenReturn("kc-uuid-nuevo");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appUserService.create(buildRequest("cajero01", "Cajero", 1L));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    // ── create — validaciones ─────────────────────────────────────────────────

    @Test
    void create_throwsWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("cajero01")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.create(buildRequest("cajero01", "Cajero", 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está en uso");

        verifyNoInteractions(keycloakAdmin);
    }

    @Test
    void create_throwsWhenStoreNotFound() {
        when(userRepository.existsByUsername("cajero01")).thenReturn(false);
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.create(buildRequest("cajero01", "Cajero", 99L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Local no encontrado");

        verifyNoInteractions(keycloakAdmin);
    }

    // ── suspend ───────────────────────────────────────────────────────────────

    @Test
    void suspend_changesStatusToSuspended() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUserResponseDTO result = appUserService.suspend(1L);

        assertThat(result.getStatus()).isEqualTo("SUSPENDED");
    }

    @Test
    void suspend_disablesUserInKeycloak() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appUserService.suspend(1L);

        verify(keycloakAdmin).setUserEnabled("kc-uuid-1", false);
    }

    @Test
    void suspend_throwsWhenUserAlreadySuspended() {
        AppUser user = buildUser(1L, "cajero01", "SUSPENDED");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.suspend(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya está suspendido");
    }

    @Test
    void suspend_throwsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.suspend(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ── activate ──────────────────────────────────────────────────────────────

    @Test
    void activate_changesStatusToActive() {
        AppUser user = buildUser(1L, "cajero01", "SUSPENDED");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUserResponseDTO result = appUserService.activate(1L);

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void activate_enablesUserInKeycloak() {
        AppUser user = buildUser(1L, "cajero01", "SUSPENDED");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appUserService.activate(1L);

        verify(keycloakAdmin).setUserEnabled("kc-uuid-1", true);
    }

    @Test
    void activate_throwsWhenUserAlreadyActive() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.activate(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya está activo");
    }

    // ── reassign ──────────────────────────────────────────────────────────────

    @Test
    void reassign_changesUserStore() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        Store newStore = buildStore(2L, "El Paraíso");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(storeRepository.findById(2L)).thenReturn(Optional.of(newStore));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appUserService.reassign(1L, 2L);

        assertThat(user.getStore().getId()).isEqualTo(2L);
    }

    @Test
    void reassign_throwsWhenNewStoreNotFound() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.reassign(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Local no encontrado");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_removesUserFromKeycloakAndDatabase() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        appUserService.delete(1L);

        verify(keycloakAdmin).deleteUser("kc-uuid-1");
        verify(userRepository).delete(user);
    }

    @Test
    void delete_callsKeycloakBeforeDatabase() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Verificar orden: Keycloak primero, luego BD
        var inOrder = inOrder(keycloakAdmin, userRepository);
        appUserService.delete(1L);
        inOrder.verify(keycloakAdmin).deleteUser("kc-uuid-1");
        inOrder.verify(userRepository).delete(user);
    }

    // ── findByUsername ────────────────────────────────────────────────────────

    @Test
    void findByUsername_normalizesToLowercase() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        when(userRepository.findByUsername("cajero01")).thenReturn(Optional.of(user));

        appUserService.findByUsername("CAJERO01");

        verify(userRepository).findByUsername("cajero01");
    }

    @Test
    void findByUsername_throwsWhenNotFound() {
        when(userRepository.findByUsername("desconocido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.findByUsername("desconocido"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ── create — nuevos campos rol/email/firstLogin ───────────────────────────

    @Test
    void create_setsRoleFromDto() {
        when(userRepository.existsByUsername("cnt01")).thenReturn(false);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(keycloakAdmin.createUser(any(), any(), any(), any(), any())).thenReturn("kc-uuid-cnt");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUserRequestDTO dto = buildRequest("cnt01", "Contador Uno", 1L);
        dto.setRole("CONTADOR");
        appUserService.create(dto);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("CONTADOR");
    }

    @Test
    void create_setsFirstLoginTrue() {
        when(userRepository.existsByUsername("cajero01")).thenReturn(false);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(keycloakAdmin.createUser(any(), any(), any(), any(), any())).thenReturn("kc-uuid");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appUserService.create(buildRequest("cajero01", "Cajero", 1L));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isFirstLogin()).isTrue();
    }

    @Test
    void create_setsEmailFromDto() {
        when(userRepository.existsByUsername("cajero01")).thenReturn(false);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(keycloakAdmin.createUser(any(), any(), any(), any(), any())).thenReturn("kc-uuid");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUserRequestDTO dto = buildRequest("cajero01", "Cajero", 1L);
        dto.setEmail("cajero01@test.com");
        appUserService.create(dto);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("cajero01@test.com");
    }

    @Test
    void create_usesAdminRoleForKcWhenRoleIsAdmin() {
        when(userRepository.existsByUsername("admin01")).thenReturn(false);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(keycloakAdmin.createUser(any(), any(), any(), any(), any())).thenReturn("kc-uuid");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUserRequestDTO dto = buildRequest("admin01", "Admin Uno", 1L);
        dto.setRole("ADMIN");
        appUserService.create(dto);

        verify(keycloakAdmin).createUser(any(), any(), any(), any(), eq("admin"));
    }

    @Test
    void create_usesUserRoleForKcWhenRoleIsEncargado() {
        when(userRepository.existsByUsername("enc01")).thenReturn(false);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(keycloakAdmin.createUser(any(), any(), any(), any(), any())).thenReturn("kc-uuid");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUserRequestDTO dto = buildRequest("enc01", "Encargado", 1L);
        dto.setRole("ENCARGADO");
        appUserService.create(dto);

        verify(keycloakAdmin).createUser(any(), any(), any(), any(), eq("user"));
    }

    @Test
    void create_withoutStoreIdIsAllowed() {
        when(userRepository.existsByUsername("cnt01")).thenReturn(false);
        when(keycloakAdmin.createUser(any(), any(), any(), any(), any())).thenReturn("kc-uuid-cnt");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUserRequestDTO dto = buildRequest("cnt01", "Contador", null);
        dto.setRole("CONTADOR");
        appUserService.create(dto);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStore()).isNull();
    }

    @Test
    void create_setsPermissionsFromDto() {
        when(userRepository.existsByUsername("enc01")).thenReturn(false);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(keycloakAdmin.createUser(any(), any(), any(), any(), any())).thenReturn("kc-uuid");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUserRequestDTO dto = buildRequest("enc01", "Encargado", 1L);
        dto.setPermissions(List.of("POS", "SALES_HISTORY", "INVENTORY"));
        appUserService.create(dto);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPermissions())
            .containsExactlyInAnyOrder("POS", "SALES_HISTORY", "INVENTORY");
    }

    // ── updatePermissions ─────────────────────────────────────────────────────

    @Test
    void updatePermissions_replacesExistingPermissions() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        user.setPermissions(new ArrayList<>(List.of("POS", "CATALOG")));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUserResponseDTO result = appUserService.updatePermissions(1L, List.of("DASHBOARD", "SALES_HISTORY"));

        assertThat(result.getPermissions()).containsExactlyInAnyOrder("DASHBOARD", "SALES_HISTORY");
    }

    @Test
    void updatePermissions_emptyListMeansFullAccess() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        user.setPermissions(new ArrayList<>(List.of("POS")));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUserResponseDTO result = appUserService.updatePermissions(1L, new ArrayList<>());

        assertThat(result.getPermissions()).isEmpty();
    }

    @Test
    void updatePermissions_nullTreatedAsEmpty() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUserResponseDTO result = appUserService.updatePermissions(1L, null);

        assertThat(result.getPermissions()).isEmpty();
    }

    @Test
    void updatePermissions_throwsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.updatePermissions(99L, List.of("POS")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ── updateStoreAccess ─────────────────────────────────────────────────────

    @Test
    void updateStoreAccess_setsNewStores() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        Store s2 = buildStore(2L, "El Paraíso");
        Store s3 = buildStore(3L, "Tegucigalpa");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(storeRepository.findById(2L)).thenReturn(Optional.of(s2));
        when(storeRepository.findById(3L)).thenReturn(Optional.of(s3));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUserResponseDTO result = appUserService.updateStoreAccess(1L, List.of(2L, 3L));

        assertThat(result.getStoreIds()).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    void updateStoreAccess_emptyListMeansAllStores() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUserResponseDTO result = appUserService.updateStoreAccess(1L, new ArrayList<>());

        assertThat(result.getStoreIds()).isEmpty();
    }

    @Test
    void updateStoreAccess_throwsWhenStoreNotFound() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.updateStoreAccess(1L, List.of(99L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Local no encontrado");
    }

    // ── changePassword ────────────────────────────────────────────────────────

    @Test
    void changePassword_setsFirstLoginFalse() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        user.setFirstLogin(true);
        when(userRepository.findByUsername("cajero01")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appUserService.changePassword("cajero01", "NuevaPass123!");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isFirstLogin()).isFalse();
    }

    @Test
    void changePassword_callsKcResetPassword() {
        AppUser user = buildUser(1L, "cajero01", "ACTIVE");
        when(userRepository.findByUsername("cajero01")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appUserService.changePassword("cajero01", "NuevaPass123!");

        verify(keycloakAdmin).resetPassword("kc-uuid-1", "NuevaPass123!");
    }

    @Test
    void changePassword_throwsWhenUserNotFound() {
        when(userRepository.findByUsername("desconocido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.changePassword("desconocido", "pass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado");
    }
}
