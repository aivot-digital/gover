package de.aivot.GoverBackend.identity.converters;

import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IdentityProviderLinksConverterTest {

    @Test
    void testConvertToDatabaseColumn() {
        var converter = new IdentityProviderLinksConverter();

        IdentityProviderLink link1 = new IdentityProviderLink()
                .setIdentityProviderKey("provider1")
                .setAdditionalScopes(List.of("scope1", "scope2"));
        IdentityProviderLink link2 = new IdentityProviderLink()
                .setIdentityProviderKey("provider2")
                .setAdditionalScopes(List.of("scope3"));
        List<IdentityProviderLink> links = List.of(link1, link2);

        String json = converter.convertToDatabaseColumn(links);
        assertNotNull(json);
        assertTrue(json.contains("provider1"));
        assertTrue(json.contains("scope1"));
        assertTrue(json.contains("provider2"));
        assertTrue(json.contains("scope3"));

        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void testConvertToEntityAttribute() {
        var converter = new IdentityProviderLinksConverter();

        String json = "[{\"identityProviderKey\":\"provider1\",\"additionalScopes\":[\"scope1\",\"scope2\"]}," +
                      "{\"identityProviderKey\":\"provider2\",\"additionalScopes\":[\"scope3\"]}]";

        List<IdentityProviderLink> links = converter.convertToEntityAttribute(json);
        assertNotNull(links);
        assertEquals(2, links.size());
        assertEquals("provider1", links.get(0).getIdentityProviderKey());
        assertEquals(List.of("scope1", "scope2"), links.get(0).getAdditionalScopes());
        assertEquals("provider2", links.get(1).getIdentityProviderKey());
        assertEquals(List.of("scope3"), links.get(1).getAdditionalScopes());

        assertNull(converter.convertToEntityAttribute(null));

        String invalidJson = "invalid-json";
        assertThrows(RuntimeException.class, () -> converter.convertToEntityAttribute(invalidJson));
    }
}