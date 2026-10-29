package de.aivot.prosuna.backend.elements.models.elements;

import de.aivot.prosuna.backend.elements.enums.OptionsSourceType;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ChipInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DomainAndUserSelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.MultiCheckboxInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.MultiCheckboxInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessInstanceAttachmentSetSelectElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TableInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TableInputElementColumn;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementConfigurationRulesTest {
    private static final String ITEMS_ERROR = "Die Mindestanzahl darf nicht größer als die Maximalanzahl sein.";

    @Test
    void shouldReportMinimumItemsExceedingMaximumItems() {
        assertRangeError(required(new ChipInputElement().setMinItems(3).setMaxItems(2)), "minItems", "maxItems", ITEMS_ERROR);
        assertRangeError(required(new ProcessInstanceAttachmentSetSelectElement().setMinItems(3).setMaxItems(2)), "minItems", "maxItems", ITEMS_ERROR);
        assertRangeError(new DomainAndUserSelectInputElement().setMinItems(3).setMaxItems(2), "minItems", "maxItems", ITEMS_ERROR);
        assertRangeError(new AssignmentContextInputElement().setMinItems(3).setMaxItems(2), "minItems", "maxItems", ITEMS_ERROR);
        assertRangeError(required(new ReplicatingContainerLayoutElement().setMinimumRequiredSets(3).setMaximumSets(2)),
                "minimumRequiredSets", "maximumSets", "Sie fordern mehr Datensätze, als Sie maximal zulassen.");
    }

    @Test
    void shouldTreatNonPositiveMaximumAsNoLimit() {
        assertEquals(Map.of(), new DomainAndUserSelectInputElement().setMinItems(3).setMaxItems(0).validateConfiguration());
        assertEquals(Map.of(), new AssignmentContextInputElement().setMinItems(3).setMaxItems(0).validateConfiguration());
    }

    @Test
    void shouldIgnoreMinimumsTheEditorHidesForOptionalElements() {
        assertEquals(Map.of(), new ChipInputElement().setMinItems(3).setMaxItems(2).validateConfiguration());
        assertEquals(Map.of(), new ProcessInstanceAttachmentSetSelectElement().setMinItems(3).setMaxItems(2).validateConfiguration());
        assertEquals(Map.of(), new ReplicatingContainerLayoutElement().setMinimumRequiredSets(3).setMaximumSets(2).validateConfiguration());
        assertEquals(Map.of(), new TableInputElement().setFields(List.of(column("name"))).setMinimumRequiredRows(3).setMaximumRows(2).validateConfiguration());
        assertEquals(Map.of(), multiCheckbox(3, "a").validateConfiguration());
    }

    @Test
    void shouldReportFileUploadLimitsOnlyForMultipleFiles() {
        var single = fileUpload(List.of("pdf")).setMinFiles(3).setMaxFiles(2);
        var multiple = fileUpload(List.of("pdf")).setIsMultifile(true).setMinFiles(3).setMaxFiles(2);

        assertEquals(Map.of(), single.validateConfiguration());
        assertRangeError(multiple, "minFiles", "maxFiles", "Sie fordern mehr Anlagen, als Sie maximal zulassen.");
    }

    @Test
    void shouldRejectAnEmptyExtensionListButAllowAMissingOne() {
        var expected = Map.of("extensions", List.of("Wählen Sie mindestens eine erlaubte Dateiendung aus."));

        assertEquals(expected, fileUpload(List.of()).validateConfiguration());
        assertEquals(Map.of(), fileUpload(null).validateConfiguration());
    }

    @Test
    void shouldReportMoreRequiredOptionsThanDefined() {
        var tooMany = required(multiCheckbox(3, "a", "b"));
        var enough = required(multiCheckbox(2, "a", "b"));
        var codeList = required(multiCheckbox(3).setOptionsSource(OptionsSourceType.CodeList));

        assertEquals(
                Map.of("minimumRequiredOptions", List.of("Sie fordern mehr Optionen, als Sie definiert haben.")),
                tooMany.validateConfiguration()
        );
        assertEquals(Map.of(), enough.validateConfiguration());
        assertEquals(Map.of(), codeList.validateConfiguration());
    }

    @Test
    void shouldReportMissingAndRepeatedTableColumnKeys() {
        var table = new TableInputElement().setFields(List.of(
                column("name"),
                column(" "),
                column("name "),
                column("age")
        ));

        assertEquals(Map.of(
                "fields.1.key", List.of("Geben Sie einen Schlüssel für Spalte 2 ein."),
                "fields.2.key", List.of("Der Schlüssel von Spalte 3 wird bereits von einer anderen Spalte verwendet.")
        ), table.validateConfiguration());
    }

    @Test
    void shouldRequireASubmittedFileName() {
        var upload = fileUpload(List.of("pdf"));
        upload.setSubmittedFileName(" ");

        assertEquals(
                Map.of("submittedFileName", List.of("Geben Sie einen Dateinamen bei Einreichung an.")),
                upload.validateConfiguration()
        );
    }

    @Test
    void shouldRequireAPublicFormTitle() {
        var expected = Map.of("publicTitle", List.of("Geben Sie einen öffentlichen Titel für das Formular an."));
        var withTitle = new FormLayoutElement();
        withTitle.setPublicTitle("Anmeldung zur Hundesteuer");

        assertEquals(expected, new FormLayoutElement().validateConfiguration());
        assertEquals(Map.of(), withTitle.validateConfiguration());
    }

    @Test
    void shouldRequireAtLeastOneTableColumn() {
        var expected = Map.of("fields", List.of("Fügen Sie mindestens eine Spalte hinzu."));

        assertEquals(expected, new TableInputElement().validateConfiguration());
        assertEquals(expected, new TableInputElement().setFields(List.of()).validateConfiguration());
    }

    private static TableInputElementColumn column(String key) {
        return new TableInputElementColumn().setKey(key).setLabel("Spalte " + key);
    }

    private static void assertRangeError(BaseElement element, String minimumProperty, String maximumProperty, String message) {
        assertEquals(
                Map.of(minimumProperty, List.of(message), maximumProperty, List.of(message)),
                element.validateConfiguration(),
                element.getClass().getSimpleName()
        );
    }

    private static <T extends BaseInputElement<?>> T required(T element) {
        element.setRequired(true);
        return element;
    }

    private static FileUploadInputElement fileUpload(List<String> extensions) {
        var upload = new FileUploadInputElement().setExtensions(extensions);
        upload.setSubmittedFileName("Anlage");
        return upload;
    }

    private static MultiCheckboxInputElement multiCheckbox(Integer minimumRequiredOptions, String... values) {
        var options = Arrays.stream(values)
                .map(value -> new MultiCheckboxInputElementOption().setValue(value).setLabel(value))
                .toList();
        return new MultiCheckboxInputElement()
                .setOptions(options)
                .setMinimumRequiredOptions(minimumRequiredOptions);
    }
}
