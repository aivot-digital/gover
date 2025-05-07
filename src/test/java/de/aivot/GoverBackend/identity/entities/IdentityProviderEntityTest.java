package de.aivot.GoverBackend.identity.entities;

import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.identity.models.IdentityAdditionalParameter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IdentityProviderEntityTest {

    @Test
    void testEquals() {
        IdentityProviderEntity entity1 = new IdentityProviderEntity()
                .setKey("key1")
                .setMetadataIdentifier("meta1")
                .setType(IdentityProviderType.BayernId)
                .setName("Provider1")
                .setDescription("Description1")
                .setAuthorizationEndpoint("auth1")
                .setTokenEndpoint("token1")
                .setUserinfoEndpoint("userinfo1")
                .setEndSessionEndpoint("end1")
                .setClientId("client1")
                .setDefaultScopes(List.of("scope1", "scope2"))
                .setAdditionalParams(List.of(new IdentityAdditionalParameter().setKey("param1").setValue("value1")))
                .setIsEnabled(true)
                .setIsTestProvider(false);

        IdentityProviderEntity entity2 = new IdentityProviderEntity()
                .setKey("key1")
                .setMetadataIdentifier("meta1")
                .setType(IdentityProviderType.BayernId)
                .setName("Provider1")
                .setDescription("Description1")
                .setAuthorizationEndpoint("auth1")
                .setTokenEndpoint("token1")
                .setUserinfoEndpoint("userinfo1")
                .setEndSessionEndpoint("end1")
                .setClientId("client1")
                .setDefaultScopes(List.of("scope1", "scope2"))
                .setAdditionalParams(List.of(new IdentityAdditionalParameter().setKey("param1").setValue("value1")))
                .setIsEnabled(true)
                .setIsTestProvider(false);

        IdentityProviderEntity entity3 = new IdentityProviderEntity()
                .setKey("key2")
                .setMetadataIdentifier("meta2")
                .setType(IdentityProviderType.BundId)
                .setName("Provider2")
                .setDescription("Description2")
                .setAuthorizationEndpoint("auth2")
                .setTokenEndpoint("token2")
                .setUserinfoEndpoint("userinfo2")
                .setEndSessionEndpoint("end2")
                .setClientId("client2")
                .setDefaultScopes(List.of("scope3"))
                .setAdditionalParams(List.of(new IdentityAdditionalParameter().setKey("param2").setValue("value2")))
                .setIsEnabled(false)
                .setIsTestProvider(true);

        assertEquals(entity1, entity2); // Same attributes
        assertNotEquals(entity1, entity3); // Different attributes
    }

    @Test
    void testHashCode() {
        IdentityProviderEntity entity1 = new IdentityProviderEntity()
                .setKey("key1")
                .setMetadataIdentifier("meta1")
                .setType(IdentityProviderType.BayernId)
                .setName("Provider1")
                .setDescription("Description1")
                .setAuthorizationEndpoint("auth1")
                .setTokenEndpoint("token1")
                .setUserinfoEndpoint("userinfo1")
                .setEndSessionEndpoint("end1")
                .setClientId("client1")
                .setDefaultScopes(List.of("scope1", "scope2"))
                .setAdditionalParams(List.of(new IdentityAdditionalParameter().setKey("param1").setValue("value1")))
                .setIsEnabled(true)
                .setIsTestProvider(false);

        IdentityProviderEntity entity2 = new IdentityProviderEntity()
                .setKey("key1")
                .setMetadataIdentifier("meta1")
                .setType(IdentityProviderType.BayernId)
                .setName("Provider1")
                .setDescription("Description1")
                .setAuthorizationEndpoint("auth1")
                .setTokenEndpoint("token1")
                .setUserinfoEndpoint("userinfo1")
                .setEndSessionEndpoint("end1")
                .setClientId("client1")
                .setDefaultScopes(List.of("scope1", "scope2"))
                .setAdditionalParams(List.of(new IdentityAdditionalParameter().setKey("param1").setValue("value1")))
                .setIsEnabled(true)
                .setIsTestProvider(false);

        IdentityProviderEntity entity3 = new IdentityProviderEntity()
                .setKey("key2")
                .setMetadataIdentifier("meta2")
                .setType(IdentityProviderType.BundId)
                .setName("Provider2")
                .setDescription("Description2")
                .setAuthorizationEndpoint("auth2")
                .setTokenEndpoint("token2")
                .setUserinfoEndpoint("userinfo2")
                .setEndSessionEndpoint("end2")
                .setClientId("client2")
                .setDefaultScopes(List.of("scope3"))
                .setAdditionalParams(List.of(new IdentityAdditionalParameter().setKey("param2").setValue("value2")))
                .setIsEnabled(false)
                .setIsTestProvider(true);

        assertEquals(entity1.hashCode(), entity2.hashCode()); // Same attributes
        assertNotEquals(entity1.hashCode(), entity3.hashCode()); // Different attributes
    }
}