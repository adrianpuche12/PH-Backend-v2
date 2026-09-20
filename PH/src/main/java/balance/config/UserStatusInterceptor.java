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

        List<String> required = resolveRequiredPermissions(uri);

        // 2. Verificar permiso de sección (solo si el usuario tiene restricciones)
        // El usuario pasa si tiene AL MENOS UNO de los permisos aceptados para esa ruta.
        if (!userPermissions.isEmpty() && !required.isEmpty() &&
                required.stream().noneMatch(userPermissions::contains)) {
            return deny(response, "SECTION_FORBIDDEN", "No tienes acceso a esta sección.");
        }

        // 3. Verificar acceso al local — solo aplica cuando hay sección en juego
        // (endpoints de metadatos como GET /stores/{id} quedan siempre accesibles)
        List<Long> accessibleStoreIds = user.getAccessibleStores().stream()
                .map(s -> s.getId())
                .toList();
        if (!accessibleStoreIds.isEmpty() && !required.isEmpty()) {
            Long requestedStore = extractStoreId(request, uri);
            if (requestedStore != null && !accessibleStoreIds.contains(requestedStore)) {
                return deny(response, "STORE_FORBIDDEN", "No tienes acceso a este local.");
            }
        }

        return true;
    }

    /**
     * Devuelve los permisos aceptados para acceder a la URI (cualquiera alcanza).
     * Lista vacía = sin restricción (usuarios legacy sin permisos configurados).
     *
     * Permisos definidos:
     *   POS            – crear/cerrar turnos, registrar ventas
     *   SALES_HISTORY  – ver historial de turnos y ventas (lectura)
     *   INVENTORY      – stock de locales
     *   DASHBOARD      – panel de métricas
     *   TRANSACTIONS   – depósitos, operaciones, balance
     *   SALARY_PAYMENTS / SUPPLIER_PAYMENTS / CATALOG
     */
    private List<String> resolveRequiredPermissions(String uri) {
        // Historial de turnos/ventas: accesible con POS O SALES_HISTORY
        if (uri.matches(".*/stores/\\d+/shifts.*") ||
            uri.matches(".*/stores/\\d+/sales.*")) {
            return List.of("POS", "SALES_HISTORY");
        }
        // POS: crear/cerrar turno y registrar venta (rutas genéricas sin storeId)
        if (uri.startsWith("/api/v2/shifts") ||
            uri.startsWith("/api/v2/sales") ||
            uri.startsWith("/api/forms/closing-deposits")) {
            return List.of("POS");
        }
        // INVENTORY
        if (uri.matches(".*/stores/\\d+/stock.*")) {
            return List.of("INVENTORY");
        }
        // DASHBOARD
        if (uri.startsWith("/api/v2/dashboard")) {
            return List.of("DASHBOARD");
        }
        // TRANSACTIONS
        if (uri.startsWith("/api/transactions") ||
            uri.startsWith("/transactions") ||
            uri.startsWith("/api/v2/deposits") ||
            uri.startsWith("/api/operations")) {
            return List.of("TRANSACTIONS");
        }
        // SALARY_PAYMENTS
        if (uri.startsWith("/api/salary-payments") ||
            uri.startsWith("/api/forms/salary-payments")) {
            return List.of("SALARY_PAYMENTS");
        }
        // SUPPLIER_PAYMENTS
        if (uri.startsWith("/api/supplier-payments") ||
            uri.startsWith("/api/forms/supplier-payments")) {
            return List.of("SUPPLIER_PAYMENTS");
        }
        // CATALOG
        if (uri.startsWith("/api/v2/products") ||
            uri.startsWith("/api/v2/categories") ||
            uri.matches(".*/stores/\\d+/products.*") ||
            uri.matches(".*/stores/\\d+/categories.*")) {
            return List.of("CATALOG");
        }
        return List.of();
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
