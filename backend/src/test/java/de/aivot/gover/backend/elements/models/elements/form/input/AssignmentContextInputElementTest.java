package de.aivot.gover.backend.elements.models.elements.form.input;

import de.aivot.gover.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
