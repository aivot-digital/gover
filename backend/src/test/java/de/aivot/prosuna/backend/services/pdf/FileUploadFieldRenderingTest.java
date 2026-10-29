package de.aivot.prosuna.backend.services.pdf;

import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElementItem;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.pdf.enums.FormPdfScope;
import de.aivot.prosuna.backend.pdf.models.FormPdfContext;
import de.aivot.prosuna.backend.services.TemplateLoaderService;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadFieldRenderingTest {
    @Test
    void customerPrintShowsBothNamesAndEscapesUploadedNames() {
        var html = render(FormPdfScope.Customer, List.of(
                file("Nachweis.pdf", "Mein Nachweis <2026> & Kopie.pdf"),
                file("Weitere Anlage.pdf", "Scan.png")));

        assertTrue(html.contains("<span>Nachweis.pdf</span>"));
        assertTrue(html.contains("Hochgeladen als: Mein Nachweis &lt;2026&gt; &amp; Kopie.pdf"));
        assertTrue(html.contains("<span>Weitere Anlage.pdf</span>"));
        assertTrue(html.contains("Hochgeladen als: Scan.png"));
        assertEquals(2, html.split("class=\"original-file-name\"", -1).length - 1);
        assertFalse(html.contains("<2026>"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Nachweis.pdf", "   "})
    void customerPrintOmitsRedundantOrMissingOriginalNames(@Nullable String originalName) {
        var html = render(FormPdfScope.Customer, List.of(file("Nachweis.pdf", originalName)));

        assertTrue(html.contains("<span>Nachweis.pdf</span>"));
        assertFalse(html.contains("Hochgeladen als:"));
    }

    @Test
    void staffPrintKeepsTheAssignedName() {
        var html = render(FormPdfScope.Staff, List.of(file("Nachweis.pdf", "Scan.png")));

        assertTrue(html.contains("<span>Nachweis.pdf</span>"));
        assertFalse(html.contains("Scan.png"));
        assertFalse(html.contains("Hochgeladen als:"));
    }

    @Test
    void blankPrintShowsTheUploadInstruction() {
        var html = render(FormPdfScope.Blank, null);

        assertTrue(html.contains("Die Anlage ist angehangen"));
        assertFalse(html.contains("Keine Anlagen hinzugefügt"));
        assertFalse(html.contains("Hochgeladen als:"));
    }

    @Test
    void customerPrintWithoutFilesShowsTheEmptyState() {
        assertTrue(render(FormPdfScope.Customer, List.of()).contains("Keine Anlagen hinzugefügt"));
        assertTrue(render(FormPdfScope.Customer, null).contains("Keine Anlagen hinzugefügt"));
    }

    private FileUploadInputElementItem file(String name, @Nullable String originalName) {
        return new FileUploadInputElementItem().setName(name).setOriginalFileName(originalName);
    }

    private String render(FormPdfScope scope, @Nullable List<FileUploadInputElementItem> files) {
        var field = new FileUploadInputElement();
        field.setId("attachments");
        field.setLabel("Nachweise");
        field.setRequired(true);
        return new TemplateLoaderService().processTemplate("form-parts/element-dispatcher.html", Map.of(
                "base", new FormPdfContext("Prosuna", null, new ProsunaConfig(), scope),
                "child", new PdfElement(field, files, List.of())
        ), TemplateMode.HTML);
    }
}
