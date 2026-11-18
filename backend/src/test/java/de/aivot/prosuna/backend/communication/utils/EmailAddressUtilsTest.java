package de.aivot.prosuna.backend.communication.utils;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailAddressUtilsTest {
    @Test void sharesTheFrontendAddressContract() throws Exception {
        try (var stream = getClass().getResourceAsStream("/email-address-cases.json")) {
            var cases = JsonMapperFactory.getInstance().readTree(stream);
            for (var item : cases) {
                var address = item.get("value").asText();
                var policy = item.get("allowLocalDomains").asBoolean() ? EmailAddressUtils.DomainPolicy.ALLOW_LOCAL : EmailAddressUtils.DomainPolicy.INTERNET;
                if (item.get("normalized").isNull()) {
                    assertFalse(EmailAddressUtils.isValidSingleAddress(address, policy), address);
                } else {
                    assertEquals(item.get("normalized").asText(), EmailAddressUtils.normalizeSingleAddress(address, policy), address);
                }
            }
        }
    }
    @Test void keepsLegacyInputsStrictAndPermitsLocalTransport() {
        assertFalse(EmailAddressUtils.isValidSingleAddress("person@verwaltung"));
        assertEquals("person@verwaltung", EmailAddressUtils.normalizeTransportAddress(" person@verwaltung "));
        assertEquals("Person@xn--fa-hia.de", EmailAddressUtils.normalizeTransportAddress("Person@faß.de"));
        assertEquals("\"ab\"@example.org", EmailAddressUtils.normalizeTransportAddress("\"ab\"@example.org"));
        assertThrows(IllegalArgumentException.class, () -> EmailAddressUtils.normalizeTransportAddress("a@example.org\r\nBcc: b@example.org"));
        assertThrows(IllegalArgumentException.class, () -> EmailAddressUtils.normalizeTransportAddress("Name <a@example.org>"));
    }
}
