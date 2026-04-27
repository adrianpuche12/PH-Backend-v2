package balance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevDataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(DevDataSourceConfig.class);

    public DevDataSourceConfig() {
        logger.info("=== PERFIL DEV activo — DataSource configurado via variables de entorno (NeonDB) ===");
    }
}


