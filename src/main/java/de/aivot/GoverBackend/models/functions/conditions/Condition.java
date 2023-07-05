package de.aivot.GoverBackend.models.functions.conditions;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

public class Condition {
    private ConditionOperator operator;
    private String reference;
    private String target;
    private String value;
    private String conditionUnmetMessage;

    public Condition(Map<String, Object> data) {
        operator = MapUtils.getEnum(data, "operator", Integer.class, ConditionOperator.values());

        reference = MapUtils.getString(data, "reference");
        target = MapUtils.getString(data, "target");
        value = MapUtils.getString(data, "value");

        conditionUnmetMessage = MapUtils.getString(data, "conditionUnmetMessage");
    }

    public String evaluate(String idPrefix, RootElement root, Map<String, Object> customerInput) {
        if (operator == null) {
            return "Evaluation failed. No operator";
        }

        if (reference == null) {
            return "Evaluation failed. reference";
        }

        Optional<? extends BaseElement> optReferencedElement = root.findChild(reference);

        if (optReferencedElement.isEmpty()) {
            return conditionUnmetMessage != null ? conditionUnmetMessage : "Referenced element not found";
        }

        BaseElement referencedElement = optReferencedElement.get();
        Object rawValA = customerInput.get(referencedElement.getResolvedId(idPrefix));

        Object rawValB = null;
        if (!operator.getUnary()) {
            if (target != null) {
                Optional<? extends BaseElement> optTargetElement = root.findChild(reference);
                if (optTargetElement.isEmpty()) {
                    return conditionUnmetMessage != null ? conditionUnmetMessage : "Targeted element not found";
                }
                rawValB = customerInput.get(optTargetElement.get().getResolvedId(idPrefix));
            } else {
                rawValB = value;
            }
        }

        boolean evaluationResult = referencedElement.evaluate(operator, rawValA, rawValB);
        if (evaluationResult) {
            return null;
        } else {
            return conditionUnmetMessage != null ? conditionUnmetMessage : "Evaluation returned false";
        }
    }

    public ConditionOperator getOperator() {
        return operator;
    }

    public void setOperator(ConditionOperator operator) {
        this.operator = operator;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getConditionUnmetMessage() {
        return conditionUnmetMessage;
    }

    public void setConditionUnmetMessage(String conditionUnmetMessage) {
        this.conditionUnmetMessage = conditionUnmetMessage;
    }
}
