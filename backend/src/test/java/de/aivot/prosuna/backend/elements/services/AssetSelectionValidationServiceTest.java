package de.aivot.prosuna.backend.elements.services;

import de.aivot.prosuna.backend.asset.entities.AssetEntity;
import de.aivot.prosuna.backend.asset.repositories.AssetRepository;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssetSelectInputElement;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssetSelectionValidationServiceTest {
    private final AssetRepository repository = mock(AssetRepository.class);
    private final AssetSelectionValidationService service = new AssetSelectionValidationService(repository);

    @Test
    void validatesAgainstPersistedProviderAndRejectsUnknownAssets() {
        var key = UUID.randomUUID();
        var field = new AssetSelectInputElement().setStorageProviderId(7);
        when(repository.findById(key)).thenReturn(Optional.of(new AssetEntity().setKey(key).setStorageProviderId(7)));
        assertDoesNotThrow(() -> service.validate(field, key.toString()));
        field.setStorageProviderId(8);
        assertThrows(ValidationException.class, () -> service.validate(field, key.toString()));
        when(repository.findById(key)).thenReturn(Optional.empty());
        assertThrows(ValidationException.class, () -> service.validate(field, key.toString()));
    }

    @Test
    void preservesUnrestrictedAndEmptySelectionsWithoutLookup() {
        assertDoesNotThrow(() -> service.validate(new AssetSelectInputElement(), UUID.randomUUID().toString()));
        var field = new AssetSelectInputElement().setStorageProviderId(7);
        assertDoesNotThrow(() -> service.validate(field, null));
        assertDoesNotThrow(() -> service.validate(field, " "));
        verifyNoInteractions(repository);
    }
}
