package de.aivot.GoverBackend.identity.converters;

import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IdentityProviderLinksConverterTest {

    @Test
    void testConvertToDatabaseColumn() {
        var converter = new IdentityProviderLinksConverter();

        UUID provider1Key = UUID.randomUUID();
        UUID provider2Key = UUID.randomUUID();

        IdentityProviderLink link1 = new IdentityProviderLink()
                .setIdentityProviderKey(provider1Key)
                .setAdditionalScopes(List.of("scope1", "scope2"));
        IdentityProviderLink link2 = new IdentityProviderLink()
                .setIdentityProviderKey(provider2Key)
                .setAdditionalScopes(List.of("scope3"));
        List<IdentityProviderLink> links = List.of(link1, link2);

        String json = converter.convertToDatabaseColumn(links);
        assertNotNull(json);
        assertTrue(json.contains(provider1Key.toString()));
        assertTrue(json.contains("scope1"));
        assertTrue(json.contains(provider2Key.toString()));
        assertTrue(json.contains("scope3"));

        assertEquals("[]", converter.convertToDatabaseColumn(null));
    }

    @Test
    void testConvertToEntityAttribute() {
        var key1 = UUID.randomUUID();
        var key2 = UUID.randomUUID();

        var converter = new IdentityProviderLinksConverter();

        String json = "[{\"identityProviderKey\":\"" + key1 + "\",\"additionalScopes\":[\"scope1\",\"scope2\"]}," +
                      "{\"identityProviderKey\":\"" + key2 + "\",\"additionalScopes\":[\"scope3\"]}]";

        List<IdentityProviderLink> links = converter.convertToEntityAttribute(json);
        assertNotNull(links);
        assertEquals(2, links.size());
        assertEquals(key1, links.get(0).getIdentityProviderKey());
        assertEquals(List.of("scope1", "scope2"), links.get(0).getAdditionalScopes());
        assertEquals(key2, links.get(1).getIdentityProviderKey());
        assertEquals(List.of("scope3"), links.get(1).getAdditionalScopes());

        assertEquals(List.of(), converter.convertToEntityAttribute(null));

        String invalidJson = "invalid-json";
        assertThrows(RuntimeException.class, () -> converter.convertToEntityAttribute(invalidJson));
    }
}