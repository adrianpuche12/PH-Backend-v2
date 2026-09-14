package balance.config;

import balance.users.model.AppUser;
import balance.users.repository.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
    private static final Pattern STORE_ID_PATTERN = Pattern.compile("/stores?/(\\d+)");

    @Autowired(required = false)
    private AppUserRepository userRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return true;

        if (userRepository == null) return true;

        String keycloakId = extractSub(authHeader.substring(7));
        if (keycloakId == null) return true;

        Optional<AppUser> userOpt;
        try {
            userOpt = userRepository.findByKeycloakId(keycloakId);
        } catch (Exception e) {
            log.error("Error al verificar permisos de usuario keycloakId={}: {}", keycloakId, e.getMessage());
            return true;  // fail-open: si la DB falla, no bloqueamos todo el sistema
        }
        // Sin registro en DB → admin u otro rol sin restricciones → pasar
        if (userOpt.isEmpty()) return true;

        AppUser user = userOpt.get();

        // 1. Verificar suspensión
        if ("SUSPENDED".equals(user.getStatus())) {
            return deny(response, "ACCOUNT_SUSPENDED", "Tu cuenta fue suspendida. Contacta al encargado.");
        }

        String uri = request.getRequestURI();
        List<String> userPermissions = user.getPermissions();

        String required = resolveRequiredPermission(uri);

        // 2. Verificar permiso de sección (solo si el usuario tiene restricciones)
        if (!userPermissions.isEmpty() && required != null && !userPermissions.contains(required)) {
            return deny(response, "SECTION_FORBIDDEN", "No tienes acceso a esta sección.");
        }

        // 3. Verificar acceso al local — solo aplica cuando hay sección en juego
        // (endpoints de metadatos como GET /stores/{id} quedan siempre accesibles)
        List<Long> accessibleStoreIds = user.getAccessibleStores().stream()
                .map(s -> s.getId())
                .toList();
        if (!accessibleStoreIds.isEmpty() && required != null) {
            Long requestedStore = extractStoreId(request, uri);
            if (requestedStore != null && !accessibleStoreIds.contains(requestedStore)) {
                return deny(response, "STORE_FORBIDDEN", "No tienes acceso a este local.");
            }
        }

        return true;
    }

    /**
     * Devuelve el permiso requerido para acceder a la URI, o null si no hay restricción.
     * Lógica vacía = acceso total (usuarios legacy sin permisos configurados).
     */
    private String resolveRequiredPermission(String uri) {
        // POS: turnos, ventas y cierre de turno
        if (uri.startsWith("/api/v2/shifts") ||
            uri.startsWith("/api/v2/sales") ||
            uri.matches(".*/stores/\\d+/shifts.*") ||
            uri.matches(".*/stores/\\d+/sales.*") ||
            uri.startsWith("/api/forms/closing-deposits")) {
            return "POS";
        }
        // INVENTORY: stock de locales
        if (uri.matches(".*/stores/\\d+/stock.*")) {
            return "INVENTORY";
        }
        // DASHBOARD
        if (uri.startsWith("/api/v2/dashboard")) {
            return "DASHBOARD";
        }
        // TRANSACTIONS: depósitos bancarios, operaciones, balance legacy
        if (uri.startsWith("/api/transactions") ||
            uri.startsWith("/transactions") ||
            uri.startsWith("/api/v2/deposits") ||
            uri.startsWith("/api/operations")) {
            return "TRANSACTIONS";
        }
        // SALARY_PAYMENTS
        if (uri.startsWith("/api/salary-payments") ||
            uri.startsWith("/api/forms/salary-payments")) {
            return "SALARY_PAYMENTS";
        }
        // SUPPLIER_PAYMENTS
        if (uri.startsWith("/api/supplier-payments") ||
            uri.startsWith("/api/forms/supplier-payments")) {
            return "SUPPLIER_PAYMENTS";
        }
        // CATALOG: productos y categorías
        if (uri.startsWith("/api/v2/products") ||
            uri.startsWith("/api/v2/categories") ||
            uri.matches(".*/stores/\\d+/products.*") ||
            uri.matches(".*/stores/\\d+/categories.*")) {
            return "CATALOG";
        }
        return null;
    }

    private Long extractStoreId(HttpServletRequest request, String uri) {
        Matcher m = STORE_ID_PATTERN.matcher(uri);
        if (m.find()) {
            try { return Long.parseLong(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        String param = request.getParameter("storeId");
        if (param != null && !param.isBlank()) {
            try { return Long.parseLong(param); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private boolean deny(HttpServletResponse response, String error, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            "{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}"
        );
        return false;
    }

    @SuppressWarnings("unchecked")
    private String extractSub(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String padded = parts[1];
            int mod = padded.length() % 4;
            if (mod == 2) padded += "==";
            else if (mod == 3) padded += "=";
            byte[] decoded = Base64.getUrlDecoder().decode(padded);
            Map<String, Object> claims = mapper.readValue(decoded, Map.class);
            Object sub = claims.get("sub");
            return sub != null ? sub.toString() : null;
        } catch (Exception e) {
            log.debug("No se pudo parsear JWT: {}", e.getMessage());
            return null;
        }
    }
}
