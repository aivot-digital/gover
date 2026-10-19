package de.aivot.prosuna.backend.elements.utils;

import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.ComputedElementSubState;
import de.aivot.prosuna.backend.elements.models.ComputedElementStates;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.BaseFormElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.BaseStepElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.NumberInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.GenericStepElement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ElementStreamUtilsTest {
    @Test
    void applyActionVisitsElementsInPreOrder() {
        var form = form(
                "form",
                step(
                        "step",
                        text("first"),
                        group("group", number("nested")),
                        text("after")
                )
        );
        var visitedIds = new ArrayList<String>();

        ElementStreamUtils.applyAction(form, element -> visitedIds.add(element.getId()));

        assertEquals(List.of("form", "step", "first", "group", "nested", "after"), visitedIds);
    }

    @Test
    void applyActionVisitsLeafElementOnlyOnce() {
        var element = text("leaf");
        var visitedIds = new ArrayList<String>();

        ElementStreamUtils.applyAction(element, current -> visitedIds.add(current.getId()));

        assertEquals(List.of("leaf"), visitedIds);
    }

    @Test
    void applyActionWithParentsProvidesAncestorChainForEveryElement() {
        var form = form(
                "form",
                step(
                        "step",
                        text("first"),
                        group("group", number("nested")),
                        text("after")
                )
        );
        var parentsByElementId = new LinkedHashMap<String, List<String>>();

        ElementStreamUtils.applyActionWithParents(
                form,
                (parents, element) -> parentsByElementId.put(element.getId(), ids(parents))
        );

        assertEquals(List.of(), parentsByElementId.get("form"));
        assertEquals(List.of("form"), parentsByElementId.get("step"));
        assertEquals(List.of("form", "step"), parentsByElementId.get("first"));
        assertEquals(List.of("form", "step"), parentsByElementId.get("group"));
        assertEquals(List.of("form", "step", "group"), parentsByElementId.get("nested"));
        assertEquals(List.of("form", "step"), parentsByElementId.get("after"));
    }

    @Test
    void applyActionWithStatesUsesTopLevelStatesForNormalLayoutChildren() {
        var form = form(
                "form",
                step(
                        "step",
                        text("first"),
                        group("group", number("nested"))
                )
        );
        var states = new ComputedElementStates();
        states.put("form", state("form-state"));
        states.put("step", state("step-state"));
        states.put("first", state("first-state"));
        states.put("nested", state("nested-state"));
        var errorsByElementId = new LinkedHashMap<String, String>();

        ElementStreamUtils.applyAction(
                form,
                states,
                (element, state) -> errorsByElementId.put(element.getId(), state.getError())
        );

        assertEquals("form-state", errorsByElementId.get("form"));
        assertEquals("step-state", errorsByElementId.get("step"));
        assertEquals("first-state", errorsByElementId.get("first"));
        assertNull(errorsByElementId.get("group"));
        assertEquals("nested-state", errorsByElementId.get("nested"));
    }

    @Test
    void applyActionWithStatesTraversesReplicatingChildrenForEverySubstate() {
        var form = form(
                "form",
                step(
                        "step",
                        replicating(
                                "replicating",
                                group("replicatingGroup", text("replicatingText")),
                                number("replicatingNumber")
                        ),
                        text("after")
                )
        );
        var rowOneStates = new ComputedElementStates();
        rowOneStates.put("replicatingGroup", state("row-1-group"));
        rowOneStates.put("replicatingText", state("row-1-text"));
        var rowTwoStates = new ComputedElementStates();
        rowTwoStates.put("replicatingGroup", state("row-2-group"));
        rowTwoStates.put("replicatingText", state("row-2-text"));
        rowTwoStates.put("replicatingNumber", state("row-2-number"));
        var states = new ComputedElementStates();
        states.put("form", state("form-state"));
        states.put("step", state("step-state"));
        states.put("replicating", state("replicating-state").setSubStates(List.of(
                ComputedElementSubState.of("row-1", rowOneStates),
                ComputedElementSubState.of("row-2", rowTwoStates)
        )));
        states.put("replicatingText", state("top-level-text-state"));
        states.put("after", state("after-state"));
        var visits = new ArrayList<String>();

        ElementStreamUtils.applyAction(
                form,
                states,
                (element, state) -> visits.add(element.getId() + ":" + state.getError())
        );

        assertEquals(
                List.of(
                        "form:form-state",
                        "step:step-state",
                        "replicating:replicating-state",
                        "replicatingGroup:row-1-group",
                        "replicatingText:row-1-text",
                        "replicatingNumber:null",
                        "replicatingGroup:row-2-group",
                        "replicatingText:row-2-text",
                        "replicatingNumber:row-2-number",
                        "after:after-state"
                ),
                visits
        );
    }

    @Test
    void applyActionWithStatesDoesNotTraverseReplicatingChildrenWithoutSubstates() {
        var form = form(
                "form",
                step(
                        "step",
                        replicating("replicating", text("replicatingText")),
                        text("after")
                )
        );
        var states = new ComputedElementStates();
        states.put("replicating", state("replicating-state"));
        var visitedIds = new ArrayList<String>();

        ElementStreamUtils.applyAction(
                form,
                states,
                (element, state) -> visitedIds.add(element.getId())
        );

        assertEquals(List.of("form", "step", "replicating", "after"), visitedIds);
    }

    @Test
    void mapActionReturnsReplacementAndDoesNotTraverseChildrenWhenRootIsReplaced() {
        var form = form("form", step("step", text("child")));
        var replacement = text("replacement");
        var visitedIds = new ArrayList<String>();

        var mapped = ElementStreamUtils.mapAction(form, element -> {
            visitedIds.add(element.getId());
            return element == form ? replacement : element;
        });

        assertSame(replacement, mapped);
        assertEquals(List.of("form"), visitedIds);
    }

    @Test
    void mapActionReplacesChildrenInPlaceWhenLayoutElementIsKept() {
        var firstReplacement = text("first-mapped");
        var afterReplacement = text("after-mapped");
        var group = group("group", number("nested"));
        var step = step("step", text("first"), group, text("after"));
        var form = form("form", step);
        var visitedIds = new ArrayList<String>();

        var mapped = ElementStreamUtils.mapAction(form, element -> {
            visitedIds.add(element.getId());
            return switch (element.getId()) {
                case "first" -> firstReplacement;
                case "after" -> afterReplacement;
                default -> element;
            };
        });

        assertSame(form, mapped);
        assertEquals(List.of("form", "step", "first", "group", "nested", "after"), visitedIds);
        assertSame(step, form.getChildren().getFirst());
        assertEquals(List.of("first-mapped", "group", "after-mapped"), ids(step.getChildren()));
        assertEquals(List.of("nested"), ids(group.getChildren()));
    }

    @Test
    void mapActionDoesNotTraverseReplacedLayoutChildren() {
        var replacementGroup = group("replacementGroup", text("replacementChild"));
        var step = step("step", group("group", number("nested")));
        var form = form("form", step);
        var visitedIds = new ArrayList<String>();

        var mapped = ElementStreamUtils.mapAction(form, element -> {
            visitedIds.add(element.getId());
            return "group".equals(element.getId()) ? replacementGroup : element;
        });

        assertSame(form, mapped);
        assertEquals(List.of("form", "step", "group"), visitedIds);
        assertEquals(List.of("replacementGroup"), ids(step.getChildren()));
        assertEquals(List.of("replacementChild"), ids(replacementGroup.getChildren()));
        assertFalse(visitedIds.contains("nested"));
    }

    private static ComputedElementState state(String error) {
        return ComputedElementState.create().setError(error);
    }

    private static List<String> ids(List<? extends BaseElement> elements) {
        return elements.stream().map(BaseElement::getId).toList();
    }

    private static FormLayoutElement form(String id, BaseStepElement... children) {
        var element = (FormLayoutElement) new FormLayoutElement().setId(id);
        element.setChildren(new LinkedList<>(List.of(children)));
        return element;
    }

    private static GenericStepElement step(String id, BaseFormElement... children) {
        var element = (GenericStepElement) new GenericStepElement().setId(id);
        element.setChildren(new LinkedList<>(List.of(children)));
        return element;
    }

    private static GroupLayoutElement group(String id, BaseFormElement... children) {
        var element = (GroupLayoutElement) new GroupLayoutElement().setId(id);
        element.setChildren(new LinkedList<>(List.of(children)));
        return element;
    }

    private static ReplicatingContainerLayoutElement replicating(String id, BaseFormElement... children) {
        var element = (ReplicatingContainerLayoutElement) new ReplicatingContainerLayoutElement().setId(id);
        element.setChildren(new LinkedList<>(List.of(children)));
        return element;
    }

    private static TextInputElement text(String id) {
        return (TextInputElement) new TextInputElement().setId(id);
    }

    private static NumberInputElement number(String id) {
        return (NumberInputElement) new NumberInputElement().setId(id);
    }
}
