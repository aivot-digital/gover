package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.enums.AssetVisibility;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetSelectInputElementTest {
    @Test
    void formatAndValidationUseCanonicalAssetKeys() {
        var element = new AssetSelectInputElement();
        var assetKey = UUID.randomUUID().toString();

        assertEquals(assetKey, element.formatValue("  " + assetKey + "  "));
        assertDoesNotThrow(() -> element.validate(assetKey));
        assertThrows(ValidationException.class, () -> element.validate("not-an-asset-key"));
        assertThrows(ValidationException.class, () -> element.validate("1-1-1-1-1"));
    }

    @Test
    void requiredSelectionRejectsEmptyValues() {
        var element = new AssetSelectInputElement();
        element.setRequired(true);

        assertThrows(ValidationException.class, () -> element.validate("  "));
        assertDoesNotThrow(() -> element.validate(UUID.randomUUID().toString()));
    }

    @Test
    void evaluationUsesScalarAssetKeys() {
        var element = new AssetSelectInputElement();
        var assetKey = UUID.randomUUID().toString();

        assertTrue(element.evaluate(ConditionOperator.Equals, assetKey, "  " + assetKey + " "));
        assertFalse(element.evaluate(ConditionOperator.Empty, assetKey, null));
        assertTrue(element.evaluate(ConditionOperator.NotEmpty, assetKey, null));
    }

    @Test
    void shouldRoundTripSelectionSettingsThroughBaseElementSerialization() throws Exception {
        var element = new AssetSelectInputElement()
                .setPlaceholder("Zertifikat auswählen")
                .setDialogTitle("Client-Zertifikat auswählen")
                .setAllowedMimeTypes(List.of("application/x-pem-file"))
                .setAssetVisibility(AssetVisibility.Private);

        var serialized = JsonMapperFactory.getInstance().writeValueAsString(element);
        var deserialized = JsonMapperFactory.getInstance().readValue(serialized, BaseElement.class);
        var selector = assertInstanceOf(AssetSelectInputElement.class, deserialized);

        assertEquals("Zertifikat auswählen", selector.getPlaceholder());
        assertEquals("Client-Zertifikat auswählen", selector.getDialogTitle());
        assertEquals(List.of("application/x-pem-file"), selector.getAllowedMimeTypes());
        assertEquals(AssetVisibility.Private, selector.getAssetVisibility());
    }
}
