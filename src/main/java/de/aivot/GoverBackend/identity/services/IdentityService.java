package de.aivot.GoverBackend.identity.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.identity.cache.entities.IdentityCacheEntity;
import de.aivot.GoverBackend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.GoverBackend.identity.constants.IdentityBodyParameterConstants;
import de.aivot.GoverBackend.identity.constants.IdentityQueryParameterConstants;
import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityResultState;
import de.aivot.GoverBackend.identity.models.IdentityAuthTokenData;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.secrets.services.SecretService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IdentityService {
    public static final String DEFAULT_RESPONSE_TYPE = "code";
    public static final String DEFAULT_LOGIN_VALUE = "true";
    public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    public static final String CONTENT_TYPE_HEADER_KEY = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String AUTHORIZATION_HEADER_KEY = "Authorization";

    private final GoverConfig goverConfig;
    private final SecretService secretService;
    private final HttpService httpService;
    private final IdentityProviderService identityProviderService;
    private final IdentityCacheRepository identityCacheRepository;

    @Autowired
    public IdentityService(
            GoverConfig goverConfig,
            SecretService secretService,
            HttpService httpService,
            IdentityProviderService identityProviderService,
            IdentityCacheRepository identityCacheRepository
    ) {
        this.goverConfig = goverConfig;
        this.secretService = secretService;
        this.httpService = httpService;
        this.identityProviderService = identityProviderService;
        this.identityCacheRepository = identityCacheRepository;
    }

    /**
     * Constructs a redirect URL for an identity provider's authorization endpoint.
     *
     * <p>This method builds a URL that redirects the user to the identity provider's authorization
     * endpoint with the necessary query parameters, including client ID, response type, login flag,
     * redirect URI, and scopes. It also validates the referer and combines default and additional scopes.</p>
     *
     * @param providerKey      The key of the identity provider. Can be <code>null</code>.
     * @param callbackBaseUrl  The base URL for the callback endpoint. Must not be <code>null</code>.
     * @param referer          The referer of the request, typically from the "Referer" header. Can be <code>null</code>.
     * @param additionalScopes A list of additional scopes to include in the request. Can be <code>null</code>.
     * @return A {@link URI} representing the constructed redirect URL.
     * @throws ResponseException If the provider key is invalid, the provider is not enabled, the referer is invalid,
     *                           or any required configuration is missing.
     */
    @Nonnull
    public URI createRedirectURL(
            @Nullable String providerKey,
            @Nonnull URI callbackBaseUrl,
            @Nullable String referer,
            @Nullable List<String> additionalScopes
    ) throws ResponseException {
        var provider = getIdentityProviderEntity(providerKey);
        var resolvedReferer = resolveReferer(referer, goverConfig.getGoverHostname());
        var combinedScopes = getCombinedScopes(provider, additionalScopes);

        // Construct the redirect URL
        var redirectUrl = UriComponentsBuilder
                .fromUri(callbackBaseUrl)
                .queryParam(IdentityQueryParameterConstants.ORIGIN, resolvedReferer.toString()) // The origin is the origin of the first request resolved from the referer header
                .build()
                .toString();

        var resolvedAuthorizationUri = resolveRelativeOrAbsoluteURL(provider.getAuthorizationEndpoint());

        // Create the redirect URL
        var builder = UriComponentsBuilder
                .newInstance()
                .uri(resolvedAuthorizationUri)
                .queryParam(IdentityQueryParameterConstants.AUTH_ENDPOINT_CLIENT_ID, provider.getClientId())
                .queryParam(IdentityQueryParameterConstants.AUTH_ENDPOINT_RESPONSE_TYPE, DEFAULT_RESPONSE_TYPE)
                .queryParam(IdentityQueryParameterConstants.AUTH_ENDPOINT_LOGIN, DEFAULT_LOGIN_VALUE)
                .queryParam(IdentityQueryParameterConstants.AUTH_ENDPOINT_REDIRECT_URI, redirectUrl)
                .queryParam(IdentityQueryParameterConstants.AUTH_ENDPOINT_SCOPE, combinedScopes);

        // Add the client secret if available
        getClientSecret(provider)
                .ifPresent(secret -> builder.queryParam(
                        IdentityQueryParameterConstants.AUTH_ENDPOINT_CLIENT_SECRET,
                        secret
                ));

        // Add any additional parameters specified in the provider entity
        provider
                .getAdditionalParams()
                .forEach(qp -> builder.queryParam(qp.getKey(), qp.getValue()));

        return builder
                .build()
                .toUri();
    }

    /**
     * Handles the callback from the identity provider after user authentication.
     *
     * <p>This method processes the authorization code received from the identity provider,
     * retrieves the authentication token, fetches user information, performs a logout
     * with the identity provider, and caches the identity data for future use.</p>
     *
     * @param providerKey       The key of the identity provider. Can be <code>null</code>.
     * @param authorizationCode The authorization code received from the identity provider. Must not be <code>null</code>.
     * @param callbackBaseUrl       The callback URL used during the authorization process. Must not be <code>null</code>.
     * @return The {@link IdentityCacheEntity} containing the cached identity data.
     * @throws ResponseException If the authorization code is missing, the identity provider is invalid or not enabled,
     *                           the token cannot be retrieved, user information cannot be fetched, or logout fails.
     */
    @Nonnull
    public IdentityCacheEntity handleCallback(
            @Nullable String providerKey,
            @Nullable String authorizationCode,
            @Nonnull URI callbackBaseUrl,
            @Nonnull String origin,
            @Nullable String existingIdentityId
    ) throws ResponseException {
        if (authorizationCode == null) {
            throw ResponseException
                    .badRequest("Es wurde kein Autorisierungscode übergeben.");
        }

        var provider = getIdentityProviderEntity(providerKey);

        // Construct the redirect URL
        var redirectUrl = UriComponentsBuilder
                .fromUri(callbackBaseUrl)
                .queryParam(IdentityQueryParameterConstants.ORIGIN, origin) // The origin is the origin of the first request resolved from the referer header
                .build()
                .toUri();

        var authToken = fetchAuthToken(
                provider,
                authorizationCode,
                redirectUrl
        );

        var userInfo = fetchUserInfo(
                provider,
                authToken
        );

        performLogout(
                provider,
                authToken
        );

        var identityId = existingIdentityId;
        if (identityId == null) {
            identityId = UUID.randomUUID().toString();
        }

        var identityEntity = new IdentityCacheEntity()
                .setId(identityId)
                .setIdentityData(userInfo)
                .setMetadataIdentifier(provider.getMetadataIdentifier())
                .setProviderKey(provider.getKey());

        identityCacheRepository
                .save(identityEntity);

        return identityEntity;
    }

    /**
     * Constructs a URL to redirect the user to an error page.
     *
     * <p>This method validates the provided origin against the application's configured hostname
     * and builds a URL with query parameters to describe the error. The query parameters include:</p>
     * <ul>
     *   <li><code>error</code>: The error code or message.</li>
     *   <li><code>error_description</code>: A detailed description of the error (optional).</li>
     *   <li><code>state</code>: A state code indicating the type of error, defaulting to "UnknownError".</li>
     * </ul>
     *
     * @param origin           The origin of the request, typically from the "Origin" header. Can be <code>null</code>.
     * @param error            The error code or message. Must not be <code>null</code>.
     * @param errorDescription A detailed description of the error. Can be <code>null</code>.
     * @return A string representing the constructed error redirect URL.
     * @throws ResponseException If the origin is invalid or does not match the application's hostname.
     */
    public String createErrorRedirectURL(
            @Nullable String origin,
            @Nonnull String error,
            @Nullable String errorDescription
    ) throws ResponseException {
        var resolvedOrigin = resolveReferer(origin, goverConfig.getGoverHostname());
        return UriComponentsBuilder
                .fromUri(resolvedOrigin)
                .queryParam(IdentityQueryParameterConstants.REMOTE_AUTH_ERROR, error)
                .queryParam(IdentityQueryParameterConstants.REMOTE_AUTH_ERROR_DESCRIPTION, errorDescription)
                .queryParam(IdentityQueryParameterConstants.RESULT_STATE_CODE, IdentityResultState.UnknownError.getKey())
                .build()
                .toString();
    }

    // region Utility methods

    /**
     * Resolves a given URL to an absolute {@link URI}.
     *
     * <p>If the provided URL starts with a forward slash (<code>/</code>), it is treated as a relative path.
     * In this case, the method uses the application's configuration to construct an absolute URL
     * by appending the relative path to the base URL defined in the {@link GoverConfig}.</p>
     *
     * <p>If the provided URL does not start with a forward slash, it is assumed to be an absolute URL
     * and is directly converted into a {@link URI} object.</p>
     *
     * @param url The URL to resolve. Must not be null.
     * @return A {@link URI} representing the resolved absolute URL.
     * @throws IllegalArgumentException If the provided URL is invalid or cannot be converted to a {@link URI}.
     */
    @Nonnull
    private URI resolveRelativeOrAbsoluteURL(@Nonnull String url) {
        if (url.startsWith("/")) {
            var absoluteUrl = goverConfig.createUrl(url);
            return URI.create(absoluteUrl);
        } else {
            return URI.create(url);
        }
    }

    /**
     * Retrieves an {@link IdentityProviderEntity} based on the provided key.
     *
     * <p>This method ensures that the identity provider exists and is enabled before returning it.
     * If the provider key is missing, invalid, or the provider is not enabled, a {@link ResponseException} is thrown.</p>
     *
     * @param providerKey The key of the identity provider to retrieve. Can be <code>null</code>.
     * @return The {@link IdentityProviderEntity} corresponding to the provided key.
     * @throws ResponseException If the provider key is missing, the provider does not exist, or the provider is not enabled.
     */
    @Nonnull
    private IdentityProviderEntity getIdentityProviderEntity(@Nullable String providerKey) throws ResponseException {
        // Check if the provider key is null
        if (providerKey == null) {
            throw ResponseException
                    .badRequest("Der Nutzerkontenanbieter ist nicht angegeben.");
        }

        // Retrieve provider or throw not found exception
        var provider = identityProviderService
                .retrieve(providerKey)
                .orElseThrow(() -> ResponseException.notFound("Der Nutzerkontenanbieter existiert nicht."));

        // Check if the provider is enabled
        if (!provider.getIsEnabled()) {
            throw ResponseException
                    .badRequest("Der Nutzerkontenanbieter ist nicht aktiviert.");
        }

        return provider;
    }

    /**
     * Combines the default scopes of an identity provider with additional scopes.
     *
     * <p>This method creates a unified list of scopes by merging the default scopes
     * from the provided {@link IdentityProviderEntity} with any additional scopes
     * specified in the input. Duplicate scopes and <code>null</code> values are removed,
     * and the resulting list is returned as a single space-separated string.</p>
     *
     * @param provider         The {@link IdentityProviderEntity} containing the default scopes.
     *                         Must not be <code>null</code>.
     * @param additionalScopes A list of additional scopes to include. Can be <code>null</code>.
     * @return A space-separated string of combined scopes, with duplicates and <code>null</code> values removed.
     */
    @Nonnull
    private static String getCombinedScopes(
            @Nonnull IdentityProviderEntity provider,
            @Nullable List<String> additionalScopes
    ) {
        // Create a copy of the list of scopes
        var combinedScopes = new LinkedList<>(provider.getDefaultScopes());

        // Check if additionalScopes is not null and add them to the list
        if (additionalScopes != null) {
            combinedScopes.addAll(additionalScopes);
        }

        // Remove any duplicate scopes or null from the list
        return combinedScopes
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(" "));
    }

    /**
     * Validates and resolves the referer of a request against the application's configured hostname.
     *
     * <p>This method ensures that the provided referer matches the application's hostname in terms of
     * scheme, host, and port. If the referer is invalid or does not match, a {@link ResponseException} is thrown.</p>
     *
     * <p>The method performs the following steps:
     * <ul>
     *   <li>Checks if the <code>referer</code> parameter is null and throws a {@link ResponseException} if it is missing.</li>
     *   <li>Parses the <code>referer</code> string into a {@link URI} object, throwing a {@link ResponseException} if the format is invalid.</li>
     *   <li>Parses the <code>hostname</code> string into a {@link URI} object, throwing a {@link ResponseException} if the format is invalid.</li>
     *   <li>Compares the scheme, host, and port of the <code>referer</code> URI with the application's hostname URI.</li>
     *   <li>Returns the validated <code>referer</code> URI if all checks pass.</li>
     * </ul>
     * </p>
     *
     * @param referer   The referer of the request, typically from the "Origin" header. Can be <code>null</code>.
     * @param hostname The application's configured hostname. Must not be <code>null</code>.
     * @return A {@link URI} representing the validated referer.
     * @throws ResponseException If the referer is missing, invalid, or does not match the application's hostname.
     */
    private static URI resolveReferer(@Nullable String referer, @Nonnull String hostname) throws ResponseException {
        // Check if the request has a referer header
        if (referer == null) {
            throw ResponseException
                    .badRequest("Es wurde kein Referer-Header übergeben.");
        }

        // Check if referer header is a valid URI
        URI refererURI;
        try {
            refererURI = new URI(referer);
            refererURI.toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            throw ResponseException
                    .badRequest("Der Referer-Header ist ungültig.");
        }

        // Get the gover hostname uri
        URI goverHostname;
        try {
            goverHostname = new URI(hostname);
            goverHostname.toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            throw ResponseException
                    .internalServerError("Der Gover-Hostname ist ungültig.");
        }

        // Check if the referer is allowed
        if (!goverHostname.getScheme().equalsIgnoreCase(refererURI.getScheme())) {
            throw ResponseException
                    .badRequest("Das Protokoll des Referer-Headers ist ungültig.");
        }
        if (!goverHostname.getHost().equalsIgnoreCase(refererURI.getHost())) {
            throw ResponseException
                    .badRequest("Der Host des Referer-Headers ist ungültig.");
        }
        if (goverHostname.getPort() != refererURI.getPort()) {
            throw ResponseException
                    .badRequest("Der Port des Referer-Headers ist ungültig.");
        }

        return refererURI;
    }

    /**
     * Fetches an authentication token from the identity provider using the authorization code flow.
     *
     * <p>This method sends a POST request to the identity provider's token endpoint with the required
     * parameters, including the authorization code, client ID, and redirect URI. If a client secret
     * is available, it is included in the request. The response is parsed into an {@link IdentityAuthTokenData}
     * object containing the access token, refresh token, and other related data.</p>
     *
     * @param provider    The {@link IdentityProviderEntity} representing the identity provider. Must not be <code>null</code>.
     * @param code        The authorization code received from the identity provider. Must not be <code>null</code>.
     * @param callbackUrl The callback URL used during the authorization process. Must not be <code>null</code>.
     * @return An {@link IdentityAuthTokenData} object containing the authentication token data.
     * @throws ResponseException If the token endpoint cannot be reached, the response status code is invalid,
     *                           or the response body cannot be parsed.
     */
    @Nonnull
    private IdentityAuthTokenData fetchAuthToken(
            @Nonnull IdentityProviderEntity provider,
            @Nonnull String code,
            @Nonnull URI callbackUrl
    ) throws ResponseException {
        var body = new HashMap<String, String>();
        body.put(IdentityBodyParameterConstants.TOKEN_ENDPOINT_GRANT_TYPE, GRANT_TYPE_AUTHORIZATION_CODE);
        body.put(IdentityBodyParameterConstants.TOKEN_ENDPOINT_CLIENT_ID, provider.getClientId());
        body.put(IdentityBodyParameterConstants.TOKEN_ENDPOINT_CODE, code);
        body.put(IdentityBodyParameterConstants.TOKEN_ENDPOINT_REDIRECT_URI, callbackUrl.toString());

        getClientSecret(provider)
                .ifPresent(secret -> body.put(IdentityBodyParameterConstants.TOKEN_ENDPOINT_CLIENT_SECRET, secret));

        var uri = resolveRelativeOrAbsoluteURL(provider.getTokenEndpoint());

        HttpResponse<String> response;
        try {
            response = httpService
                    .postFormUrlEncoded(uri, body);
        } catch (IOException e) {
            throw ResponseException
                    .internalServerError(
                            e,
                            "Fehler beim Verbindungsaufbau zum Nutzerkontenanbieter %s (%s) für den Zugriffsschlüssel",
                            provider.getName(),
                            provider.getKey()
                    );
        } catch (InterruptedException e) {
            throw ResponseException
                    .internalServerError(
                            e,
                            "Verbindungsabbruch beim Abrufen des Zugriffsschlüssels für Nutzerkontenanbieter %s (%s)",
                            provider.getName(),
                            provider.getKey()
                    );
        }

        if (response.statusCode() != 200) {
            throw ResponseException
                    .internalServerError(
                            "Ungültiger Status-Code beim Abrufen des Zugriffsschlüssels für Nutzerkontenanbieter %s (%s): %d",
                            provider.getName(),
                            provider.getKey(),
                            response.statusCode()
                    );
        }

        var responseBody = response.body();

        IdentityAuthTokenData accessTokenData;
        try {
            accessTokenData = new ObjectMapper()
                    .readValue(responseBody, IdentityAuthTokenData.class);
        } catch (JsonProcessingException e) {
            throw ResponseException
                    .internalServerError(
                            e,
                            "Fehler beim Verarbeiten der Rückgabe des Zugriffsschlüssels des Nutzerkontenanbieters %s (%s)",
                            provider.getName(),
                            provider.getKey()
                    );
        }

        return accessTokenData;
    }

    /**
     * Fetches user information from the identity provider's user info endpoint.
     *
     * <p>This method sends a GET request to the user info endpoint of the identity provider
     * using the access token provided in the {@link IdentityAuthTokenData}. The response is
     * parsed into a map of user attributes, which includes both mapped attributes defined
     * in the identity provider's configuration and any additional attributes from the raw data.</p>
     *
     * @param provider      The {@link IdentityProviderEntity} representing the identity provider. Must not be <code>null</code>.
     * @param authTokenData The {@link IdentityAuthTokenData} containing the access token. Must not be <code>null</code>.
     * @return A {@link Map} containing the user information, with attribute names as keys and their values as strings.
     * @throws ResponseException If the user info endpoint is not configured, the endpoint cannot be reached,
     *                           the response status code is invalid, or the response body cannot be parsed.
     */
    @Nonnull
    private Map<String, String> fetchUserInfo(
            @Nonnull IdentityProviderEntity provider,
            @Nonnull IdentityAuthTokenData authTokenData
    ) throws ResponseException {
        if (provider.getUserinfoEndpoint() == null) {
            return Map.of();
        }

        var uri = resolveRelativeOrAbsoluteURL(provider.getUserinfoEndpoint());

        HttpResponse<String> response;
        try {
            response = httpService
                    .get(uri, Map.of(
                            CONTENT_TYPE_HEADER_KEY, CONTENT_TYPE_JSON,
                            AUTHORIZATION_HEADER_KEY, "Bearer " + authTokenData.accessToken()
                    ));
        } catch (IOException e) {
            throw ResponseException
                    .internalServerError(
                            e,
                            "Fehler beim Verbindungsaufbau zum Nutzerkontenanbieter %s (%s) für die Nutzerinformationen",
                            provider.getName(),
                            provider.getKey()
                    );
        } catch (InterruptedException e) {
            throw ResponseException
                    .internalServerError(
                            e,
                            "Verbindungsabbruch beim Abrufen der Nutzerinformationen für Nutzerkontenanbieter %s (%s)",
                            provider.getName(),
                            provider.getKey()
                    );
        }

        if (response.statusCode() != 200) {
            throw ResponseException
                    .internalServerError(
                            "Ungültiger Status-Code beim Abrufen der Nutzerinformationen für Nutzerkontenanbieter %s (%s): %d",
                            provider.getName(),
                            provider.getKey(),
                            response.statusCode()
                    );
        }

        Map<String, Object> rawData;
        try {
            rawData = new ObjectMapper()
                    .readerForMapOf(String.class)
                    .readValue(response.body());
        } catch (JsonProcessingException e) {
            throw ResponseException
                    .internalServerError(
                            e,
                            "Fehler beim Verarbeiten der Rückgabe der Nutzerinformationen des Nutzerkontenanbieters %s (%s)",
                            provider.getName(),
                            provider.getKey()
                    );
        }

        var map = new HashMap<String, String>();
        for (var mapping : provider.getAttributes()) {
            var value = rawData.get(mapping.getKeyInData());

            if (value == null) {
                map.put(mapping.getKeyInData(), null);
            } else {
                map.put(mapping.getKeyInData(), value.toString());
            }
        }

        for (var entry : rawData.entrySet()) {
            map.put(entry.getKey(), entry.getValue().toString());
        }

        return map;
    }

    /**
     * Performs the logout process for the given identity provider.
     *
     * <p>This method sends a POST request to the identity provider's end session endpoint
     * to terminate the user's session. The request includes the client ID, refresh token,
     * and optionally the client secret. If the end session endpoint is not configured,
     * the method exits without performing any action.</p>
     *
     * @param provider      The {@link IdentityProviderEntity} representing the identity provider. Must not be <code>null</code>.
     * @param authTokenData The {@link IdentityAuthTokenData} containing the access and refresh tokens. Must not be <code>null</code>.
     * @throws ResponseException If the end session endpoint cannot be reached, the response status code is invalid,
     *                           or the request fails due to connection issues or interruptions.
     */
    private void performLogout(
            @Nonnull IdentityProviderEntity provider,
            @Nonnull IdentityAuthTokenData authTokenData
    ) throws ResponseException {
        if (provider.getEndSessionEndpoint() == null) {
            return;
        }

        var uri = resolveRelativeOrAbsoluteURL(provider.getEndSessionEndpoint());

        var body = new HashMap<String, String>();
        body.put(IdentityBodyParameterConstants.LOGOUT_ENDPOINT_CLIENT_ID, provider.getClientId());
        body.put(IdentityBodyParameterConstants.LOGOUT_ENDPOINT_REFRESH_TOKEN, authTokenData.refreshToken());

        getClientSecret(provider)
                .ifPresent(secret -> body.put(IdentityBodyParameterConstants.LOGOUT_ENDPOINT_CLIENT_SECRET, secret));

        HttpResponse<String> response;
        try {
            response = httpService
                    .postFormUrlEncoded(uri, body, Map.of(
                            "Authorization", "Bearer " + authTokenData.accessToken()
                    ));
        } catch (IOException e) {
            throw ResponseException
                    .internalServerError(
                            e,
                            "Fehler beim Verbindungsaufbau zum Nutzerkontenanbieter %s (%s) für den Logout",
                            provider.getName(),
                            provider.getKey()
                    );
        } catch (InterruptedException e) {
            throw ResponseException
                    .internalServerError(
                            e,
                            "Verbindungsabbruch beim Logout für Nutzerkontenanbieter %s (%s)",
                            provider.getName(),
                            provider.getKey()
                    );
        }

        if (response.statusCode() != 204) {
            throw ResponseException
                    .internalServerError(
                            "Ungültiger Status-Code beim Logout für Nutzerkontenanbieter %s (%s): %d",
                            provider.getName(),
                            provider.getKey(),
                            response.statusCode()
                    );
        }
    }

    /**
     * Retrieves and decrypts the client secret for the given identity provider.
     *
     * <p>This method fetches the client secret associated with the provided {@link IdentityProviderEntity}.
     * If the client secret key is not set, it returns an empty {@link Optional}. If the secret cannot
     * be retrieved or decrypted, a {@link ResponseException} is thrown.</p>
     *
     * @param provider The {@link IdentityProviderEntity} for which the client secret is retrieved. Must not be <code>null</code>.
     * @return An {@link Optional} containing the decrypted client secret, or an empty {@link Optional} if no client secret key is set.
     * @throws ResponseException If the secret cannot be retrieved, does not exist, or cannot be decrypted.
     */
    @Nonnull
    private Optional<String> getClientSecret(@Nonnull IdentityProviderEntity provider) throws ResponseException {
        if (provider.getClientSecretKey() == null) {
            return Optional.empty();
        }

        var secret = secretService
                .retrieve(provider.getClientSecretKey())
                .orElseThrow(() -> ResponseException
                        .internalServerError(
                                "Das Geheimnis mit dem Schlüssel %s existiert nicht für den Nutzerkontenanbieter %s (%s)",
                                provider.getClientSecretKey(),
                                provider.getName(),
                                provider.getKey()
                        ));

        String decryptedSecret;
        try {
            decryptedSecret = secretService
                    .decrypt(secret);
        } catch (Exception e) {
            throw ResponseException
                    .internalServerError(
                            "Das Geheimnis mit dem Schlüssel %s für den Nutzerkontenanbieter %s (%s) konnte nicht entschlüsselt werden",
                            provider.getClientSecretKey(),
                            provider.getName(),
                            provider.getKey()
                    );
        }

        return Optional
                .of(decryptedSecret);
    }

    // endregion
}
