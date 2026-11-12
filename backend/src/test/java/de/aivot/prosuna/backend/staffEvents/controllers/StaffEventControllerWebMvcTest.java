package de.aivot.prosuna.backend.staffEvents.controllers;

import de.aivot.prosuna.backend.ServerConfiguration;
import de.aivot.prosuna.backend.security.CsrfResponseHeaderFilter;
import de.aivot.prosuna.backend.security.RedisCsrfTokenRepository;
import de.aivot.prosuna.backend.security.SecurityConfiguration;
import de.aivot.prosuna.backend.staffEvents.models.StaffEvent;
import de.aivot.prosuna.backend.staffEvents.models.StaffEventAudience;
import de.aivot.prosuna.backend.staffEvents.services.StaffEventService;
import de.aivot.prosuna.backend.system.controllers.AuthController;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = StaffEventController.class,
        properties = "spring.flyway.repairOnMigrate=false",
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class,
                OAuth2ResourceServerWebSecurityAutoConfiguration.class
        }
)
@ContextConfiguration(classes = {
        StaffEventController.class,
        StaffEventService.class,
        StaffEventControllerWebMvcTest.InstanceSevenAudience.class
})
// ServerConfiguration negotiates every response as JSON, which a stream has to work with.
@Import({
        ServerConfiguration.class,
        SecurityConfiguration.class,
        RedisCsrfTokenRepository.class,
        CsrfResponseHeaderFilter.class
})
class StaffEventControllerWebMvcTest {
    private static final String ACCESS_TOKEN = "staff-access-token";
    private static final String USER_ID = "staff-user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffEventService events;

    @MockitoBean
    private UserService users;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private StringRedisTemplate redis;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception {
        when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));
        when(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(Jwt
                .withTokenValue(ACCESS_TOKEN)
                .header("alg", "none")
                .subject(USER_ID)
                .claim("realm_access", Map.of("roles", List.of("user")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(30))
                .build());
        when(users.fromJWT(any())).thenReturn(Optional.of(new UserEntity().setId(USER_ID)));
    }

    @Test
    void rejectsRequestsWithoutAccessToken() throws Exception {
        mockMvc
                .perform(get("/api/staff-events/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void streamsOnlyTheEventsTheUserMayReceive() throws Exception {
        var result = subscribe();

        events.deliver("{\"type\":\"process-instance\",\"scope\":\"7\",\"userId\":null}");
        // The audience does not allow the user to receive changes of instance 8.
        events.deliver("{\"type\":\"process-instance\",\"scope\":\"8\",\"userId\":null}");
        // Without an audience, an event is only delivered to the user it names.
        events.deliver("{\"type\":\"unknown\",\"scope\":\"9\",\"userId\":null}");
        events.deliver("{\"type\":\"notification\",\"scope\":\"10\",\"userId\":\"" + USER_ID + "\"}");
        events.deliver("{\"type\":\"notification\",\"scope\":\"11\",\"userId\":\"other-user\"}");
        events.deliver("not json");

        var body = result.getResponse().getContentAsString();
        assertTrue(body.contains("data:{\"type\":\"process-instance\",\"scope\":\"7\",\"userId\":null}\n\n"), body);
        assertTrue(body.contains("\"scope\":\"10\""), body);
        assertFalse(body.contains("\"scope\":\"8\""), body);
        assertFalse(body.contains("\"scope\":\"9\""), body);
        assertFalse(body.contains("\"scope\":\"11\""), body);
    }

    @Test
    void keepsTheStreamOpenLongerThanTheAccessToken() throws Exception {
        var result = subscribe();

        assertEquals(Duration.ofMinutes(30).toMillis(), result.getRequest().getAsyncContext().getTimeout());
    }

    @Test
    void sendsHeartbeatsToActiveUsers() throws Exception {
        var result = subscribe();
        when(userRepository.findActiveIdsByIdIn(any())).thenReturn(List.of(USER_ID));

        events.sendHeartbeats();

        assertTrue(result.getRequest().isAsyncStarted());
        assertEquals(":\n\n", result.getResponse().getContentAsString());
    }

    @Test
    void endsTheStreamOfADisabledUserAtTheNextHeartbeat() throws Exception {
        var result = subscribe();
        when(userRepository.findActiveIdsByIdIn(any())).thenReturn(List.of());

        events.sendHeartbeats();
        events.deliver("{\"type\":\"process-instance\",\"scope\":\"7\",\"userId\":null}");

        assertEquals("", result.getResponse().getContentAsString());
        // Spring MVC finishes an ended stream in an async dispatch, which passes the security filter chain again.
        mockMvc
                .perform(asyncDispatch(result))
                .andExpect(status().isOk());
    }

    private MvcResult subscribe() throws Exception {
        return mockMvc
                .perform(get("/api/staff-events/")
                        .cookie(new Cookie(AuthController.ACCESS_COOKIE_NAME, ACCESS_TOKEN)))
                .andExpect(request().asyncStarted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(header().string("X-Accel-Buffering", "no"))
                .andReturn();
    }

    static class InstanceSevenAudience implements StaffEventAudience {
        @Nonnull
        @Override
        public String type() {
            return "process-instance";
        }

        @Nonnull
        @Override
        public Collection<String> filter(@Nonnull StaffEvent event, @Nonnull Collection<String> userIds) {
            return "7".equals(event.scope()) ? userIds : List.of();
        }
    }
}
