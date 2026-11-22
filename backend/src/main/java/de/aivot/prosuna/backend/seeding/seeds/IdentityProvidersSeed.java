package de.aivot.prosuna.backend.seeding.seeds;

import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.models.IdentityAdditionalParameter;
import de.aivot.prosuna.backend.identity.models.IdentityAttributeMapping;
import de.aivot.prosuna.backend.identity.repositories.IdentityProviderRepository;
import de.aivot.prosuna.backend.models.config.KeycloakConfig;
import de.aivot.prosuna.backend.seeding.data.SystemIdentityProviderAttributes;
import de.aivot.prosuna.backend.seeding.models.SeedProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Seeds a stage and a production variant of each system identity provider. These providers are brokered by the
 * customer realm of Prosuna's Keycloak, so their endpoints are built from the configured Keycloak hostname.
 * Administrators cannot edit these endpoints, so existing system providers are updated whenever that hostname changes.
 * <p>
 * The identity provider service is not used, because it creates every provider as a custom provider.
 */
@Component
public class IdentityProvidersSeed implements SeedProvider {
    private static final String CUSTOMER_REALM_PATH = "/realms/customer";
    private static final String AUTHORIZATION_PATH = "/protocol/openid-connect/auth";
    private static final String TOKEN_PATH = "/protocol/openid-connect/token";
    private static final String USERINFO_PATH = "/protocol/openid-connect/userinfo";
    private static final String END_SESSION_PATH = "/protocol/openid-connect/logout";
    private static final String CLIENT_ID = "app";
    private static final String PKCE_METHOD = "S256";
    private static final String KEYCLOAK_IDP_HINT_PARAMETER = "kc_idp_hint";

    /**
     * @param descriptionSubject The provider name with its article, as used in the description.
     * @param keycloakAlias      The alias of the brokered provider in Keycloak, which also names its client scope.
     */
    private record SystemIdentityProvider(
            @Nonnull IdentityProviderType type,
            @Nonnull String name,
            @Nonnull String descriptionSubject,
            @Nonnull String keycloakAlias,
            @Nonnull String uniqueIdAttribute,
            @Nonnull List<IdentityAttributeMapping> attributes
    ) {
    }

    private static final List<SystemIdentityProvider> SYSTEM_IDENTITY_PROVIDERS = List.of(
            new SystemIdentityProvider(
                    IdentityProviderType.BayernId,
                    "BayernID",
                    "der BayernID",
                    "bayernid",
                    "bPK2",
                    SystemIdentityProviderAttributes.BAYERN_ID
            ),
            new SystemIdentityProvider(
                    IdentityProviderType.BundId,
                    "BundID",
                    "der BundID",
                    "bundid",
                    "bPK2",
                    SystemIdentityProviderAttributes.BUND_ID
            ),
            new SystemIdentityProvider(
                    IdentityProviderType.ShId,
                    "Servicekonto Schleswig-Holstein",
                    "des Servicekonto Schleswig-Holstein",
                    "schleswigholstein",
                    "preferred_username",
                    SystemIdentityProviderAttributes.SH_ID
            ),
            new SystemIdentityProvider(
                    IdentityProviderType.MUK,
                    "Mein Unternehmenskonto",
                    "des Mein Unternehmenskonto (MUK)",
                    "muk",
                    "preferred_username",
                    SystemIdentityProviderAttributes.MUK
            )
    );

    private final IdentityProviderRepository identityProviderRepository;
    private final KeycloakConfig keycloakConfig;

    public IdentityProvidersSeed(IdentityProviderRepository identityProviderRepository, KeycloakConfig keycloakConfig) {
        this.identityProviderRepository = identityProviderRepository;
        this.keycloakConfig = keycloakConfig;
    }


    @Override
    public boolean shouldSeed() {
        return SYSTEM_IDENTITY_PROVIDERS
                .stream()
                .anyMatch(provider -> isMissing(provider, true) || isMissing(provider, false))
                || !findSystemProvidersWithOutdatedEndpoints().isEmpty();
    }

