package de.aivot.gover.backend.elements.models.elements.form.content;

import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.form.content.ProcessAttachmentDisplayContentElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ProcessAttachmentDisplayContentElementTest {
    @Test
    void shouldRoundTripThroughBaseElementSerialization() throws Exception {
        var serialized = ObjectMapperFactory
                .getInstance()
                .writeValueAsString(new ProcessAttachmentDisplayContentElement()
                        .setAttachmentSetKey("case_documents")
                        .setLabel("Fallunterlagen")
                        .setHint("Bitte prüfen Sie den Anhang."));

        var deserialized = ObjectMapperFactory
                .getInstance()
                .readValue(serialized, BaseElement.class);

        assertInstanceOf(ProcessAttachmentDisplayContentElement.class, deserialized);
        assertEquals("case_documents", ((ProcessAttachmentDisplayContentElement) deserialized).getAttachmentSetKey());
        assertEquals("Fallunterlagen", ((ProcessAttachmentDisplayContentElement) deserialized).getLabel());
        assertEquals("Bitte prüfen Sie den Anhang.", ((ProcessAttachmentDisplayContentElement) deserialized).getHint());
    }
}
