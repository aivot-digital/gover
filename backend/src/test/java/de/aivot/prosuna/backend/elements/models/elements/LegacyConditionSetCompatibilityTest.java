package de.aivot.prosuna.backend.elements.models.elements;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.ConditionSetOperator;
import de.aivot.prosuna.backend.models.functions.conditions.Condition;
import de.aivot.prosuna.backend.models.functions.conditions.ConditionSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacyConditionSetCompatibilityTest {
    @Test
    void preservesLegacyGroupsReferencesAndMessagesThroughPersistenceAndRepeatedEvaluation() {
        var field = new TextInputElement();
        field.setId("field");
        var condition = new Condition();
        condition.setOperator(ConditionOperator.Equals);
        condition.setReference("field");
        condition.setValue("accepted");
        condition.setConditionUnmetMessage("Einzelne Bedingung nicht erfüllt");
        var subgroup = new ConditionSet(ConditionSetOperator.All, List.of(condition), List.of(), "Inaktive Gruppenmeldung");
        var rule = new ConditionSet(ConditionSetOperator.Any, List.of(), List.of(subgroup), "Gruppe nicht erfüllt");
        field.setVisibility(new ElementVisibilityFunctions().setConditionSet(rule));
        field.setValidation(new ElementValidationFunctions().setConditionSet(subgroup));
        var root = new GroupLayoutElement();
        root.setId("group");
        root.setChildren(List.of(field));
        var mapper = JsonMapperTestUtils.createMapper();
        var restored = mapper.readValue(mapper.writeValueAsString(root), GroupLayoutElement.class);
        var restoredField = (TextInputElement) restored.getChildren().getFirst();
        assertEquals(rule, restoredField.getVisibility().getConditionSet());
        assertEquals(subgroup, restoredField.getValidation().getConditionSet());
        var data = new DerivedRuntimeElementData();
        for (var value : List.of("rejected", "accepted", "rejected")) {
            data.getEffectiveValues().put("field", value);
            assertEquals(value.equals("accepted") ? null : "Gruppe nicht erfüllt", restoredField.getVisibility().getConditionSet().evaluate(restored, data, restoredField));
            assertEquals(value.equals("accepted") ? null : "Einzelne Bedingung nicht erfüllt", restoredField.getValidation().getConditionSet().evaluate(restored, data, restoredField));
        }
    }
}
