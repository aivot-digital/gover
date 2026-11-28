package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUploadInputElementTest {
    @Test
    void acceptsFilesLargerThanTenMegabytesBecauseUploadsAreLimitedByTheStorageProvider() {
        var element = new FileUploadInputElement().setExtensions(List.of("pdf"));

        assertDoesNotThrow(() -> element.performValidation(List.of(file(50 * 1024 * 1024))));
    }

    @Test
    void stillRejectsFilesWithoutValidSize() {
        var element = new FileUploadInputElement().setExtensions(List.of("pdf"));

        assertThrows(ValidationException.class, () -> element.performValidation(List.of(file(0))));
    }

    private static FileUploadInputElementItem file(int size) {
        return new FileUploadInputElementItem()
                .setName("nachweis.pdf")
                .setUri("process-instance-attachment://1")
                .setSize(size);
    }
}
