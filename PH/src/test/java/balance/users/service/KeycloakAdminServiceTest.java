package balance.users.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakAdminServiceTest {

    private KeycloakAdminService service;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        service = new KeycloakAdminService();
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "keycloakUrl",   "http://kc-test:8080");
        ReflectionTestUtils.setField(service, "realm",          "test-realm");
        ReflectionTestUtils.setField(service, "adminUsername",  "admin");
        ReflectionTestUtils.setField(service, "adminPassword",  "secret");
        ReflectionTestUtils.setField(service, "restTemplate",   restTemplate);
    }

    // ── Token caching ─────────────────────────────────────────────────────────

    @Test
    void getAdminToken_fetchesNewTokenWhenCacheExpired() {
        mockTokenResponse("token-abc");

        // Llamar dos métodos que usen el token
        mockSetUserEnabled("user-1", true);
        service.setUserEnabled("user-1", true);
        service.setUserEnabled("user-1", false);

        // El token debería haberse obtenido solo una vez (cache)
        verify(restTemplate, atMostOnce()).postForEntity(
            contains("/realms/master/protocol/openid-connect/token"),
            any(), eq(Map.class));
    }

    // ── setUserEnabled ────────────────────────────────────────────────────────

    @Test
    void setUserEnabled_sendsCorrectPutRequest() {
        mockTokenResponse("token-xyz");
        mockSetUserEnabled("kc-123", true);

        service.setUserEnabled("kc-123", true);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
            eq("http://kc-test:8080/admin/realms/test-realm/users/kc-123"),
            eq(HttpMethod.PUT),
            captor.capture(),
            eq(Void.class));

        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertThat(body).containsEntry("enabled", true);
    }

    @Test
    void setUserEnabled_disablesUserCorrectly() {
        mockTokenResponse("token-xyz");
        mockSetUserEnabled("kc-456", false);

        service.setUserEnabled("kc-456", false);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
            contains("kc-456"), eq(HttpMethod.PUT), captor.capture(), eq(Void.class));

        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertThat(body).containsEntry("enabled", false);
    }

    // ── logoutUser ────────────────────────────────────────────────────────────

    @Test
    void logoutUser_sendsPostToLogoutEndpoint() {
        mockTokenResponse("token-xyz");
        when(restTemplate.exchange(
            contains("/logout"), eq(HttpMethod.POST), any(), eq(Void.class)))
            .thenReturn(ResponseEntity.ok().build());

        service.logoutUser("kc-123");

        verify(restTemplate).exchange(
            eq("http://kc-test:8080/admin/realms/test-realm/users/kc-123/logout"),
            eq(HttpMethod.POST), any(), eq(Void.class));
    }

    @Test
    void logoutUser_ignores404WhenNoActiveSessions() {
        mockTokenResponse("token-xyz");
        when(restTemplate.exchange(contains("/logout"), eq(HttpMethod.POST), any(), eq(Void.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // No debe lanzar excepción
        assertThatCode(() -> service.logoutUser("kc-123")).doesNotThrowAnyException();
    }

    @Test
    void logoutUser_throwsOnNon404Error() {
        mockTokenResponse("token-xyz");
        when(restTemplate.exchange(contains("/logout"), eq(HttpMethod.POST), any(), eq(Void.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> service.logoutUser("kc-123"))
            .isInstanceOf(RuntimeException.class);
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    void deleteUser_sendsDeleteRequest() {
        mockTokenResponse("token-xyz");
        when(restTemplate.exchange(contains("/users/kc-123"), eq(HttpMethod.DELETE), any(), eq(Void.class)))
            .thenReturn(ResponseEntity.noContent().build());

        service.deleteUser("kc-123");

        verify(restTemplate).exchange(
            eq("http://kc-test:8080/admin/realms/test-realm/users/kc-123"),
            eq(HttpMethod.DELETE), any(), eq(Void.class));
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Test
    void resetPassword_sendsPasswordPayload() {
        mockTokenResponse("token-xyz");
        when(restTemplate.exchange(contains("/reset-password"), eq(HttpMethod.PUT), any(), eq(Void.class)))
            .thenReturn(ResponseEntity.noContent().build());

        service.resetPassword("kc-123", "newPass123!");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
            contains("/reset-password"), eq(HttpMethod.PUT), captor.capture(), eq(Void.class));

        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertThat(body)
            .containsEntry("value", "newPass123!")
            .containsEntry("temporary", false)
            .containsEntry("type", "password");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void mockTokenResponse(String token) {
        ResponseEntity<Map> tokenResp = ResponseEntity.ok(Map.of("access_token", token));
        when(restTemplate.postForEntity(
            contains("/realms/master/protocol/openid-connect/token"),
            any(), eq(Map.class)))
            .thenReturn(tokenResp);
    }

    private void mockSetUserEnabled(String userId, boolean enabled) {
        when(restTemplate.exchange(
            contains("/users/" + userId),
            eq(HttpMethod.PUT), any(), eq(Void.class)))
            .thenReturn(ResponseEntity.noContent().build());
    }
}
