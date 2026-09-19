package balance.controller;

import balance.model.ClosingDeposit;
import balance.repository.ClosingDepositRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public List<Map<String, Object>> getRecentClosingDeposits() {
        var page = closingDepositRepository.findAll(PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by("id").descending()));
        return page.getContent().stream().map(cd -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", cd.getId());
            m.put("shiftId", cd.getShiftId());
            m.put("imageUri", cd.getImageUri());
            m.put("depositDate", cd.getDepositDate() != null ? cd.getDepositDate().toString() : null);
            m.put("amount", cd.getAmount());
            return m;
        }).collect(Collectors.toList());
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
        
        // Información real de la conexión
        try (Connection conn = dataSource.getConnection()) {
            info.put("conexionRealUrl", conn.getMetaData().getURL());
            info.put("conexionRealUsername", conn.getMetaData().getUserName());
            info.put("baseDatosActual", conn.getCatalog());
            info.put("nombreProductoBD", conn.getMetaData().getDatabaseProductName());
            info.put("versionProductoBD", conn.getMetaData().getDatabaseProductVersion());
            info.put("claseDataSource", dataSource.getClass().getName());
        } catch (SQLException e) {
            info.put("error", e.getMessage());
            info.put("stackTrace", Arrays.toString(e.getStackTrace()));
        }
        
        return info;
    }
}