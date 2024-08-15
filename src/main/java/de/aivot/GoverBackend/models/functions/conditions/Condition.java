package de.aivot.GoverBackend.models.functions.conditions;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    public String evaluate(String idPrefix, RootElement root, Map<String, Object> customerInput, ScriptEngine scriptEngine) {
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
        boolean isReferencedElementVisible = referencedElement.isVisible(idPrefix, root, customerInput, scriptEngine);
        Object rawValA = null;
        if (isReferencedElementVisible) {
            rawValA = customerInput.get(referencedElement.getResolvedId(idPrefix));
        }

        Object rawValB = null;
        if (!operator.getUnary()) {
            if (target != null) {
                Optional<? extends BaseElement> optTargetElement = root.findChild(target);
                if (optTargetElement.isEmpty()) {
                    return conditionUnmetMessage != null ? conditionUnmetMessage : "Targeted element not found";
                }

                boolean isTargetElementVisible = optTargetElement.get().isVisible(idPrefix, root, customerInput, scriptEngine);
                if (isTargetElementVisible) {
                    rawValB = customerInput.get(optTargetElement.get().getResolvedId(idPrefix));
                }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Condition condition = (Condition) o;

        if (operator != condition.operator) return false;
        if (!Objects.equals(reference, condition.reference)) return false;
        if (!Objects.equals(target, condition.target)) return false;
        if (!Objects.equals(value, condition.value)) return false;
        return Objects.equals(conditionUnmetMessage, condition.conditionUnmetMessage);
    }

    @Override
    public int hashCode() {
        int result = operator != null ? operator.hashCode() : 0;
        result = 31 * result + (reference != null ? reference.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (conditionUnmetMessage != null ? conditionUnmetMessage.hashCode() : 0);
        return result;
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
