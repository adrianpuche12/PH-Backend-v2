package balance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador de diagnóstico para verificar la configuración de la base de datos.
 * Este controlador es temporal y debería eliminarse en producción.
 */
@RestController
@RequestMapping("/debug")
public class DebugController {
    
    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    /**
     * Endpoint para obtener información detallada sobre la conexión a la base de datos.
     * Accede a esta información en: http://[tu-host]:[tu-puerto]/debug/datasource
     */
    @GetMapping("/closing-deposits")
    public Object getRecentClosingDeposits() {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT id, shift_id, image_uri, deposit_date, amount FROM closing_deposits ORDER BY id DESC LIMIT 10";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
                 java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("shiftId", rs.getObject("shift_id"));
                    m.put("imageUri", rs.getString("image_uri"));
                    m.put("depositDate", rs.getString("deposit_date"));
                    m.put("amount", rs.getBigDecimal("amount"));
                    result.add(m);
                }
            }
        } catch (Exception e) {
            return Map.of("sqlError", e.getClass().getName() + ": " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/version")
    public Map<String, String> getVersion() {
        return Map.of(
            "version", "saveRecipe-nativeSQL-v1",
            "build", "2026-07-28"
        );
    }

    @GetMapping("/datasource")
    public Map<String, String> getDataSourceInfo() {
        Map<String, String> info = new HashMap<>();
        
        // Información del entorno
        info.put("perfilActivo", String.join(", ", Arrays.asList(environment.getActiveProfiles())));
        info.put("jdbcUrl", environment.getProperty("spring.datasource.url"));
        info.put("username", environment.getProperty("spring.datasource.username"));
        
        // Información real de la conexión + query de diagnóstico closing_deposits
        try (Connection conn = dataSource.getConnection()) {
            info.put("conexionRealUrl", conn.getMetaData().getURL());
            info.put("conexionRealUsername", conn.getMetaData().getUserName());
            info.put("baseDatosActual", conn.getCatalog());
            info.put("nombreProductoBD", conn.getMetaData().getDatabaseProductName());
            info.put("versionProductoBD", conn.getMetaData().getDatabaseProductVersion());
            info.put("claseDataSource", dataSource.getClass().getName());

            // Diagnóstico: últimos 5 cierres con imageUri
            try (var stmt = conn.prepareStatement(
                    "SELECT id, shift_id, image_uri, deposit_date FROM closing_deposits ORDER BY id DESC LIMIT 5");
                 var rs = stmt.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    sb.append("id=").append(rs.getLong("id"))
                      .append(" shiftId=").append(rs.getObject("shift_id"))
                      .append(" imageUri=").append(rs.getString("image_uri"))
                      .append(" date=").append(rs.getString("deposit_date"))
                      .append(" | ");
                }
                info.put("closingDeposits", sb.length() > 0 ? sb.toString() : "EMPTY");
            }
        } catch (SQLException e) {
            info.put("error", e.getMessage());
            info.put("stackTrace", Arrays.toString(e.getStackTrace()));
        }

        return info;
    }
}