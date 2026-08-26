package de.aivot.prosuna.backend.system.controllers;

import de.aivot.prosuna.backend.core.services.HttpService;
import de.aivot.prosuna.backend.security.CsrfResponseHeaderFilter;
import de.aivot.prosuna.backend.security.RedisCsrfTokenRepository;
import de.aivot.prosuna.backend.security.SecurityConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class,
                OAuth2ResourceServerWebSecurityAutoConfiguration.class
        }
)
@ContextConfiguration(classes = AuthController.class)
@Import({
        SecurityConfiguration.class,
        RedisCsrfTokenRepository.class,
        CsrfResponseHeaderFilter.class
})
@TestPropertySource(properties = {
        "prosuna.prosunaHostname=https://prosuna.example.com",
        "keycloak.hostname=https://auth.example.com",
        "keycloak.internalHostname=https://auth.example.com",
        "keycloak.realm=prosuna",
        "keycloak.frontendClientId=prosuna-client",
        "keycloak.frontendClientSecret=prosuna-secret"
})
class AuthControllerWebMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HttpService httpService;

    @MockitoBean
    private StringRedisTemplate redis;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void callbackErrorShouldRenderHtmlWithoutInternalForward() throws Exception {
        var result = mockMvc
                .perform(get("/api/auth/oidc-callback").param("code", "probe"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("auth/oidc-callback-error"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<title>Anmeldung fehlgeschlagen</title>")))
                .andExpect(content().string(containsString("Der state-Parameter ist ungültig.")))
                .andReturn();

        assertNull(result.getResponse().getForwardedUrl());
    }
}