    @Override
    public void seed() {
        for (var outdatedProvider : findSystemProvidersWithOutdatedEndpoints()) {
            identityProviderRepository.save(setEndpoints(outdatedProvider));
        }

        // System identity providers cannot be deleted, so a missing variant was never seeded and is created.
        for (var provider : SYSTEM_IDENTITY_PROVIDERS) {
            for (var testProvider : List.of(true, false)) {
                if (isMissing(provider, testProvider)) {
                    identityProviderRepository.save(setEndpoints(createIdentityProvider(provider, testProvider)));
                }
            }
        }
    }

    @Nonnull
    private List<IdentityProviderEntity> findSystemProvidersWithOutdatedEndpoints() {
        var expectedEndpoints = createEndpoints();
        return identityProviderRepository
                .findAllByTypeNot(IdentityProviderType.Custom)
                .stream()
                .filter(provider -> !expectedEndpoints.equals(Arrays.asList(
                        provider.getAuthorizationEndpoint(),
                        provider.getTokenEndpoint(),
                        provider.getUserinfoEndpoint(),
                        provider.getEndSessionEndpoint()
                )))
                .toList();
    }

    @Nonnull
    private IdentityProviderEntity setEndpoints(@Nonnull IdentityProviderEntity provider) {
        var endpoints = createEndpoints();
        return provider
                .setAuthorizationEndpoint(endpoints.get(0))
                .setTokenEndpoint(endpoints.get(1))
                .setUserinfoEndpoint(endpoints.get(2))
                .setEndSessionEndpoint(endpoints.get(3));
    }

    /**
     * @return The authorization, token, userinfo and end session endpoint of the customer realm, in this order.
     */
    @Nonnull
    private List<String> createEndpoints() {
        var realmUrl = keycloakConfig.getHostname().replaceAll("/+$", "") + CUSTOMER_REALM_PATH;
        return List.of(
                realmUrl + AUTHORIZATION_PATH,
                realmUrl + TOKEN_PATH,
                realmUrl + USERINFO_PATH,
                realmUrl + END_SESSION_PATH
        );
    }

    private boolean isMissing(@Nonnull SystemIdentityProvider provider, boolean testProvider) {
        return !identityProviderRepository.existsByTypeAndIsTestProvider(provider.type(), testProvider);
    }

    @Nonnull
    private static IdentityProviderEntity createIdentityProvider(@Nonnull SystemIdentityProvider provider,
                                                                 boolean testProvider) {
        var name = testProvider
                ? provider.name() + " (Vorproduktion)"
                : provider.name();
        var description = testProvider
                ? "Konfiguration für die Vorproduktionsumgebung " + provider.descriptionSubject() + "."
                : "Konfiguration für die Produktionsumgebung " + provider.descriptionSubject() + ".";
        var keycloakIdpHint = testProvider
                ? provider.keycloakAlias() + "-stage"
                : provider.keycloakAlias();

        return new IdentityProviderEntity()
                .setKey(UUID.randomUUID())
                .setMetadataIdentifier(provider.type().getDefaultMetadataIdentifier())
                .setType(provider.type())
                .setName(name)
                .setDescription(description)
                .setIconAssetKey(null)
                .setClientId(CLIENT_ID)
                .setClientSecretKey(null)
                .setAttributes(provider.attributes())
                .setDefaultScopes(List.of("email", "profile", "openid", provider.keycloakAlias()))
                .setAdditionalParams(List.of(new IdentityAdditionalParameter()
                        .setKey(KEYCLOAK_IDP_HINT_PARAMETER)
                        .setValue(keycloakIdpHint)))
                .setIsEnabled(false)
                .setIsTestProvider(testProvider)
                .setPkceMethod(PKCE_METHOD)
                .setUniqueIdAttribute(provider.uniqueIdAttribute());
    }
}
