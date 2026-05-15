package balance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = BalanceApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class BalanceApplicationTests {

    // Evita que Spring intente conectarse a Keycloak para obtener JWKS al arrancar
    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
    }

}