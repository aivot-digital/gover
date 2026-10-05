package de.aivot.prosuna.backend.elements.models.elements.form.input;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentContextInputElementTest {
    private static final String PREFERENCE_WITHOUT_SELECTION_MESSAGE = "Für eine Bevorzugung muss ein Personenkreis ausgewählt sein.";

    @Test
    void shouldRejectGeneralPreferenceWithoutSelection() {
        var element = new AssignmentContextInputElement();
        var value = new AssignmentContextInputElementValue()
                .setGeneralAssigneePreference(AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE);

        var exception = assertThrows(ValidationException.class, () -> element.performValidation(value));

        assertEquals(PREFERENCE_WITHOUT_SELECTION_MESSAGE, exception.getMessage());
    }

    @Test
    void shouldRejectRepeatExecutionPreferenceWithoutSelection() {
        var element = new AssignmentContextInputElement();
        var value = new AssignmentContextInputElementValue()
                .setRepeatExecutionAssigneePreference(AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE);

        var exception = assertThrows(ValidationException.class, () -> element.performValidation(value));

        assertEquals(PREFERENCE_WITHOUT_SELECTION_MESSAGE, exception.getMessage());
    }

    @Test
    void shouldAcceptPreferenceWithSelection() {
        var element = new AssignmentContextInputElement();
        var value = new AssignmentContextInputElementValue()
                .setDomainAndUserSelection(List.of(new DomainAndUserSelectInputElementValue("orgUnit", "10")))
                .setRepeatExecutionAssigneePreference(AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE);

        assertDoesNotThrow(() -> element.performValidation(value));
    }

    @Test
    void shouldSerializePreferenceRestrictions() {
        var element = new AssignmentContextInputElement()
                .setDisableProcessInstanceAssigneeOption(true)
                .setDisableAssignmentContextRepeatExecutionAssigneePreferenceOptions(true);
        element.setId("assignmentContext");
        var sameElement = new AssignmentContextInputElement()
                .setDisableProcessInstanceAssigneeOption(true)
                .setDisableAssignmentContextRepeatExecutionAssigneePreferenceOptions(true);
        sameElement.setId("assignmentContext");
        var differentElement = new AssignmentContextInputElement()
                .setDisableProcessInstanceAssigneeOption(false)
                .setDisableAssignmentContextRepeatExecutionAssigneePreferenceOptions(true);
        differentElement.setId("assignmentContext");

        var json = new ObjectMapper().valueToTree(element);

        assertTrue(json.path("disableProcessInstanceAssigneeOption").asBoolean());
        assertTrue(json.path("disableAssignmentContextRepeatExecutionAssigneePreferenceOptions").asBoolean());
        assertEquals(element, sameElement);
        assertEquals(element.hashCode(), sameElement.hashCode());
        assertNotEquals(element, differentElement);
    }

    @Test
    void shouldRejectDisabledPreferencesWithoutAffectingUnrestrictedElements() {
        var selection = List.of(new DomainAndUserSelectInputElementValue("user", "recipient"));
        var generalValue = new AssignmentContextInputElementValue()
                .setDomainAndUserSelection(selection)
                .setGeneralAssigneePreference(AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PROCESS_INSTANCE_ASSIGNEE);
        var repeatValue = new AssignmentContextInputElementValue()
                .setDomainAndUserSelection(selection)
                .setRepeatExecutionAssigneePreference(AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE);
        var unrestricted = new AssignmentContextInputElement();
        var restricted = new AssignmentContextInputElement()
                .setDisableProcessInstanceAssigneeOption(true)
                .setDisableAssignmentContextRepeatExecutionAssigneePreferenceOptions(true);

        assertDoesNotThrow(() -> unrestricted.performValidation(generalValue));
        assertDoesNotThrow(() -> unrestricted.performValidation(repeatValue));
        assertEquals("Die Bevorzugung der dem Vorgang zugewiesenen Person ist hier nicht zulässig.",
                assertThrows(ValidationException.class, () -> restricted.performValidation(generalValue)).getMessage());
        assertEquals("Eine Bevorzugung bei erneuter Ausführung ist hier nicht zulässig.",
                assertThrows(ValidationException.class, () -> restricted.performValidation(repeatValue)).getMessage());
    }
}
