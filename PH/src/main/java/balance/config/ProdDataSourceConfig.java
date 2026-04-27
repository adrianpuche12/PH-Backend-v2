package balance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class ProdDataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(ProdDataSourceConfig.class);

    public ProdDataSourceConfig() {
        logger.info("=== PERFIL PROD activo — DataSource configurado via variables de entorno (NeonDB) ===");
    }
}