package de.aivot.prosuna.backend.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.AvailabilityProbesAutoConfiguration;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = KubernetesProbesWebMvcTest.ProbeConfiguration.class)
@ImportAutoConfiguration(classes = {
        ApplicationAvailabilityAutoConfiguration.class,
        AvailabilityProbesAutoConfiguration.class,
        HealthContributorRegistryAutoConfiguration.class,
        HealthEndpointAutoConfiguration.class,
        EndpointAutoConfiguration.class,
        WebEndpointAutoConfiguration.class,
        ManagementContextAutoConfiguration.class
}, exclude = {
        OAuth2ResourceServerAutoConfiguration.class,
        OAuth2ResourceServerWebSecurityAutoConfiguration.class
})
class KubernetesProbesWebMvcTest {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @MockitoBean
    private StringRedisTemplate redis;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        // Register the chain so the CSRF response filter runs at its configured security-filter position.
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
        AvailabilityChangeEvent.publish(applicationContext, LivenessState.CORRECT);
        AvailabilityChangeEvent.publish(applicationContext, ReadinessState.ACCEPTING_TRAFFIC);
    }

    @ParameterizedTest
    @ValueSource(strings = {"liveness", "readiness"})
    void anonymousProbeShouldReturnOnlyStatusWithoutAccessingRedis(String probe) throws Exception {
        mockMvc.perform(get("/api/actuator/health/" + probe))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", JsonCompareMode.STRICT))
                .andExpect(header().doesNotExist(RedisCsrfTokenRepository.HEADER_NAME))
                .andExpect(cookie().doesNotExist(RedisCsrfTokenRepository.COOKIE_NAME));

        verifyNoInteractions(redis);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/actuator/health",
            "/api/actuator/health/liveness",
            "/api/actuator/health/readiness"
    })
    void anonymousHeadHealthCheckShouldSucceedWithoutAccessingRedis(String path) throws Exception {
        // The app container waits for the API with a HEAD request to the health endpoint.
        mockMvc.perform(head(path))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(RedisCsrfTokenRepository.HEADER_NAME))
                .andExpect(cookie().doesNotExist(RedisCsrfTokenRepository.COOKIE_NAME));

        verifyNoInteractions(redis);
    }

    @ParameterizedTest
    @ValueSource(strings = {"liveness", "readiness"})
    void probeWithContextPathAndCsrfCookieShouldNotAccessRedis(String probe) throws Exception {
        mockMvc.perform(get("/prosuna/api/actuator/health/" + probe)
                        .contextPath("/prosuna")
                        .cookie(new Cookie(RedisCsrfTokenRepository.COOKIE_NAME, "existing-csrf-id")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", JsonCompareMode.STRICT))
                .andExpect(header().doesNotExist(RedisCsrfTokenRepository.HEADER_NAME));

        verifyNoInteractions(redis);
    }

    @Test
    void brokenLivenessShouldReturnServiceUnavailableWithoutDetails() throws Exception {
        AvailabilityChangeEvent.publish(applicationContext, LivenessState.BROKEN);

        mockMvc.perform(get("/api/actuator/health/liveness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"status\":\"DOWN\"}", JsonCompareMode.STRICT));

        verifyNoInteractions(redis);
    }

    @Test
    void refusingReadinessShouldReturnServiceUnavailableWithoutDetails() throws Exception {
        AvailabilityChangeEvent.publish(applicationContext, ReadinessState.REFUSING_TRAFFIC);

        mockMvc.perform(get("/api/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"status\":\"OUT_OF_SERVICE\"}", JsonCompareMode.STRICT));

        verifyNoInteractions(redis);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/actuator",
            "/api/actuator/health/db",
            "/api/actuator/health/liveness/livenessState",
            "/api/actuator/health/readiness/readinessState",
            "/api/private"
    })
    void otherProtectedPathsShouldStillRequireAuthentication(String path) throws Exception {
        allowCsrfTokenStorage();

        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"liveness", "readiness"})
    void anonymousProbePostShouldStillRequireAuthentication(String probe) throws Exception {
        allowCsrfTokenStorage();

        mockMvc.perform(post("/api/actuator/health/" + probe))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rootHealthShouldHideDetailsWithoutIssuingCsrfToken() throws Exception {
        mockMvc.perform(get("/api/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(header().doesNotExist(RedisCsrfTokenRepository.HEADER_NAME))
                .andExpect(cookie().doesNotExist(RedisCsrfTokenRepository.COOKIE_NAME));

        verifyNoInteractions(redis);
    }

    @SuppressWarnings("unchecked")
    private void allowCsrfTokenStorage() {
        when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import({SecurityConfiguration.class, RedisCsrfTokenRepository.class, CsrfResponseHeaderFilter.class})
    static class ProbeConfiguration {
    }
}
