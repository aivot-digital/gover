package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.exceptions.RequiredValidationException;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoragePathSelectorInputElementTest {
    @Test
    void formatValueShouldMapObjectAndTrimPath() {
        var element = new StoragePathSelectorInputElement();

        var value = element.formatValue(Map.of(
                "storageProviderId", 77,
                "path", " /Antraege/2026/ "
        ));

        assertEquals(77, value.getStorageProviderId());
        assertEquals("/Antraege/2026/", value.getPath());
    }

    @Test
    void validateShouldRequireCompleteValueWhenRequired() {
        var element = new StoragePathSelectorInputElement();
        element.setRequired(true);

        assertThrows(RequiredValidationException.class, () -> element.validate(Map.of()));
        assertDoesNotThrow(() -> element.validate(Map.of(
                "storageProviderId", 77,
                "path", "/Antraege/2026/"
        )));
    }

    @Test
    void validateShouldRejectIncompleteValue() {
        var element = new StoragePathSelectorInputElement();

        assertThrows(ValidationException.class, () -> element.validate(Map.of(
                "storageProviderId", 77
        )));
        assertThrows(ValidationException.class, () -> element.validate(Map.of(
                "path", "/Antraege/2026/"
        )));
    }

    @Test
    void evaluateShouldCheckFilledStateAndEquality() {
        var element = new StoragePathSelectorInputElement();
        var value = new StoragePathSelectorInputElementValue()
                .setStorageProviderId(77)
                .setPath("/Antraege/2026/");

        assertTrue(element.evaluate(ConditionOperator.NotEmpty, value, null));
        assertFalse(element.evaluate(ConditionOperator.Empty, value, null));
        assertTrue(element.evaluate(ConditionOperator.Equals, value, Map.of(
                "storageProviderId", 77,
                "path", "/Antraege/2026/"
        )));
    }

    @Test
    void shouldRoundTripThroughBaseElementSerialization() throws Exception {
        var element = new StoragePathSelectorInputElement()
                .setPlaceholder("Ordner auswählen")
                .setStorageProviderSelectHint("Speicheranbieter auswählen")
                .setAllowedStorageProviderTypes(List.of(StorageProviderType.External, StorageProviderType.Assets))
                .setAllowReadOnlyStorageProviders(true);

        var serialized = JsonMapperFactory
                .getInstance()
                .writeValueAsString(element);

        var deserialized = JsonMapperFactory
                .getInstance()
                .readValue(serialized, BaseElement.class);

        var selector = assertInstanceOf(StoragePathSelectorInputElement.class, deserialized);
        assertEquals("Ordner auswählen", selector.getPlaceholder());
        assertEquals("Speicheranbieter auswählen", selector.getStorageProviderSelectHint());
        assertEquals(List.of(StorageProviderType.External, StorageProviderType.Assets), selector.getAllowedStorageProviderTypes());
        assertTrue(selector.getAllowReadOnlyStorageProviders());
    }
}
