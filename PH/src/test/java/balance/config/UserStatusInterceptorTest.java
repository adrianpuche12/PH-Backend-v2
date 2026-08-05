package balance.config;

import balance.model.Store;
import balance.sales.repository.ShiftExpenseRepository;
import balance.sales.repository.ShiftRepository;
import balance.users.model.AppUser;
import balance.users.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(MockitoExtension.class)
class UserStatusInterceptorTest {

    @Mock AppUserRepository userRepository;
    @Mock ShiftRepository shiftRepository;
    @Mock ShiftExpenseRepository expenseRepository;

    @InjectMocks UserStatusInterceptor interceptor;

    MockHttpServletRequest req;
    MockHttpServletResponse res;

    @BeforeEach
    void setup() {
        req = new MockHttpServletRequest();
        res = new MockHttpServletResponse();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String token(String sub, boolean admin) {
        String roles = admin ? "[\"admin\",\"user\"]" : "[\"user\"]";
        String json  = "{\"sub\":\"" + sub + "\",\"realm_access\":{\"roles\":" + roles + "}}";
        String enc   = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
        return "header." + enc + ".sig";
    }

    private AppUser activeUser(String sub, List<String> permissions, List<Long> storeIds) {
        AppUser u = new AppUser();
        u.setKeycloakId(sub);
        u.setStatus("ACTIVE");
        u.setFullName("Test User");
        u.setUsername("test");
        u.setPermissions(new ArrayList<>(permissions));
        List<Store> stores = new ArrayList<>();
        for (Long id : storeIds) {
            Store s = new Store();
            s.setId(id);
            stores.add(s);
        }
        u.setAccessibleStores(stores);
        return u;
    }

    private void bearer(String sub, boolean admin) {
        req.addHeader("Authorization", "Bearer " + token(sub, admin));
    }

    // ── 1. Preflight OPTIONS siempre pasa ────────────────────────────────────

    @Test
    void options_request_passes() throws Exception {
        req.setMethod("OPTIONS");
        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    // ── 2. Sin header → pasa ─────────────────────────────────────────────────

    @Test
    void no_auth_header_passes() throws Exception {
        req.setMethod("GET");
        req.setRequestURI("/api/v2/users");
        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    // ── 3. Usuario suspendido → 403 ACCOUNT_SUSPENDED ────────────────────────

    @Test
    void suspended_user_gets_403() throws Exception {
        bearer("suspended-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/shifts");

        AppUser suspended = activeUser("suspended-sub", List.of(), List.of());
        suspended.setStatus("SUSPENDED");
        when(userRepository.findByKeycloakId("suspended-sub")).thenReturn(Optional.of(suspended));

        assertThat(interceptor.preHandle(req, res, null)).isFalse();
        assertThat(res.getStatus()).isEqualTo(FORBIDDEN.value());
        assertThat(res.getContentAsString()).contains("ACCOUNT_SUSPENDED");
    }

    // ── 4. Admin JWT → pasa cualquier path ───────────────────────────────────

    @Test
    void admin_jwt_bypasses_all_checks() throws Exception {
        bearer("admin-sub", true);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/users");

        AppUser admin = activeUser("admin-sub", List.of(), List.of());
        when(userRepository.findByKeycloakId("admin-sub")).thenReturn(Optional.of(admin));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
        assertThat(res.getStatus()).isEqualTo(OK.value());
    }

    // ── 5. Usuario no en DB → pasa (admin puro Keycloak) ─────────────────────

    @Test
    void user_not_in_db_passes() throws Exception {
        bearer("kc-only-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/shifts");

        when(userRepository.findByKeycloakId("kc-only-sub")).thenReturn(Optional.empty());

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    // ── 6. Whitelist: by-username siempre pasa para no-admin ─────────────────

    @Test
    void byusername_path_whitelisted_for_non_admin() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/users/by-username/cajero01");

        AppUser u = activeUser("user-sub", List.of("POS"), List.of(1L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    // ── 7. Path admin-only → 403 FORBIDDEN para no-admin ─────────────────────

    @Test
    void admin_only_path_blocks_non_admin() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/users");

        AppUser u = activeUser("user-sub", List.of(), List.of());
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isFalse();
        assertThat(res.getStatus()).isEqualTo(FORBIDDEN.value());
        assertThat(res.getContentAsString()).contains("FORBIDDEN");
    }

    // ── 8. Permisos vacíos = acceso total a secciones ─────────────────────────

    @Test
    void empty_permissions_grants_all_sections() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/stores/1/shifts");

        AppUser u = activeUser("user-sub", List.of(), List.of());  // sin restricciones
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    // ── 9. Sección habilitada → pasa ─────────────────────────────────────────

    @Test
    void user_with_matching_section_passes() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/stores/1/shifts");

        AppUser u = activeUser("user-sub", List.of("SALES_HISTORY"), List.of());
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    // ── 10. Sección no habilitada → 403 SECTION_FORBIDDEN ────────────────────

    @Test
    void user_without_section_gets_403() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/stores/1/shifts");

        AppUser u = activeUser("user-sub", List.of("POS", "INVENTORY"), List.of());
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isFalse();
        assertThat(res.getStatus()).isEqualTo(FORBIDDEN.value());
        assertThat(res.getContentAsString()).contains("SECTION_FORBIDDEN");
    }

    // ── 11. accessibleStores vacío = acceso a todos los locales ───────────────

    @Test
    void empty_accessible_stores_grants_all_stores() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/stores/99/shifts");

        AppUser u = activeUser("user-sub", List.of("SALES_HISTORY"), List.of());
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    // ── 12. Local correcto → pasa ─────────────────────────────────────────────

    @Test
    void user_accessing_allowed_store_passes() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/stores/3/shifts");

        AppUser u = activeUser("user-sub", List.of("SALES_HISTORY"), List.of(3L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    // ── 13. Local incorrecto → 403 STORE_FORBIDDEN ────────────────────────────

    @Test
    void user_accessing_forbidden_store_gets_403() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/stores/7/shifts");

        AppUser u = activeUser("user-sub", List.of("SALES_HISTORY"), List.of(3L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isFalse();
        assertThat(res.getStatus()).isEqualTo(FORBIDDEN.value());
        assertThat(res.getContentAsString()).contains("STORE_FORBIDDEN");
    }

    // ── 14. Via-turno: storeId obtenido por JOIN ──────────────────────────────

    @Test
    void shift_resource_resolves_store_via_join() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/shifts/42/sales");

        AppUser u = activeUser("user-sub", List.of("SALES_HISTORY"), List.of(3L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));
        when(shiftRepository.findStoreIdByShiftId(42L)).thenReturn(Optional.of(3L));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    @Test
    void shift_resource_denies_wrong_store_via_join() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/shifts/42/sales");

        AppUser u = activeUser("user-sub", List.of("SALES_HISTORY"), List.of(3L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));
        when(shiftRepository.findStoreIdByShiftId(42L)).thenReturn(Optional.of(9L)); // otro local

        assertThat(interceptor.preHandle(req, res, null)).isFalse();
        assertThat(res.getContentAsString()).contains("STORE_FORBIDDEN");
    }

    // ── 15. Via-expense: storeId obtenido por JOIN ────────────────────────────

    @Test
    void expense_resource_resolves_store_via_join() throws Exception {
        bearer("user-sub", false);
        req.setMethod("PUT");
        req.setRequestURI("/api/v2/expenses/10");

        AppUser u = activeUser("user-sub", List.of("POS"), List.of(2L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));
        when(expenseRepository.findStoreIdByExpenseId(10L)).thenReturn(Optional.of(2L));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    // ── 16. POS: abrir turno en local correcto ────────────────────────────────

    @Test
    void pos_open_shift_allowed_store_passes() throws Exception {
        bearer("user-sub", false);
        req.setMethod("POST");
        req.setRequestURI("/api/v2/stores/1/shifts");

        AppUser u = activeUser("user-sub", List.of("POS"), List.of(1L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    // ── 17. INVENTORY: stock bloqueado sin INVENTORY ni POS ─────────────────

    @Test
    void inventory_stock_path_blocked_without_permission() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/stores/1/stock");

        AppUser u = activeUser("user-sub", List.of("SALES_HISTORY"), List.of(1L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isFalse();
        assertThat(res.getContentAsString()).contains("SECTION_FORBIDDEN");
    }

    // ── 17b. POS puede leer stock para mostrar productos en el POS ───────────

    @Test
    void pos_user_can_read_stock() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/stores/1/stock");

        AppUser u = activeUser("user-sub", List.of("POS"), List.of(1L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    // ── 17c. POS no puede ajustar stock (solo INVENTORY) ─────────────────────

    @Test
    void pos_user_cannot_adjust_stock() throws Exception {
        bearer("user-sub", false);
        req.setMethod("POST");
        req.setRequestURI("/api/v2/stores/1/stock/adjustment");

        AppUser u = activeUser("user-sub", List.of("POS"), List.of(1L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isFalse();
        assertThat(res.getContentAsString()).contains("SECTION_FORBIDDEN");
    }

    // ── 17d. POS puede ver ventas y egresos de su turno activo ───────────────

    @Test
    void pos_user_can_read_shift_sales() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/shifts/5/sales");

        AppUser u = activeUser("user-sub", List.of("POS"), List.of(1L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));
        when(shiftRepository.findStoreIdByShiftId(5L)).thenReturn(Optional.of(1L));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    @Test
    void pos_user_can_read_shift_expenses() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/shifts/5/expenses");

        AppUser u = activeUser("user-sub", List.of("POS"), List.of(1L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));
        when(shiftRepository.findStoreIdByShiftId(5L)).thenReturn(Optional.of(1L));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    // ── 17e. CATALOG puede leer stock (GET) ──────────────────────────────────

    @Test
    void catalog_user_can_read_stock() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/stores/1/stock");

        AppUser u = activeUser("user-sub", List.of("CATALOG"), List.of(1L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    @Test
    void catalog_user_can_read_stock_summary() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/v2/stores/1/stock/summary");

        AppUser u = activeUser("user-sub", List.of("CATALOG"), List.of(1L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isTrue();
    }

    @Test
    void catalog_user_cannot_adjust_stock() throws Exception {
        bearer("user-sub", false);
        req.setMethod("POST");
        req.setRequestURI("/api/v2/stores/1/stock/adjustment");

        AppUser u = activeUser("user-sub", List.of("CATALOG"), List.of(1L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isFalse();
        assertThat(res.getContentAsString()).contains("SECTION_FORBIDDEN");
    }

    // ── 18. TRANSACTIONS: query param storeId ────────────────────────────────

    @Test
    void transactions_query_param_store_forbidden() throws Exception {
        bearer("user-sub", false);
        req.setMethod("GET");
        req.setRequestURI("/api/transactions/date-range-store");
        req.setParameter("storeId", "5");

        AppUser u = activeUser("user-sub", List.of("TRANSACTIONS"), List.of(3L));
        when(userRepository.findByKeycloakId("user-sub")).thenReturn(Optional.of(u));

        assertThat(interceptor.preHandle(req, res, null)).isFalse();
        assertThat(res.getContentAsString()).contains("STORE_FORBIDDEN");
    }
}
