package balance.config;

import balance.users.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Base64;

@Component
public class UserStatusInterceptor implements HandlerInterceptor {

    @Autowired
    private AppUserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return true;

        String token = authHeader.substring(7);
        String keycloakId = extractClaim(token, "sub");
        if (keycloakId == null) return true;

        boolean suspended = userRepository.findByKeycloakId(keycloakId)
                .map(u -> "SUSPENDED".equals(u.getStatus()))
                .orElse(false);

        if (suspended) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"error\":\"ACCOUNT_SUSPENDED\"," +
                "\"message\":\"Tu cuenta fue suspendida. Contacta al encargado.\"}"
            );
            return false;
        }

        return true;
    }

    private String extractClaim(String token, String claimName) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            byte[] decoded = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            String payload = new String(decoded);
            String key = "\"" + claimName + "\":\"";
            int start = payload.indexOf(key);
            if (start == -1) return null;
            start += key.length();
            int end = payload.indexOf('"', start);
            return end > start ? payload.substring(start, end) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String padBase64(String s) {
        return switch (s.length() % 4) {
            case 2 -> s + "==";
            case 3 -> s + "=";
            default -> s;
        };
    }
}
