package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.core.models.HttpResponseImpl;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.KeyCloakOIDCConfig;
import de.aivot.GoverBackend.user.models.KeycloakUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class KeyCloakApiServiceTest {
    @Mock
    private HttpService httpService;

    private KeyCloakApiService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        var config = new KeyCloakOIDCConfig();
        config.setHostname("https://keycloak.example.com");
        config.setRealm("staff");
        config.setBackendClientId("backend-client");
        config.setBackendClientSecret("backend-secret");

        service = new KeyCloakApiService(config, httpService);
    }

    @Test
    void createUser_MapsKnownKeycloakValidationErrorsToGermanMessages() throws Exception {
        when(httpService.postFormUrlEncoded(any(), anyMap())).thenReturn(
                new HttpResponseImpl<>(200, "{\"access_token\":\"token\",\"expires_in\":300}")
        );
        when(httpService.postEntity(any(), any(String.class), any(), eq(Void.class))).thenThrow(
                new RestClientResponseException(
                        "400 Bad Request",
                        400,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        """
                        {"field":"lastName","errorMessage":"error-person-name-invalid-character","params":["lastName"]}
                        """.getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8
                )
        );

        var exception = assertThrows(ResponseException.class, () -> service.createUser(
                new KeycloakUser()
                        .setFirstName("Jane")
                        .setLastName("Müller^")
                        .setEmail("jane.doe@example.com")
                        .setEnabled(true)
        ));

        assertEquals("Der eingegebene Wert für „Nachname“ enthält unzulässige Zeichen. Bitte entfernen Sie unzulässige Sonderzeichen und versuchen Sie es erneut.", exception.getTitle());
        assertEquals("""
                        {"field":"lastName","errorMessage":"error-person-name-invalid-character","params":["lastName"]}
                        """, exception.getDetails());
    }
}
