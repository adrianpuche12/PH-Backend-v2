package balance.config;

import balance.sales.repository.ShiftExpenseRepository;
import balance.sales.repository.ShiftRepository;
import balance.users.model.AppUser;
import balance.users.repository.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UserStatusInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(UserStatusInterceptor.class);
    private static final AntPathMatcher ANT = new AntPathMatcher();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Paths que non-admins necesitan en login/primer login — se verifican antes que todo
    private static final List<String> ALWAYS_ALLOWED = List.of(
        "/api/v2/users/by-username/**",
        "/api/v2/users/change-password",
        "/api/v2/users/accessible-stores"
    );

    // Paths admin-only que se bloquean solo cuando ninguna sección los reclama
    private static final List<String> ADMIN_ONLY = List.of(
        "/api/v2/users/**",
        "/api/v2/stores/**",
        "/api/stores/**",
        "/api/v2/uploads/**"
    );

    // Reglas path → secciones válidas (OR); el primer match gana.
    private record SectionRule(String method, String pattern, List<String> allowedSections) {
        boolean matches(String reqMethod, String path) {
            boolean methodOk = "*".equals(method) || method.equalsIgnoreCase(reqMethod);
            return methodOk && ANT.match(pattern, path);
        }
    }

    private static final List<SectionRule> SECTION_RULES = List.of(
        // ── POS — Ventas ────────────────────────────────────────────────────
        new SectionRule("POST",   "/api/v2/stores/*/shifts",         List.of("POS")),
        new SectionRule("*",      "/api/v2/shifts/active/*",         List.of("POS")),
        new SectionRule("*",      "/api/v2/shifts/*/close",          List.of("POS")),
        new SectionRule("*",      "/api/v2/shifts/*/closing",        List.of("POS")),
        new SectionRule("POST",   "/api/v2/shifts/*/sales",          List.of("POS")),
        new SectionRule("POST",   "/api/v2/shifts/*/expenses",       List.of("POS")),
        new SectionRule("*",      "/api/v2/shifts/*/summary",        List.of("POS")),
        new SectionRule("PUT",    "/api/v2/expenses/*",              List.of("POS")),
        new SectionRule("DELETE", "/api/v2/expenses/*",              List.of("POS")),

        // ── SALES_HISTORY — Historial de ventas ─────────────────────────────
        new SectionRule("GET",    "/api/v2/stores/*/shifts",         List.of("SALES_HISTORY")),
        new SectionRule("GET",    "/api/v2/stores/*/sales/summary",  List.of("SALES_HISTORY")),
        new SectionRule("GET",    "/api/v2/stores/*/sales",          List.of("SALES_HISTORY")),
        new SectionRule("GET",    "/api/v2/sales/cash-summary",      List.of("SALES_HISTORY")),
        new SectionRule("GET",    "/api/v2/sales/*",                 List.of("SALES_HISTORY")),
        // POS también puede ver las ventas y egresos de su turno activo
        new SectionRule("GET",    "/api/v2/shifts/*/sales",          List.of("SALES_HISTORY", "POS")),
        new SectionRule("GET",    "/api/v2/shifts/*/expenses",       List.of("SALES_HISTORY", "POS")),
        new SectionRule("GET",    "/api/v2/shifts/*",                List.of("SALES_HISTORY")),
        new SectionRule("GET",    "/api/v2/shifts",                  List.of("SALES_HISTORY")),

        // ── INVENTORY — Inventario ───────────────────────────────────────────
        // POS y CATALOG también pueden leer el stock (GET); solo INVENTORY puede escribir
        new SectionRule("GET",    "/api/v2/stores/*/stock",          List.of("INVENTORY", "POS", "CATALOG")),
        new SectionRule("GET",    "/api/v2/stores/*/stock/**",       List.of("INVENTORY", "CATALOG")),
        new SectionRule("*",      "/api/v2/stores/*/stock/**",       List.of("INVENTORY")),
        new SectionRule("*",      "/api/v2/stores/*/stock",          List.of("INVENTORY")),

        // ── TRANSACTIONS — Operaciones ───────────────────────────────────────
        new SectionRule("*",      "/api/transactions/store/*",            List.of("TRANSACTIONS")),
        new SectionRule("*",      "/api/transactions/date-range-store",   List.of("TRANSACTIONS")),
        new SectionRule("*",      "/api/operations/**",                   List.of("TRANSACTIONS")),

        // ── BANK_DEPOSITS — Depósitos bancarios ─────────────────────────────
        new SectionRule("*",      "/api/v2/deposits/**",                  List.of("BANK_DEPOSITS")),
        new SectionRule("*",      "/api/v2/deposits",                     List.of("BANK_DEPOSITS")),
        new SectionRule("*",      "/api/forms/closing-deposits/**",       List.of("BANK_DEPOSITS")),
        new SectionRule("*",      "/api/forms/closing-deposits",          List.of("BANK_DEPOSITS")),

        // ── SALARY_PAYMENTS ─────────────────────────────────────────────────
        new SectionRule("*",      "/api/salary-payments/store/*",         List.of("SALARY_PAYMENTS")),
        new SectionRule("*",      "/api/forms/salary-payments",           List.of("SALARY_PAYMENTS")),

        // ── SUPPLIER_PAYMENTS ────────────────────────────────────────────────
        new SectionRule("*",      "/api/supplier-payments/store/*",       List.of("SUPPLIER_PAYMENTS")),
        new SectionRule("*",      "/api/forms/supplier-payments",         List.of("SUPPLIER_PAYMENTS")),

        // ── CATALOG — Catálogo ───────────────────────────────────────────────
        new SectionRule("*",      "/api/v2/stores/*/products",            List.of("CATALOG")),
        new SectionRule("*",      "/api/v2/products/**",                  List.of("CATALOG")),
        new SectionRule("*",      "/api/v2/stores/*/categories",          List.of("CATALOG")),
        new SectionRule("*",      "/api/v2/categories/**",                List.of("CATALOG")),

        // ── FORMS — Formularios ──────────────────────────────────────────────
        new SectionRule("*",      "/api/forms/gasto-admin/**",            List.of("FORMS")),
        new SectionRule("*",      "/api/forms/gasto-admin",               List.of("FORMS")),

        // ── DASHBOARD ────────────────────────────────────────────────────────
        new SectionRule("*",      "/api/v2/dashboard/**",                 List.of("DASHBOARD")),
        new SectionRule("*",      "/api/v2/dashboard",                    List.of("DASHBOARD"))
    );

    // Patrón para endpoints cuyo storeId se obtiene JOIN via turno
    private static final Pattern SHIFT_RESOURCE   = Pattern.compile("^/api/v2/shifts/(\\d+)(/|$)");
    // Patrón para endpoints cuyo storeId se obtiene JOIN via gasto de turno
    private static final Pattern EXPENSE_RESOURCE = Pattern.compile("^/api/v2/expenses/(\\d+)$");
    // Patrones de storeId directo en path
    private static final List<Pattern> STORE_IN_PATH = List.of(
        Pattern.compile("^/api/v2/stores/(\\d+)/"),
        Pattern.compile("^/api/transactions/store/(\\d+)"),
        Pattern.compile("^/api/salary-payments/store/(\\d+)"),
        Pattern.compile("^/api/supplier-payments/store/(\\d+)"),
        Pattern.compile("^/api/v2/shifts/active/(\\d+)")
    );

    @Autowired(required = false) private AppUserRepository userRepository;
    @Autowired(required = false) private ShiftRepository shiftRepository;
    @Autowired(required = false) private ShiftExpenseRepository expenseRepository;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return true;

        if (userRepository == null) return true;

        String token = authHeader.substring(7);
        TokenInfo info = extractTokenInfo(token);
        if (info == null) return true;

        // ── Check 1: status de cuenta ─────────────────────────────────────
        Optional<AppUser> userOpt = userRepository.findByKeycloakId(info.sub());
        if (userOpt.isEmpty()) return true;

        AppUser user = userOpt.get();
        if ("SUSPENDED".equals(user.getStatus())) {
            return deny(response, "ACCOUNT_SUSPENDED",
                "Tu cuenta fue suspendida. Contacta al encargado.");
        }

        // Los admins saltan todos los checks
        if (info.isAdmin()) return true;

        String path   = request.getRequestURI();
        String method = request.getMethod();

        // Whitelist — login / primer login de cualquier usuario
        for (String pattern : ALWAYS_ALLOWED) {
            if (ANT.match(pattern, path)) return true;
        }

        // ── Check 2: sección ──────────────────────────────────────────────
        List<String> allowedSections = resolveAllowedSections(method, path);

        if (allowedSections != null) {
            List<String> permissions = user.getPermissions();
            if (!permissions.isEmpty() && allowedSections.stream().noneMatch(permissions::contains)) {
                return deny(response, "SECTION_FORBIDDEN",
                    "No tenés permiso para esta sección.");
            }

            // ── Check 3: local ────────────────────────────────────────────
            List<Long> storeIds = user.getAccessibleStores().stream()
                .map(s -> s.getId())
                .toList();

            if (!storeIds.isEmpty()) {
                Long storeId = resolveStoreId(request, path);
                if (storeId != null && !storeIds.contains(storeId)) {
                    return deny(response, "STORE_FORBIDDEN",
                        "No tenés acceso a los datos de este local.");
                }
            }

            return true;
        }

        // Sin sección — verificar si es admin-only
        for (String pattern : ADMIN_ONLY) {
            if (ANT.match(pattern, path)) {
                return deny(response, "FORBIDDEN",
                    "Acceso restringido a administradores.");
            }
        }

        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> resolveAllowedSections(String method, String path) {
        for (SectionRule rule : SECTION_RULES) {
            if (rule.matches(method, path)) return rule.allowedSections();
        }
        return null;
    }

    private Long resolveStoreId(HttpServletRequest request, String path) {
        // Via turno: /api/v2/shifts/{shiftId}/...
        Matcher shiftM = SHIFT_RESOURCE.matcher(path);
        if (shiftM.find() && shiftRepository != null) {
            return shiftRepository.findStoreIdByShiftId(Long.parseLong(shiftM.group(1)))
                .orElse(null);
        }

        // Via gasto de turno: /api/v2/expenses/{expenseId}
        Matcher expM = EXPENSE_RESOURCE.matcher(path);
        if (expM.find() && expenseRepository != null) {
            return expenseRepository.findStoreIdByExpenseId(Long.parseLong(expM.group(1)))
                .orElse(null);
        }

        // storeId directo en path
        for (Pattern p : STORE_IN_PATH) {
            Matcher m = p.matcher(path);
            if (m.find()) return Long.parseLong(m.group(1));
        }

        // storeId como query param
        String param = request.getParameter("storeId");
        if (param != null) {
            try { return Long.parseLong(param); } catch (NumberFormatException ignored) {}
        }

        return null;
    }

    private boolean deny(HttpServletResponse response, String error, String message)
            throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            String.format("{\"error\":\"%s\",\"message\":\"%s\"}", error, message)
        );
        return false;
    }

    @SuppressWarnings("unchecked")
    private TokenInfo extractTokenInfo(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payload = parts[1];
            int mod = payload.length() % 4;
            if (mod == 2) payload += "==";
            else if (mod == 3) payload += "=";
            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            Map<String, Object> claims = MAPPER.readValue(decoded, Map.class);

            Object sub = claims.get("sub");
            if (sub == null) return null;

            boolean isAdmin = false;
            Object realmAccess = claims.get("realm_access");
            if (realmAccess instanceof Map<?, ?> ra) {
                Object roles = ra.get("roles");
                if (roles instanceof List<?> roleList) {
                    isAdmin = roleList.contains("admin");
                }
            }
            return new TokenInfo(sub.toString(), isAdmin);
        } catch (Exception e) {
            log.debug("No se pudo parsear JWT: {}", e.getMessage());
            return null;
        }
    }

    private record TokenInfo(String sub, boolean isAdmin) {}
}
