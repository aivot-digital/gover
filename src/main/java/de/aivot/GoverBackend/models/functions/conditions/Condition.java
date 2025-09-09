package de.aivot.GoverBackend.models.functions.conditions;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.ElementWithChildren;
import de.aivot.GoverBackend.enums.ConditionOperator;

import java.io.Serializable;
import java.util.Objects;

public class Condition implements Serializable {
    private ConditionOperator operator;
    private String reference;
    private String target;
    private String value;
    private String conditionUnmetMessage;

    public Condition() {

    }

    public String evaluate(ElementWithChildren<?> rootElement, ElementData elementData, BaseElement element) {
        // Prüfe, ob ein Operator angegeben wurde. Falls nicht, gib eine Fehlermeldung zurück.
        if (operator == null) {
            return "Auswertung fehlgeschlagen. Kein Operator angegeben.";
        }

        // Prüfe, ob ein Referenz-Element angegeben wurde. Falls nicht, gib eine Fehlermeldung zurück.
        if (reference == null) {
            return "Auswertung fehlgeschlagen. Kein Referenz-Element angegeben.";
        }

        // Prüfe, ob das Referenz-Element gefunden werden kann. Falls nicht, gib eine Fehlermeldung zurück.
        var optReferencedElement = rootElement
                .findChild(reference);

        if (optReferencedElement.isEmpty()) {
            return "Auswertung fehlgeschlagen. Referenz-Element nicht gefunden.";
        }

        var referencedBaseElement = optReferencedElement.get();

        // Prüfe, ob das Referenz-Element ein Eingabe-Element ist. Falls nicht, gib eine Fehlermeldung zurück.
        if (referencedBaseElement instanceof BaseInputElement<?> referencedElement) {
            // Hole den Wert des Referenz-Elements. Dieser kann NULL sein.
            var rawValA = elementData
                    .getOrDefault(referencedElement.getId(), new ElementDataObject(referencedElement))
                    .getValue();

            Object rawValB = null;
            if (!operator.getUnary()) {
                if (target != null) {
                    var optTargetElement = rootElement
                            .findChild(target);

                    if (optTargetElement.isEmpty()) {
                        return "Auswertung fehlgeschlagen. Ziel-Element nicht gefunden.";
                    }

                    var targetBaseElement = optTargetElement.get();

                    if (targetBaseElement instanceof BaseInputElement<?> targetElement) {
                        rawValB = elementData
                                .getOrDefault(targetElement.getId(), new ElementDataObject(targetElement))
                                .getValue();
                    } else {
                        return "Auswertung fehlgeschlagen. Ziel-Element ist kein Eingabe-Element.";
                    }
                } else {
                    rawValB = value;
                }
            }

            boolean evaluationResult = referencedElement.evaluate(operator, rawValA, rawValB);
            if (evaluationResult) {
                return null;
            } else {
                return conditionUnmetMessage != null ? conditionUnmetMessage : "Bedingung nicht erfüllt.";
            }
        } else {
            return "Auswertung fehlgeschlagen. Referenz-Element ist kein Eingabe-Element.";
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
