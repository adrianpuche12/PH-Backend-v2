package balance.users.service;

import balance.model.Store;
import balance.repository.StoreRepository;
import balance.users.model.AppUser;
import balance.users.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServicePermissionsTest {

    @Mock AppUserRepository    userRepository;
    @Mock StoreRepository      storeRepository;
    @Mock KeycloakAdminService keycloakAdmin;
    @Mock EmailService         emailService;

    @InjectMocks AppUserService service;

    // ── updatePermissions: validación de nombres ──────────────────────────────

    @Test
    void updatePermissions_accepts_all_valid_sections() {
        AppUser user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        List<String> allValid = List.of(
            "POS", "SALES_HISTORY", "INVENTORY", "TRANSACTIONS",
            "BANK_DEPOSITS", "SALARY_PAYMENTS", "SUPPLIER_PAYMENTS", "FORMS"
        );

        assertThatCode(() -> service.updatePermissions(1L, allValid)).doesNotThrowAnyException();
    }

    @Test
    void updatePermissions_accepts_empty_list() {
        AppUser user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        assertThatCode(() -> service.updatePermissions(1L, List.of())).doesNotThrowAnyException();
    }

    @Test
    void updatePermissions_accepts_null_list() {
        AppUser user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        assertThatCode(() -> service.updatePermissions(1L, null)).doesNotThrowAnyException();
    }

    @Test
    void updatePermissions_rejects_unknown_section() {
        AppUser user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updatePermissions(1L, List.of("POS", "SECCION_FALSA")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SECCION_FALSA");
    }

    @Test
    void updatePermissions_rejects_lowercase_section() {
        AppUser user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updatePermissions(1L, List.of("pos")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("pos");
    }

    @Test
    void updatePermissions_rejects_multiple_invalid_sections_in_one_call() {
        AppUser user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updatePermissions(1L, List.of("TYPO1", "TYPO2")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Permisos inválidos");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AppUser buildUser(Long id) {
        AppUser u = new AppUser();
        u.setKeycloakId("kc-" + id);
        u.setFullName("Test User");
        u.setUsername("user" + id);
        u.setStatus("ACTIVE");
        u.setPermissions(new ArrayList<>());
        u.setAccessibleStores(new ArrayList<>());
        return u;
    }
}
