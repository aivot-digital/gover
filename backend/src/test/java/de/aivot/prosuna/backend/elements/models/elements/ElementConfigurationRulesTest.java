package de.aivot.prosuna.backend.elements.models.elements;

import de.aivot.prosuna.backend.elements.enums.OptionsSourceType;
import de.aivot.prosuna.backend.elements.enums.OverrideFunctionType;
import de.aivot.prosuna.backend.elements.models.elements.form.content.AlertContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.ImageContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.LinkButtonContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.ProcessAttachmentDisplayContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ChipInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DataObjectSelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DomainAndUserSelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DomainAndUserSelectProcessAccessConstraint;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.MapPointInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.MultiCheckboxInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.MultiCheckboxInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.NumberInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessInstanceAttachmentSetSelectElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.StoragePathSelectorInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TableInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TableInputElementColumn;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.IntroductionStepElement;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementConfigurationRulesTest {
    private static final String ITEMS_ERROR = "Die Mindestanzahl darf nicht größer als die Maximalanzahl sein.";

    @Test
    void shouldReportMinimumItemsExceedingMaximumItems() {
        assertRangeError(required(labelled(new ChipInputElement()).setMinItems(3).setMaxItems(2)), "minItems", "maxItems", ITEMS_ERROR);
        assertRangeError(required(labelled(new ProcessInstanceAttachmentSetSelectElement()).setMinItems(3).setMaxItems(2)), "minItems", "maxItems", ITEMS_ERROR);
        assertRangeError(labelled(new DomainAndUserSelectInputElement()).setMinItems(3).setMaxItems(2), "minItems", "maxItems", ITEMS_ERROR);
        assertRangeError(labelled(new AssignmentContextInputElement()).setMinItems(3).setMaxItems(2), "minItems", "maxItems", ITEMS_ERROR);
        assertRangeError(required(labelled(new ReplicatingContainerLayoutElement()).setMinimumRequiredSets(3).setMaximumSets(2)),
                "minimumRequiredSets", "maximumSets", "Sie fordern mehr Datensätze, als Sie maximal zulassen.");
    }

    @Test
    void shouldTreatNonPositiveMaximumAsNoLimit() {
        assertEquals(Map.of(), labelled(new DomainAndUserSelectInputElement()).setMinItems(3).setMaxItems(0).validateConfiguration());
        assertEquals(Map.of(), labelled(new AssignmentContextInputElement()).setMinItems(3).setMaxItems(0).validateConfiguration());
    }

    @Test
    void shouldIgnoreMinimumsTheEditorHidesForOptionalElements() {
        assertEquals(Map.of(), labelled(new ChipInputElement()).setMinItems(3).setMaxItems(2).validateConfiguration());
        assertEquals(Map.of(), labelled(new ProcessInstanceAttachmentSetSelectElement()).setMinItems(3).setMaxItems(2).validateConfiguration());
        assertEquals(Map.of(), labelled(new ReplicatingContainerLayoutElement()).setMinimumRequiredSets(3).setMaximumSets(2).validateConfiguration());
        assertEquals(Map.of(), labelled(new TableInputElement()).setFields(List.of(column("name"))).setMinimumRequiredRows(3).setMaximumRows(2).validateConfiguration());
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
    void shouldRequireAllowedExtensions() {
        var expected = Map.of("extensions", List.of("Wählen Sie mindestens eine erlaubte Dateiendung aus."));

        assertEquals(expected, fileUpload(List.of()).validateConfiguration());
        assertEquals(expected, fileUpload(null).validateConfiguration());
    }

    @Test
    void shouldReportMoreRequiredOptionsThanDefined() {
        var tooMany = required(multiCheckbox(3, "a", "b"));
        var enough = required(multiCheckbox(2, "a", "b"));
        var codeList = required(multiCheckbox(3).setOptionsSource(OptionsSourceType.CodeList).setCodeListKey("staaten"));

        assertEquals(
                Map.of("minimumRequiredOptions", List.of("Sie fordern mehr Optionen, als Sie definiert haben.")),
                tooMany.validateConfiguration()
        );
        assertEquals(Map.of(), enough.validateConfiguration());
        assertEquals(Map.of(), codeList.validateConfiguration());
    }

    @Test
    void shouldReportMissingAndRepeatedTableColumnKeys() {
        var table = labelled(new TableInputElement()).setFields(List.of(
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

        assertEquals(expected, labelled(new TableInputElement()).validateConfiguration());
        assertEquals(expected, labelled(new TableInputElement()).setFields(List.of()).validateConfiguration());
    }

    @Test
    void shouldReportTableMaximumsThatRejectEveryRow() {
        var expected = Map.of("maximumRows", List.of("Lassen Sie mindestens eine Zeile zu oder entfernen Sie die Maximalanzahl der Zeilen."));

        assertEquals(expected, required(labelled(new TableInputElement()).setFields(List.of(column("name"))).setMinimumRequiredRows(3).setMaximumRows(0)).validateConfiguration());
        assertEquals(expected, labelled(new TableInputElement()).setFields(List.of(column("name"))).setMaximumRows(-1).validateConfiguration());
        assertEquals(Map.of(), labelled(new TableInputElement()).setFields(List.of(column("name"))).setMaximumRows(1).validateConfiguration());
    }

    @Test
    void shouldRequireALabelForVisibleInputs() {
        var expected = Map.of("label", List.of("Geben Sie einen Titel für das Feld an."));
        var hidden = new TextInputElement();
        hidden.setTechnical(true);
        var overridden = new TextInputElement();
        overridden.setOverride(new ElementOverrideFunctions().setType(OverrideFunctionType.Javascript));

        assertEquals(expected, new TextInputElement().validateConfiguration());
        assertEquals(expected, labelled(new CheckboxInputElement()).setLabel(" ").validateConfiguration());
        assertEquals(Map.of(), hidden.validateConfiguration());
        assertEquals(Map.of(), overridden.validateConfiguration());
        assertEquals(Map.of(), labelled(new TextInputElement()).validateConfiguration());
    }

    @Test
    void shouldReportUnusableManualOptions() {
        assertEquals(Map.of("options", List.of("Fügen Sie mindestens eine Option hinzu.")),
                labelled(new SelectInputElement()).validateConfiguration());
        assertEquals(Map.of("options", List.of("Fügen Sie mindestens eine Option hinzu.")),
                labelled(new RadioInputElement()).setOptions(List.of()).validateConfiguration());
        assertEquals(Map.of("options", List.of(
                        "Geben Sie für jede Option eine Beschriftung und einen Wert an.",
                        "Die Werte der Optionen müssen eindeutig sein.",
                        "Die Beschriftungen der Optionen müssen eindeutig sein."
                )),
                labelled(new SelectInputElement()).setOptions(List.of(
                        SelectInputElementOption.of("a", "A"),
                        SelectInputElementOption.of("a ", "A"),
                        SelectInputElementOption.of(" ", "B")
                )).validateConfiguration());
        assertEquals(Map.of("options", List.of("Geben Sie für jede Option eine Beschriftung und einen Wert an.")),
                multiCheckbox(null, "a", "").validateConfiguration());
        assertEquals(Map.of(), labelled(new RadioInputElement())
                .setOptions(List.of(RadioInputElementOption.of("a", "A"), RadioInputElementOption.of("b", "B")))
                .validateConfiguration());
    }

    @Test
    void shouldRequireACodeListInsteadOfOptionsForCodeListSources() {
        var expected = Map.of("codeListKey", List.of("Wählen Sie eine Codeliste aus."));

        assertEquals(expected, labelled(new SelectInputElement()).setOptionsSource(OptionsSourceType.CodeList).validateConfiguration());
        assertEquals(expected, labelled(new RadioInputElement()).setOptionsSource(OptionsSourceType.CodeList).validateConfiguration());
        assertEquals(expected, multiCheckbox(null).setOptionsSource(OptionsSourceType.CodeList).validateConfiguration());
        assertEquals(expected, labelled(new ChipInputElement()).setOptionsSource(OptionsSourceType.CodeList).validateConfiguration());
        assertEquals(Map.of(), labelled(new SelectInputElement()).setOptionsSource(OptionsSourceType.CodeList).setCodeListKey("staaten").validateConfiguration());
        // Chip suggestions are optional.
        assertEquals(Map.of(), labelled(new ChipInputElement()).validateConfiguration());
    }

    @Test
    void shouldRequireTheContentOfContentElements() {
        assertEquals(Map.of("text", List.of("Geben Sie einen Titel oder einen Text für den Hinweis an.")),
                new AlertContentElement().setText("<p></p>").validateConfiguration());
        assertEquals(Map.of(), new AlertContentElement().setTitle("Hinweis").validateConfiguration());
        assertEquals(Map.of("content", List.of("Geben Sie eine Überschrift ein.")),
                new HeadlineContentElement().setContent(" ").validateConfiguration());
        assertEquals(Map.of("content", List.of("Geben Sie einen Text ein.")),
                new RichTextContentElement().setContent("<p>&nbsp;</p>").validateConfiguration());
        assertEquals(Map.of(), new RichTextContentElement().setContent("<p>Text</p>").validateConfiguration());
        assertEquals(Map.of("src", List.of("Geben Sie die URL des Bildes an oder wählen Sie ein Bild aus.")),
                new ImageContentElement().validateConfiguration());
        assertEquals(Map.of("attachmentSetKey", List.of("Wählen Sie den Anlagensatz aus, dessen Anlagen angezeigt werden sollen.")),
                new ProcessAttachmentDisplayContentElement().validateConfiguration());
    }

    @Test
    void shouldNotRequireContentThatAnOverrideCanProvide() {
        var headline = new HeadlineContentElement();
        headline.setOverride(new ElementOverrideFunctions().setType(OverrideFunctionType.NoCode));

        assertEquals(Map.of(), headline.validateConfiguration());
    }

    @Test
    void shouldRequireATargetForLinkButtons() {
        assertEquals(Map.of("href", List.of("Geben Sie den Link an, den die Schaltfläche öffnen soll.")),
                new LinkButtonContentElement().validateConfiguration());
        assertEquals(Map.of(), new LinkButtonContentElement().setHref("https://example.org").validateConfiguration());
        assertEquals(Map.of(), new LinkButtonContentElement().setStaffTaskEvent("approve").validateConfiguration());
        assertEquals(Map.of(), new LinkButtonContentElement().setCustomerTaskEvent("upload").validateConfiguration());
    }

    @Test
    void shouldReportSettingsThatRejectEveryValue() {
        assertEquals(Map.of("dataModelKey", List.of("Wählen Sie ein Datenmodell aus.")),
                labelled(new DataObjectSelectInputElement()).validateConfiguration());
        assertEquals(Map.of("decimalPlaces", List.of("Die Anzahl der Dezimalstellen darf nicht negativ sein.")),
                labelled(new NumberInputElement()).setDecimalPlaces(-1).validateConfiguration());
        assertEquals(Map.of(), labelled(new NumberInputElement()).setDecimalPlaces(0).validateConfiguration());
        assertEquals(Map.of("allowedStorageProviderTypes", List.of("Wählen Sie mindestens einen Speicheranbieter-Typ aus.")),
                labelled(new StoragePathSelectorInputElement()).setAllowedStorageProviderTypes(List.of()).validateConfiguration());
        assertEquals(Map.of(), labelled(new StoragePathSelectorInputElement()).validateConfiguration());
        assertEquals(Map.of("allowedTypes", List.of("Wählen Sie mindestens einen auswählbaren Typ aus.")),
                labelled(new DomainAndUserSelectInputElement()).setAllowedTypes(List.of()).validateConfiguration());
    }

    @Test
    void shouldReportIncompleteProcessAccessConstraints() {
        var expected = Map.of(
                "processAccessConstraint.processId", List.of("Geben Sie eine Prozess-ID größer als 0 an."),
                "processAccessConstraint.processVersion", List.of("Geben Sie eine Prozessversion größer als 0 an.")
        );
        var incomplete = new DomainAndUserSelectProcessAccessConstraint().setProcessVersion(0);
        var complete = new DomainAndUserSelectProcessAccessConstraint().setProcessId(1).setProcessVersion(2);

        assertEquals(expected, labelled(new DomainAndUserSelectInputElement()).setProcessAccessConstraint(incomplete).validateConfiguration());
        assertEquals(expected, labelled(new AssignmentContextInputElement()).setProcessAccessConstraint(incomplete).validateConfiguration());
        assertEquals(Map.of(), labelled(new DomainAndUserSelectInputElement()).setProcessAccessConstraint(complete).validateConfiguration());
    }

    @Test
    void shouldReportMapCentersOutsideTheMap() {
        assertEquals(Map.of(
                "centerLatitude", List.of("Der Breitengrad muss zwischen -90 und 90 liegen."),
                "centerLongitude", List.of("Der Längengrad muss zwischen -180 und 180 liegen."),
                "zoom", List.of("Die Zoomstufe muss zwischen 1 und 19 liegen.")
        ), labelled(new MapPointInputElement()).setCenterLatitude(91.0).setCenterLongitude(-181.0).setZoom(20).validateConfiguration());
        assertEquals(Map.of(), labelled(new MapPointInputElement()).setCenterLatitude(-90.0).setCenterLongitude(180.0).setZoom(1).validateConfiguration());
    }

    @Test
    void shouldRequireTableColumnLabels() {
        var table = labelled(new TableInputElement()).setFields(List.of(
                column("name"),
                new TableInputElementColumn().setKey("age").setLabel(" ")
        ));

        assertEquals(Map.of("fields.1.label", List.of("Geben Sie einen Titel für Spalte 2 ein.")), table.validateConfiguration());
    }

    @Test
    void shouldRequireStepTitles() {
        var step = new GenericStepElement();
        var titled = new GenericStepElement();
        titled.setTitle("Angaben zur Person");

        assertEquals(Map.of("title", List.of("Geben Sie einen Titel für den Abschnitt ein.")), step.validateConfiguration());
        assertEquals(Map.of(), titled.validateConfiguration());
    }

    @Test
    void shouldRequireAnAlternativeTextForTheIntroductionLogo() {
        var withLogo = new IntroductionStepElement();
        withLogo.setInitiativeLogoLink("https://example.org/logo.png");

        assertEquals(Map.of("initiativeName", List.of("Geben Sie einen Alternativtext für das ausgewählte Logo an.")), withLogo.validateConfiguration());
        assertEquals(Map.of(), new IntroductionStepElement().validateConfiguration());
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

    // Every visible input needs a label, which is not what most of these tests are about.
    private static <T extends BaseInputElement<?>> T labelled(T element) {
        element.setLabel("Feld");
        return element;
    }

    private static <T extends BaseInputElement<?>> T required(T element) {
        element.setRequired(true);
        return element;
    }

    private static FileUploadInputElement fileUpload(List<String> extensions) {
        var upload = labelled(new FileUploadInputElement()).setExtensions(extensions);
        upload.setSubmittedFileName("Anlage");
        return upload;
    }

    private static MultiCheckboxInputElement multiCheckbox(Integer minimumRequiredOptions, String... values) {
        var options = Arrays.stream(values)
                .map(value -> new MultiCheckboxInputElementOption().setValue(value).setLabel(value))
                .toList();
        return labelled(new MultiCheckboxInputElement())
                .setOptions(options)
                .setMinimumRequiredOptions(minimumRequiredOptions);
    }
}
