package de.aivot.prosuna.backend.elements.models.elements.form.content;

import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LinkButtonContentElementTest {
    @Test
    void shouldRoundTripThroughBaseElementSerialization() throws Exception {
        var serialized = ObjectMapperFactory
                .getInstance()
                .writeValueAsString(new LinkButtonContentElement()
                        .setLabel("Weiterlesen")
                        .setHref("https://example.org")
                        .setOpenInNewTab(false)
                        .setStaffTaskEvent("staff-event")
                        .setCustomerTaskEvent("customer-event")
                        .setVariant("outlined")
                        .setColor("secondary"));

        var deserialized = ObjectMapperFactory
                .getInstance()
                .readValue(serialized, BaseElement.class);

        var linkButton = assertInstanceOf(LinkButtonContentElement.class, deserialized);
        assertEquals("Weiterlesen", linkButton.getLabel());
        assertEquals("https://example.org", linkButton.getHref());
        assertEquals(Boolean.FALSE, linkButton.getOpenInNewTab());
        assertEquals("staff-event", linkButton.getStaffTaskEvent());
        assertEquals("customer-event", linkButton.getCustomerTaskEvent());
        assertEquals("outlined", linkButton.getVariant());
        assertEquals("secondary", linkButton.getColor());
    }
}
