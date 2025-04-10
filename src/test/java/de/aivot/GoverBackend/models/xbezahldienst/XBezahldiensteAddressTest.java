package de.aivot.GoverBackend.models.xbezahldienst;

import de.aivot.GoverBackend.payment.models.XBezahldiensteAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XBezahldiensteAddressTest {

    @Test
    /*
     * Test method for {@link de.aivot.GoverBackend.models.xbezahldienst.Address#setStreet()}.
     * Test if the street is set correctly with null, not allowed characters, and a string that is too long.
     */
    void setStreet() {
        XBezahldiensteAddress address = new XBezahldiensteAddress();

        address.setStreet(null);
        assertNull(address.getStreet());

        address.setStreet("Test123");
        assertEquals("Test123", address.getStreet());

        address.setStreet("Test123#");
        assertEquals("Test123", address.getStreet());

        address.setStreet("Test".repeat(250));
        assertEquals(250, address.getStreet().length());
    }

    @Test
    void setHouseNumber() {
    }

    @Test
    void setAddressLine() {
    }

    @Test
    void setPostalCode() {
    }

    @Test
    void setCity() {
    }

    @Test
    void setCountry() {
    }
}