package de.aivot.prosuna.backend.elements.models.elements.form.content;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class LinkButtonContentElementTest {
    @ParameterizedTest
    @ValueSource(strings = {"primary", "secondary", "success", "info", "warning", "error"})
    void preservesSupportedColors(String color) throws Exception {
        var mapper = JsonMapperFactory.getInstance();
        var element = new LinkButtonContentElement().setColor(color);
        var restored = assertInstanceOf(LinkButtonContentElement.class,
                mapper.readValue(mapper.writeValueAsString(element), BaseElement.class));
        assertEquals(color, restored.getResolvedColor());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown"})
    void defaultsMissingOrUnsupportedColorsToPrimary(String color) {
        assertEquals("primary", new LinkButtonContentElement().setColor(color).getResolvedColor());
    }

    @Test
    void preservesSelectedActionsWithEmptyEventNames() throws Exception {
        // The editor uses an empty event name for a selected action that has not been configured yet.
        for (boolean staff : new boolean[]{true, false}) {
            var element = new LinkButtonContentElement();
            if (staff) {
                element.setStaffTaskEvent("");
            } else {
                element.setCustomerTaskEvent("");
            }
            var mapper = JsonMapperFactory.getInstance();
            var restored = assertInstanceOf(LinkButtonContentElement.class,
                    mapper.readValue(mapper.writeValueAsString(element), BaseElement.class));
            assertEquals("", staff ? restored.getStaffTaskEvent() : restored.getCustomerTaskEvent());
            assertNull(staff ? restored.getCustomerTaskEvent() : restored.getStaffTaskEvent());
            assertNull(restored.getHref());
        }
    }

    @Test
    void shouldRoundTripThroughBaseElementSerialization() throws Exception {
        var serialized = JsonMapperFactory
                .getInstance()
                .writeValueAsString(new LinkButtonContentElement()
                        .setLabel("Weiterlesen")
                        .setIcon("info")
                        .setHref("https://example.org")
                        .setOpenInNewTab(false)
                        .setStaffTaskEvent("staff-event")
                        .setCustomerTaskEvent("customer-event")
                        .setVariant("outlined")
                        .setColor("secondary"));

        var deserialized = JsonMapperFactory
                .getInstance()
                .readValue(serialized, BaseElement.class);

        var linkButton = assertInstanceOf(LinkButtonContentElement.class, deserialized);
        assertEquals("Weiterlesen", linkButton.getLabel());
        assertEquals("info", linkButton.getIcon());
        assertEquals("https://example.org", linkButton.getHref());
        assertEquals(Boolean.FALSE, linkButton.getOpenInNewTab());
        assertEquals("staff-event", linkButton.getStaffTaskEvent());
        assertEquals("customer-event", linkButton.getCustomerTaskEvent());
        assertEquals("outlined", linkButton.getVariant());
        assertEquals("secondary", linkButton.getColor());
    }
}
