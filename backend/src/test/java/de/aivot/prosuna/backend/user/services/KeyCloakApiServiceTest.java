package de.aivot.prosuna.backend.user.services;

import de.aivot.prosuna.backend.core.models.HttpResponseImpl;
import de.aivot.prosuna.backend.core.services.HttpService;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.KeycloakConfig;
import de.aivot.prosuna.backend.user.models.KeycloakUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class KeyCloakApiServiceTest {
    @Mock
    private HttpService httpService;

    private KeyCloakApiService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        var config = new KeycloakConfig();
        config.setHostname("https://keycloak.example.com");
        config.setInternalHostname(config.getHostname());
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

    @Test
    void createUser_WithMissingLocationExplainsTheSubmittedRequestWithoutRetrying() throws Exception {
        mockAcceptedCreation(ResponseEntity.status(HttpStatus.CREATED).build());

        try (var factory = mockStatic(JsonMapperFactory.class)) {
            factory.when(JsonMapperFactory::getInstance).thenReturn(JsonMapper.builder().build());

            var exception = assertThrows(ResponseException.class, () -> service.createUser(new KeycloakUser()));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
            assertEquals("Die Anfrage zur Kontoanlage wurde an den Identitätsanbieter übermittelt, die Kennung des Kontos konnte jedoch nicht ermittelt werden. Prüfen Sie beim Identitätsanbieter, ob das Konto angelegt wurde, bevor Sie die Anlage erneut ausführen.", exception.getTitle());
            verify(httpService).postEntity(any(), any(String.class), any(), eq(Void.class));
            verify(httpService, never()).get(any(URI.class), any());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 500})
    void createUser_WithFailedLookupExplainsTheSubmittedRequestWithoutRetrying(int lookupStatus) throws Exception {
        mockAcceptedCreation(ResponseEntity.created(URI.create("https://keycloak.example.com/users/created-user")).build());
        when(httpService.get(any(URI.class), any())).thenReturn(new HttpResponseImpl<>(lookupStatus, ""));

        try (var factory = mockStatic(JsonMapperFactory.class)) {
            factory.when(JsonMapperFactory::getInstance).thenReturn(JsonMapper.builder().build());

            var exception = assertThrows(ResponseException.class, () -> service.createUser(new KeycloakUser()));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
            assertEquals("Die Anfrage zur Kontoanlage wurde an den Identitätsanbieter übermittelt, das Konto konnte anschließend jedoch nicht geladen werden. Prüfen Sie beim Identitätsanbieter, ob das Konto angelegt wurde, bevor Sie die Anlage erneut ausführen.", exception.getTitle());
            verify(httpService).postEntity(any(), any(String.class), any(), eq(Void.class));
            verify(httpService).get(any(URI.class), any());
        }
    }

    private void mockAcceptedCreation(ResponseEntity<Void> response) throws Exception {
        when(httpService.postFormUrlEncoded(any(), anyMap())).thenReturn(
                new HttpResponseImpl<>(200, "{\"access_token\":\"token\",\"expires_in\":300}")
        );
        when(httpService.postEntity(any(), any(String.class), any(), eq(Void.class))).thenReturn(response);
    }
}
