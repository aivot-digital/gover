package de.aivot.gover.backend.process.models;

import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.models.elements.BaseFormElement;
import de.aivot.gover.backend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.gover.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.gover.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.models.ProcessNodeDefinitionMetadata;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessNodeDefinitionMetadataTest {
    private static final Pattern ELEMENT_ID_PATTERN = Pattern.compile("^[a-z][a-zA-Z0-9_]*$");

    @Test
    void withLayout_ShouldExposeCompleteFormAndIndividualSectionsAsReusableUiDefinitions() {
        var formLayout = new FormLayoutElement();
        formLayout.setId("rt_form");
        formLayout.setChildren(List.of(
                step("st_person", "Personendaten", textInput("tx_name")),
                step("st_unnamed", null, textInput("tx_note"))
        ));

        var metadata = ProcessNodeDefinitionMetadata
                .empty()
                .withLayout(formLayout, origin());

        assertEquals(3, metadata.reusableUiDefinitions().size());

        var completeFormDefinition = metadata.reusableUiDefinitions().get(0);
        assertEquals("Gesamtes Formular", completeFormDefinition.label());

        var completeFormGroup = assertInstanceOf(GroupLayoutElement.class, completeFormDefinition.uiDefinition());
        assertEquals("Gesamtes Formular", completeFormGroup.getName());
        assertValidElementId(completeFormGroup.getId());
        assertEquals(2, completeFormGroup.getChildren().size());

        var firstSectionGroup = assertInstanceOf(GroupLayoutElement.class, completeFormGroup.getChildren().get(0));
        assertEquals("Personendaten", firstSectionGroup.getName());
        assertSectionHeadline(firstSectionGroup, "Personendaten");

        var secondSectionGroup = assertInstanceOf(GroupLayoutElement.class, completeFormGroup.getChildren().get(1));
        assertEquals("Unbenannter Abschnitt", secondSectionGroup.getName());
        assertSectionHeadline(secondSectionGroup, "Unbenannter Abschnitt");

        var namedSectionDefinition = metadata.reusableUiDefinitions().get(1);
        assertEquals("Personendaten", namedSectionDefinition.label());
        var namedSectionGroup = assertInstanceOf(GroupLayoutElement.class, namedSectionDefinition.uiDefinition());
        assertEquals("Personendaten", namedSectionGroup.getName());
        assertSectionHeadline(namedSectionGroup, "Personendaten");

        var unnamedSectionDefinition = metadata.reusableUiDefinitions().get(2);
        assertEquals("Unbenannter Abschnitt", unnamedSectionDefinition.label());
        var unnamedSectionGroup = assertInstanceOf(GroupLayoutElement.class, unnamedSectionDefinition.uiDefinition());
        assertEquals("Unbenannter Abschnitt", unnamedSectionGroup.getName());
        assertSectionHeadline(unnamedSectionGroup, "Unbenannter Abschnitt");
    }

    @Test
    void withLayout_ShouldExposeRootGroupLayoutsFromNonFormUiDefinitions() {
        var groupLayout = new GroupLayoutElement();
        groupLayout.setId("gp_task");
        groupLayout.setName("Aufgabendaten");
        groupLayout.setChildren(new LinkedList<>(List.of(textInput("tx_task"))));

        var metadata = ProcessNodeDefinitionMetadata
                .empty()
                .withLayout(groupLayout, origin());

        assertEquals(1, metadata.reusableUiDefinitions().size());

        var reusableUiDefinition = metadata.reusableUiDefinitions().get(0);
        assertEquals("Aufgabendaten", reusableUiDefinition.label());
        assertInstanceOf(GroupLayoutElement.class, reusableUiDefinition.uiDefinition());
    }

    @Test
    void withLayout_ShouldExposeFileUploadAttachmentSetMultiplicity() {
        var groupLayout = new GroupLayoutElement();
        groupLayout.setId("gp_uploads");
        groupLayout.setName("Uploaddaten");
        groupLayout.setChildren(new LinkedList<>(List.of(
                fileUpload("fu_single", "documents.single", "Einzeldokument", false),
                fileUpload("fu_multi", "documents.multi", "Mehrere Dokumente", true),
                replicatingContainer("rc_documents", "documentRows", group(
                        "gp_nested_uploads",
                        fileUpload("fu_replicated", "documents.replicated", "Wiederholtes Dokument", false)
                ))
        )));

        var metadata = ProcessNodeDefinitionMetadata
                .empty()
                .withLayout(groupLayout, origin());

        assertEquals(3, metadata.forwardedAttachmentSets().size());

        var singleFileAttachmentSet = metadata.forwardedAttachmentSets().get(0);
        assertEquals("documents_single", singleFileAttachmentSet.dataKey());
        assertEquals("Einzeldokument", singleFileAttachmentSet.label());
        assertFalse(singleFileAttachmentSet.isMultifile());

        var multiFileAttachmentSet = metadata.forwardedAttachmentSets().get(1);
        assertEquals("documents_multi", multiFileAttachmentSet.dataKey());
        assertEquals("Mehrere Dokumente", multiFileAttachmentSet.label());
        assertTrue(multiFileAttachmentSet.isMultifile());

        var replicatedAttachmentSet = metadata.forwardedAttachmentSets().get(2);
        assertEquals("documents_replicated", replicatedAttachmentSet.dataKey());
        assertEquals("Wiederholtes Dokument", replicatedAttachmentSet.label());
        assertTrue(replicatedAttachmentSet.isMultifile());
    }

    @Test
    void withLayout_ShouldSkipFileUploadAttachmentSetWhenNormalizedDataKeyExceedsRuntimeLimit() {
        var validDataKey = "x".repeat(255);

        var groupLayout = new GroupLayoutElement();
        groupLayout.setId("gp_uploads");
        groupLayout.setName("Uploaddaten");
        groupLayout.setChildren(new LinkedList<>(List.of(
                fileUpload("fu_valid", validDataKey, "Gültiges Dokument", false),
                fileUpload("fu_too_long", "x".repeat(256), "Zu langes Dokument", false)
        )));

        var metadata = ProcessNodeDefinitionMetadata
                .empty()
                .withLayout(groupLayout, origin());

        assertEquals(1, metadata.forwardedAttachmentSets().size());
        assertEquals(validDataKey, metadata.forwardedAttachmentSets().getFirst().dataKey());
    }

    @Test
    void forwardedAttachmentSet_ShouldSerializeMultifileFlagWithFrontendPropertyName() throws Exception {
        var metadata = ProcessNodeDefinitionMetadata
                .empty()
                .addForwardedAttachmentSet("documents", "Dokumente", null, true, origin());

        var json = ObjectMapperFactory
                .getInstance()
                .writeValueAsString(metadata);

        assertTrue(json.contains("\"isMultifile\":true"));
    }

    private static void assertSectionHeadline(GroupLayoutElement sectionGroup, String expectedTitle) {
        assertTrue(sectionGroup.getChildren().size() >= 1);

        var headline = assertInstanceOf(HeadlineContentElement.class, sectionGroup.getChildren().get(0));
        assertEquals(expectedTitle, headline.getContent());
        assertValidElementId(headline.getId());
    }

    private static void assertValidElementId(String id) {
        assertTrue(ELEMENT_ID_PATTERN.matcher(id).matches(), "Expected valid element id, got " + id);
    }

    private static GenericStepElement step(String id, String title, BaseFormElement... children) {
        var step = new GenericStepElement();
        step.setId(id);
        step.setTitle(title);
        step.setChildren(List.of(children));
        return step;
    }

    private static TextInputElement textInput(String id) {
        var textInput = new TextInputElement();
        textInput.setId(id);
        return textInput;
    }

    private static GroupLayoutElement group(String id, BaseFormElement... children) {
        var group = new GroupLayoutElement();
        group.setId(id);
        group.setChildren(List.of(children));
        return group;
    }

    private static ReplicatingContainerLayoutElement replicatingContainer(String id, String destinationKey, BaseFormElement... children) {
        var replicatingContainer = new ReplicatingContainerLayoutElement();
        replicatingContainer.setId(id);
        replicatingContainer.setDestinationKey(destinationKey);
        replicatingContainer.setChildren(List.of(children));
        return replicatingContainer;
    }

    private static FileUploadInputElement fileUpload(String id, String destinationKey, String label, boolean isMultifile) {
        var fileUpload = new FileUploadInputElement();
        fileUpload.setId(id);
        fileUpload.setDestinationKey(destinationKey);
        fileUpload.setLabel(label);
        fileUpload.setIsMultifile(isMultifile);
        return fileUpload;
    }

    private static ProcessNodeEntity origin() {
        return new ProcessNodeEntity()
                .setId(123)
                .setName("Ausgangsknoten");
    }
}
