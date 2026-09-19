package balance.controller;

import balance.repository.ClosingDepositRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    @Autowired
    private ClosingDepositRepository closingDepositRepository;

    /**
     * Endpoint para obtener información detallada sobre la conexión a la base de datos.
     * Accede a esta información en: http://[tu-host]:[tu-puerto]/debug/datasource
     */
    @GetMapping("/closing-deposits")
    public Object getRecentClosingDeposits() {
        try {
            var all = closingDepositRepository.findAll();
            var result = new ArrayList<Map<String, Object>>();
            for (var cd : all) {
                var m = new HashMap<String, Object>();
                m.put("id", cd.getId());
                m.put("shiftId", cd.getShiftId());
                m.put("imageUri", cd.getImageUri());
                m.put("depositDate", cd.getDepositDate() != null ? cd.getDepositDate().toString() : null);
                m.put("amount", cd.getAmount());
                result.add(m);
            }
            result.sort((a, b) -> Long.compare((Long) b.get("id"), (Long) a.get("id")));
            return result.subList(0, Math.min(10, result.size()));
        } catch (Exception e) {
            var err = new HashMap<String, String>();
            err.put("jpaError", e.getClass().getName());
            err.put("message", e.getMessage() != null ? e.getMessage() : "(null)");
            return err;
        }
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
            try {
                PreparedStatement cdStmt = conn.prepareStatement(
                    "SELECT id, shift_id, image_uri, deposit_date FROM closing_deposits ORDER BY id DESC LIMIT 5");
                ResultSet cdRs = cdStmt.executeQuery();
                StringBuilder sb = new StringBuilder();
                while (cdRs.next()) {
                    sb.append("id=").append(cdRs.getLong("id"))
                      .append(" shiftId=").append(cdRs.getObject("shift_id"))
                      .append(" imageUri=").append(cdRs.getString("image_uri"))
                      .append(" date=").append(cdRs.getString("deposit_date"))
                      .append(" | ");
                }
                cdRs.close();
                cdStmt.close();
                info.put("closingDeposits", sb.length() > 0 ? sb.toString() : "EMPTY");
            } catch (Exception cdEx) {
                info.put("closingDepositsError", cdEx.getClass().getSimpleName() + ": " + cdEx.getMessage());
            }
        } catch (SQLException e) {
            info.put("error", e.getMessage());
            info.put("stackTrace", Arrays.toString(e.getStackTrace()));
        }

        return info;
    }
}