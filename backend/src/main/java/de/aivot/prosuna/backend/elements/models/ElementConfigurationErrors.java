package de.aivot.prosuna.backend.elements.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.SummaryLayoutElement;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Configuration errors of an element tree, as reported by {@link BaseElement#validateConfiguration()}.
 *
 * @param properties Error messages of this element, keyed by the name of the erroneous property.
 * @param children   Errors of child elements, keyed by child ID. Children without errors are omitted.
 */
public record ElementConfigurationErrors(
        @Nonnull Map<String, List<String>> properties,
        @Nonnull Map<String, ElementConfigurationErrors> children
) {
    @Nonnull
    public static ElementConfigurationErrors of(@Nonnull BaseElement element) {
        var children = new LinkedHashMap<String, ElementConfigurationErrors>();
        for (var child : validatedChildren(element)) {
            var childErrors = of(child);
            if (!childErrors.isEmpty()) {
                children.put(child.getId(), childErrors);
            }
        }
        return new ElementConfigurationErrors(element.validateConfiguration(), children);
    }

    @Nonnull
    private static List<? extends BaseElement> validatedChildren(@Nonnull BaseElement element) {
        // Summary children only display values, so the editor hides their input settings and the runtime skips their
        // errors, see ElementDerivationService. Some layouts keep children deserialized as null.
        if (element instanceof LayoutElement<?> layoutElement
                && !(element instanceof SummaryLayoutElement)
                && layoutElement.getChildren() != null) {
            return layoutElement.getChildren();
        }
        return List.of();
    }

    // Jackson would otherwise serialize this as an "empty" property of the API response.
    @JsonIgnore
    public boolean isEmpty() {
        return properties.isEmpty() && children.isEmpty();
    }

    /**
     * Creates readable messages for error lists that cannot show the tree, e.g. the problems of a process node.
     *
     * @param element The element these errors were created for. Its own messages are not prefixed, because callers
     *                already name it, e.g. by the label of the field holding the element tree.
     */
    @Nonnull
    public List<String> describe(@Nonnull BaseElement element) {
        return describe(element, "");
    }

    @Nonnull
    private List<String> describe(@Nonnull BaseElement element, @Nonnull String prefix) {
        var messages = new LinkedList<String>();
        // The same message is reported for every property involved in a rule, but should only be listed once.
        properties.values().stream()
                .flatMap(List::stream)
                .distinct()
                .forEach(message -> messages.add(prefix + message));

        for (var child : validatedChildren(element)) {
            var childErrors = children.get(child.getId());
            if (childErrors != null) {
                messages.addAll(childErrors.describe(child, "Element " + StringUtils.quote(describeElement(child)) + ": "));
            }
        }
        return messages;
    }

    @Nonnull
    private static String describeElement(@Nonnull BaseElement element) {
        var label = element instanceof BaseInputElement<?> input
                ? StringUtils.toNullableTrimmedString(input.getLabel())
                : null;
        if (label == null) {
            label = StringUtils.toNullableTrimmedString(element.getName());
        }
        return label != null ? label : element.getId();
    }
}
