package de.aivot.GoverBackend.elements.utils;

import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.steps.StepElement;

import java.util.function.Consumer;

public class ElementStreamUtils {
    public static void applyAction(BaseElement element, Consumer<BaseElement> action) {
        action.accept(element);

        switch (element) {
            case RootElement rootElement:
                action.accept(rootElement.getIntroductionStep());
                action.accept(rootElement.getSummaryStep());
                action.accept(rootElement.getSubmitStep());

                for (BaseElement child : rootElement.getChildren()) {
                    applyAction(child, action);
                }
                break;
            case StepElement stepElement:
                for (BaseElement child : stepElement.getChildren()) {
                    applyAction(child, action);
                }
                break;
            case GroupLayout groupLayout:
                for (BaseElement child : groupLayout.getChildren()) {
                    applyAction(child, action);
                }
                break;
            case ReplicatingContainerLayout replicatingContainerLayout:
                for (BaseElement child : replicatingContainerLayout.getChildren()) {
                    applyAction(child, action);
                }
                break;
            default:
                break;
        }
    }
}
