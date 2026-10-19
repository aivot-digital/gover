package de.aivot.prosuna.backend.security;

import de.aivot.prosuna.backend.system.controllers.AuthController;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SecurityConfigurationCsrfWebMvcTest.TestController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class,
                OAuth2ResourceServerWebSecurityAutoConfiguration.class
        }
)
@ContextConfiguration(classes = SecurityConfigurationCsrfWebMvcTest.TestController.class)
@Import({
        SecurityConfiguration.class,
        RedisCsrfTokenRepository.class,
        CsrfResponseHeaderFilter.class
})
class SecurityConfigurationCsrfWebMvcTest {
    private static final String ACCESS_TOKEN = "staff-access-token";
    private static final String CSRF_ID = "csrf-id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StringRedisTemplate redis;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("csrf:token:" + CSRF_ID)).thenReturn("stored-csrf-token");
        when(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(Jwt
                .withTokenValue(ACCESS_TOKEN)
                .header("alg", "none")
                .subject("staff-user")
                .claim("realm_access", Map.of("roles", List.of("user")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build());
    }

    @Test
    void rejectsStateChangingRequestsWithAccessCookieWithoutCsrfToken() throws Exception {
        mockMvc
                .perform(post("/api/csrf-test/")
                        .cookie(accessCookie(), csrfCookie()))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptsStateChangingRequestsWithAccessCookieAndCsrfToken() throws Exception {
        mockMvc
                .perform(post("/api/csrf-test/")
                        .cookie(accessCookie(), csrfCookie())
                        .header(RedisCsrfTokenRepository.HEADER_NAME, createMaskedCsrfToken()))
                .andExpect(status().isOk());
    }

    @Test
    void acceptsSafeRequestsWithAccessCookieWithoutCsrfToken() throws Exception {
        mockMvc
                .perform(get("/api/csrf-test/")
                        .cookie(accessCookie(), csrfCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsStateChangingRequestsWithAccessCookieAndWrongCsrfToken() throws Exception {
        mockMvc
                .perform(post("/api/csrf-test/")
                        .cookie(accessCookie(), csrfCookie())
                        .header(RedisCsrfTokenRepository.HEADER_NAME, "wrong-csrf-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void keepsPublicEndpointsExcludedFromCsrfProtection() throws Exception {
        mockMvc
                .perform(post("/api/public/csrf-test/")
                        .cookie(accessCookie()))
                .andExpect(status().isOk());
    }

    /**
     * Masks the stored token like the CSRF token that the backend hands out in the response header, which the frontend
     * sends back unchanged.
     */
    private static String createMaskedCsrfToken() {
        var request = new MockHttpServletRequest();
        new XorCsrfTokenRequestAttributeHandler().handle(
                request,
                new MockHttpServletResponse(),
                () -> new DefaultCsrfToken(
                        RedisCsrfTokenRepository.HEADER_NAME,
                        RedisCsrfTokenRepository.PARAMETER_NAME,
                        "stored-csrf-token"
                )
        );
        return ((CsrfToken) request.getAttribute(CsrfToken.class.getName())).getToken();
    }

    private static Cookie accessCookie() {
        return new Cookie(AuthController.ACCESS_COOKIE_NAME, ACCESS_TOKEN);
    }

    private static Cookie csrfCookie() {
        return new Cookie(RedisCsrfTokenRepository.COOKIE_NAME, CSRF_ID);
    }

    @RestController
    static class TestController {
        @GetMapping("/api/csrf-test/")
        public String read() {
            return "read";
        }

        @PostMapping("/api/csrf-test/")
        public String change() {
            return "changed";
        }

        @PostMapping("/api/public/csrf-test/")
        public String changePublic() {
            return "changed";
        }
    }
}
