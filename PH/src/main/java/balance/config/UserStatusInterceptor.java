package balance.config;

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
import java.util.Map;

@Component
public class UserStatusInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(UserStatusInterceptor.class);

    @Autowired private AppUserRepository userRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return true;

        String keycloakId = extractSub(authHeader.substring(7));
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
