package de.aivot.GoverBackend.identity.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.identity.models.OpenIdConfiguration;
import de.aivot.GoverBackend.identity.repositories.IdentityProviderRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import de.aivot.GoverBackend.secrets.repositories.SecretRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing identity providers in the Gover system.
 *
 * <p>This class provides methods for performing CRUD operations on {@link IdentityProviderEntity} objects,
 * including creating, updating, deleting, and retrieving identity providers. It also includes
 * validation logic to ensure the integrity of the data and compliance with business rules.</p>
 *
 * <p>Key functionalities:</p>
 * <ul>
 *     <li>Retrieves identity providers with support for filtering and pagination.</li>
 *     <li>Validates and updates identity provider details, ensuring fixed values like type and key are preserved.</li>
 *     <li>Deletes identity providers, ensuring no linked forms exist and the provider is of type {@link IdentityProviderType#Custom}.</li>
 *     <li>Checks for the existence of identity providers by ID or specification.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 *     IdentityProviderService service = new IdentityProviderService(...);
 *     Page&lt;IdentityProviderEntity&gt; providers = service.performList(pageable, specification, filter);
 *     Optional&lt;IdentityProviderEntity&gt; provider = service.retrieve("provider-id");
 * </pre>
 *
 * @see IdentityProviderEntity
 * @see IdentityProviderType
 * @see ResponseException
 */
@Service
public class IdentityProviderService implements EntityService<IdentityProviderEntity, String> {
    private final IdentityProviderRepository identityProviderRepository;
    private final SecretRepository secretRepository;
    private final AssetRepository assetRepository;
    private final FormRepository formRepository;
    private final HttpService httpService;

    @Autowired
    public IdentityProviderService(
            IdentityProviderRepository identityProviderRepository,
            SecretRepository secretRepository,
            AssetRepository assetRepository,
            FormRepository formRepository,
            HttpService httpService
    ) {
        this.identityProviderRepository = identityProviderRepository;
        this.secretRepository = secretRepository;
        this.assetRepository = assetRepository;
        this.formRepository = formRepository;
        this.httpService = httpService;
    }

    /**
     * Prepares an {@link IdentityProviderEntity} by retrieving and parsing the OpenID Connect configuration
     * from the specified endpoint.
     *
     * <p>This method performs the following steps:</p>
     * <ul>
     *     <li>Sends an HTTP GET request to the provided endpoint to fetch the OpenID Connect configuration.</li>
     *     <li>Validates the HTTP response status code and throws a {@link ResponseException} if it is not 200.</li>
     *     <li>Parses the response body into an {@link OpenIdConfiguration} object.</li>
     *     <li>Maps the configuration properties to a new {@link IdentityProviderEntity} instance.</li>
     * </ul>
     *
     * <p>Exceptions are thrown in the following cases:</p>
     * <ul>
     *     <li>If the endpoint cannot be reached or an I/O error occurs.</li>
     *     <li>If the HTTP response status code is not 200.</li>
     *     <li>If the response body cannot be parsed into a valid {@link OpenIdConfiguration} object.</li>
     * </ul>
     *
     * @param endpoint The OpenID Connect discovery endpoint URL.
     * @return A prepared {@link IdentityProviderEntity} with the configuration details.
     * @throws ResponseException If the endpoint is unreachable, returns an invalid response, or the response cannot be parsed.
     */
    @Nonnull
    public IdentityProviderEntity prepare(@Nonnull String endpoint) throws ResponseException {
        var uri = URI.create(endpoint);

        HttpResponse<String> response;
        try {
            response = httpService.get(uri);
        } catch (IOException | InterruptedException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Der Endpoint %s konnte nicht erreicht werden. Bitte überprüfen Sie den Endpoint.",
                    uri.toString()
            );
        }

        if (response.statusCode() != 200) {
            throw ResponseException.conflict(
                    "Der Endpoint %s hat den Status %d zurückgegeben. Bitte überprüfen Sie den Endpoint.",
                    uri.toString(),
                    response.statusCode()
            );
        }

        var body = response.body();

        OpenIdConfiguration openIdConfiguration;
        try {
            openIdConfiguration = new ObjectMapper()
                    .readValue(body, OpenIdConfiguration.class);
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Der Endpoint %s hat eine ungültige Antwort zurückgegeben. Bitte überprüfen Sie den Endpoint.",
                    uri.toString()
            );
        }

        return new IdentityProviderEntity()
                .setAuthorizationEndpoint(openIdConfiguration.getAuthorizationEndpoint())
                .setTokenEndpoint(openIdConfiguration.getTokenEndpoint())
                .setUserinfoEndpoint(openIdConfiguration.getUserinfoEndpoint())
                .setEndSessionEndpoint(openIdConfiguration.getEndSessionEndpoint())
                .setDefaultScopes(Arrays.asList(openIdConfiguration.getScopesSupported()));
    }

    /**
     * Creates a new {@link IdentityProviderEntity} with default values and saves it to the database.
     *
     * <p>This method performs the following steps:</p>
     * <ul>
     *     <li>Generates a unique key for the entity.</li>
     *     <li>Sets the type of the identity provider to {@link IdentityProviderType#Custom}.</li>
     *     <li>Validates and cleans up the {@code clientSecretKey} and {@code iconAssetKey} fields.</li>
     *     <li>Saves the entity to the database using the {@link IdentityProviderRepository}.</li>
     * </ul>
     *
     * @param entity The {@link IdentityProviderEntity} to be created.
     * @return The saved {@link IdentityProviderEntity}.
     * @throws ResponseException If an error occurs during the creation process.
     */
    @Nonnull
    @Override
    public IdentityProviderEntity create(@Nonnull IdentityProviderEntity entity) throws ResponseException {
        // Set default values
        entity.setKey(UUID.randomUUID().toString());
        entity.setType(IdentityProviderType.Custom);

        cleanClientSecretKey(entity);
        cleanIconAssetKey(entity);

        return identityProviderRepository
                .save(entity);
    }

    /**
     * Retrieves a paginated list of {@link IdentityProviderEntity} objects based on the provided
     * {@link Pageable}, {@link Specification}, and {@link Filter}.
     *
     * <p>This method allows filtering and pagination of identity providers stored in the database.</p>
     *
     * @param pageable      The pagination information.
     * @param specification The specification for filtering the entities.
     * @param filter        Additional filter criteria.
     * @return A {@link Page} containing the filtered and paginated {@link IdentityProviderEntity} objects.
     * @throws ResponseException If an error occurs during the retrieval process.
     */
    @Nullable
    @Override
    public Page<IdentityProviderEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<IdentityProviderEntity> specification,
            @Nullable Filter<IdentityProviderEntity> filter
    ) throws ResponseException {
        return identityProviderRepository
                .findAll(specification, pageable);
    }

    /**
     * Checks if an {@link IdentityProviderEntity} exists with the given ID.
     *
     * @param id The ID of the {@link IdentityProviderEntity} to check.
     * @return {@code true} if the entity exists, {@code false} otherwise.
     */
    @Override
    public boolean exists(@Nonnull String id) {
        return identityProviderRepository
                .existsById(id);
    }

    /**
     * Checks if an {@link IdentityProviderEntity} exists that matches the given {@link Specification}.
     *
     * @param specification The specification to match the entity.
     * @return {@code true} if a matching entity exists, {@code false} otherwise.
     */
    @Override
    public boolean exists(@Nonnull Specification<IdentityProviderEntity> specification) {
        return identityProviderRepository
                .exists(specification);
    }

    /**
     * Retrieves an {@link IdentityProviderEntity} by its ID.
     *
     * @param id The ID of the {@link IdentityProviderEntity} to retrieve.
     * @return An {@link Optional} containing the entity if found, or empty if not found.
     * @throws ResponseException If an error occurs during the retrieval process.
     */
    @Nonnull
    @Override
    public Optional<IdentityProviderEntity> retrieve(@Nonnull String id) throws ResponseException {
        return identityProviderRepository
                .findById(id);
    }

    /**
     * Retrieves an {@link IdentityProviderEntity} that matches the given {@link Specification}.
     *
     * @param specification The specification to match the entity.
     * @return An {@link Optional} containing the entity if found, or empty if not found.
     * @throws ResponseException If an error occurs during the retrieval process.
     */
    @Nonnull
    @Override
    public Optional<IdentityProviderEntity> retrieve(@Nonnull Specification<IdentityProviderEntity> specification) throws ResponseException {
        return identityProviderRepository
                .findOne(specification);
    }

    /**
     * Updates an existing {@link IdentityProviderEntity} with the provided updated values.
     *
     * <p>This method performs the following steps:</p>
     * <ul>
     *     <li>Ensures that the identity provider being updated is of type {@link IdentityProviderType#Custom}.
     *         If it is not, a {@link ResponseException} is thrown.</li>
     *     <li>Preserves fixed values such as the key and type from the existing entity.</li>
     *     <li>Validates and cleans up the {@code clientSecretKey} and {@code iconAssetKey} fields.</li>
     *     <li>Saves the updated entity to the database using the {@link IdentityProviderRepository}.</li>
     * </ul>
     *
     * @param id             The ID of the entity being updated.
     * @param updatedEntity  The {@link IdentityProviderEntity} containing the updated values.
     * @param existingEntity The existing {@link IdentityProviderEntity} to be updated.
     * @return The updated and saved {@link IdentityProviderEntity}.
     * @throws ResponseException If the entity is not of type {@link IdentityProviderType#Custom} or if an error occurs during the update process.
     */
    @Nonnull
    @Override
    public IdentityProviderEntity performUpdate(
            @Nonnull String id,
            @Nonnull IdentityProviderEntity updatedEntity,
            @Nonnull IdentityProviderEntity existingEntity
    ) throws ResponseException {
        if (existingEntity.getType() != IdentityProviderType.Custom) {
            throw ResponseException.conflict(
                    "Der Nutzerkontenanbieter %s (%s) ist ein Systemanbieter und kann nicht bearbeitet werden.",
                    existingEntity.getName(),
                    existingEntity.getKey()
            );
        }

        // Copy fix values
        updatedEntity.setKey(existingEntity.getKey());
        updatedEntity.setType(existingEntity.getType());

        cleanClientSecretKey(updatedEntity);
        cleanIconAssetKey(updatedEntity);

        return identityProviderRepository
                .save(updatedEntity);
    }

    /**
     * Deletes the specified {@link IdentityProviderEntity} from the database.
     *
     * <p>This method performs the following validations before deletion:</p>
     * <ul>
     *     <li>Ensures that the identity provider is of type {@link IdentityProviderType#Custom}.
     *         If it is not, a {@link ResponseException} is thrown.</li>
     *     <li>Checks if there are any forms linked to the identity provider. If such forms exist,
     *         a {@link ResponseException} is thrown, requiring the user to remove the links before deletion.</li>
     * </ul>
     *
     * @param entity The {@link IdentityProviderEntity} to be deleted.
     * @throws ResponseException If the entity is not of type {@link IdentityProviderType#Custom} or
     *                           if there are forms linked to the identity provider.
     */
    @Override
    public void performDelete(@Nonnull IdentityProviderEntity entity) throws ResponseException {
        if (entity.getType() != IdentityProviderType.Custom) {
            throw ResponseException.conflict(
                    "Der Nutzerkontenanbieter %s (%s) ist ein Systemanbieter und kann nicht gelöscht werden.",
                    entity.getName(),
                    entity.getKey()
            );
        }

        var linkedFormExists = formRepository
                .existsWithLinkedIdentityProvider(entity.getKey());

        if (linkedFormExists) {
            throw ResponseException.conflict(
                    "Für den Nutzerkontenanbieter %s (%s) existieren noch Formulare, die diesen Anbieter verwenden. Bitte entfernen Sie diese Verknüpfung, bevor Sie den Anbieter löschen.",
                    entity.getName(),
                    entity.getKey()
            );
        }

        identityProviderRepository
                .delete(entity);
    }

    // region Helpers

    private void cleanClientSecretKey(@Nonnull IdentityProviderEntity updatedEntity) {
        if (StringUtils.isNullOrEmpty(updatedEntity.getClientSecretKey())) {
            updatedEntity.setClientSecretKey(null);
        } else {
            Optional<SecretEntity> secretEntity;
            try {
                secretEntity = secretRepository
                        .findById(updatedEntity.getClientSecretKey());
            } catch (Exception e) {
                secretEntity = Optional.empty();
            }

            if (secretEntity.isEmpty()) {
                updatedEntity.setClientSecretKey(null);
            }
        }
    }

    private void cleanIconAssetKey(@Nonnull IdentityProviderEntity updatedEntity) {
        if (StringUtils.isNullOrEmpty(updatedEntity.getIconAssetKey())) {
            updatedEntity.setIconAssetKey(null);
        } else {
            Optional<AssetEntity> assetEntity;
            try {
                assetEntity = assetRepository
                        .findById(updatedEntity.getIconAssetKey());
            } catch (Exception e) {
                assetEntity = Optional.empty();
            }

            if (assetEntity.isEmpty()) {
                updatedEntity.setIconAssetKey(null);
            }
        }
    }

    // endregion
}
