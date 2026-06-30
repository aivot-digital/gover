package de.aivot.gover.backend.elements.utils;

import de.aivot.gover.backend.elements.models.elements.BaseFormElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.gover.backend.elements.models.elements.steps.BaseStepElement;
import de.aivot.gover.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.gover.backend.elements.utils.ElementReferenceUtils;
import de.aivot.gover.backend.javascript.models.JavascriptCode;
import de.aivot.gover.backend.nocode.models.NoCodeExpression;
import de.aivot.gover.backend.nocode.models.NoCodeProcessDataReference;
import de.aivot.gover.backend.nocode.models.NoCodeReference;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementReferenceUtilsTest {
    @Test
    void shouldResolveJavascriptProcessDataReferencesFromDestinationKeys() {
        var destinationKeyIndex = Map.of("person.vorname", Set.of("tx_123"));
        var javascriptCode = JavascriptCode.of("""
                (() => {
                    const name = $.person.vorname;
                    return name;
                })();
                """);

        var referencedIds = ElementReferenceUtils.getReferencedIds(
                javascriptCode,
                null,
                null,
                destinationKeyIndex
        );

        assertEquals(Set.of("tx_123"), referencedIds);
    }

    @Test
    void shouldKeepExistingJavascriptElementReferenceExtraction() {
        var destinationKeyIndex = Map.of("person.vorname", Set.of("tx_123"));
        var javascriptCode = JavascriptCode.of("""
                (() => {
                    //>>>text_123
                    const postalCode = ctx.effectiveValues.text_456;
                    const state = ctx.elementStates.text_789;
                    const name = $.person.vorname;
                    return postalCode;
                })();
                """);

        var referencedIds = ElementReferenceUtils.getReferencedIds(
                javascriptCode,
                null,
                null,
                destinationKeyIndex
        );

        assertEquals(4, referencedIds.size());
        assertTrue(referencedIds.contains("text_123"));
        assertTrue(referencedIds.contains("text_456"));
        assertTrue(referencedIds.contains("text_789"));
        assertTrue(referencedIds.contains("tx_123"));
    }

    @Test
    void shouldResolveDirectNoCodeProcessDataReferencesFromDestinationKeys() {
        var destinationKeyIndex = Map.of("person.vorname", Set.of("tx_123"));

        var referencedIds = ElementReferenceUtils.getReferencedIds(
                null,
                new NoCodeProcessDataReference("person.vorname"),
                null,
                destinationKeyIndex
        );

        assertEquals(Set.of("tx_123"), referencedIds);
    }

    @Test
    void shouldResolveNestedNoCodeProcessDataReferencesFromDestinationKeys() {
        var destinationKeyIndex = Map.of("person.vorname", Set.of("tx_123"));
        var expression = NoCodeExpression.of(
                "test",
                NoCodeReference.of("direct_123"),
                NoCodeExpression.of(
                        "nested",
                        new NoCodeProcessDataReference("person.vorname")
                )
        );

        var referencedIds = ElementReferenceUtils.getReferencedIds(
                null,
                expression,
                null,
                destinationKeyIndex
        );

        assertEquals(Set.of("direct_123", "tx_123"), referencedIds);
    }

    @Test
    void shouldBuildDestinationKeyIndexWithDuplicateDestinationKeys() {
        var firstName = new TextInputElement();
        firstName.setId("tx_123");
        firstName.setDestinationKey(" person.vorname ");

        var duplicateFirstName = new TextInputElement();
        duplicateFirstName.setId("tx_456");
        duplicateFirstName.setDestinationKey("person.vorname");

        var ignored = new TextInputElement();
        ignored.setId("tx_789");

        var step = new GenericStepElement();
        step.setChildren(new LinkedList<BaseFormElement>(List.of(firstName, duplicateFirstName, ignored)));

        var root = new FormLayoutElement();
        root.setChildren(new LinkedList<BaseStepElement>(List.of(step)));

        var destinationKeyIndex = ElementReferenceUtils.buildDestinationKeyIndex(root);

        assertEquals(Set.of("tx_123", "tx_456"), destinationKeyIndex.get("person.vorname"));
    }
}
