package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.PrintableElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AssignmentContextInputElement extends BaseInputElement<AssignmentContextInputElementValue> implements PrintableElement<AssignmentContextInputElementValue> {
    public static final String ALLOWED_TYPE_ORG_UNIT = "orgUnit";
    public static final String ALLOWED_TYPE_TEAM = "team";
    public static final String ALLOWED_TYPE_USER = "user";

    @Nullable
    private String headline;

    @Nullable
    private String text;

    @Nullable
    private String placeholder;

    @Nullable
    private Integer minItems;

    @Nullable
    private Integer maxItems;

    @Nullable
    private List<String> allowedTypes;

    @Nullable
    private DomainAndUserSelectProcessAccessConstraint processAccessConstraint;

    public AssignmentContextInputElement() {
        super(ElementType.AssignmentContext);
    }

    @Nullable
    @Override
    public AssignmentContextInputElementValue formatValue(@Nullable Object value) {
        return _formatValue(value);
    }

    @Override
    public void performValidation(@Nullable AssignmentContextInputElementValue value) throws ValidationException {
        if (processAccessConstraint != null) {
            var processId = processAccessConstraint.getProcessId();
            var processVersion = processAccessConstraint.getProcessVersion();

            if (processId == null || processVersion == null) {
                throw new ValidationException(this, "Die Prozessbeschränkung muss Prozess-ID und Prozessversion enthalten.");
            }

            if (processId <= 0 || processVersion <= 0) {
                throw new ValidationException(this, "Prozess-ID und Prozessversion müssen größer als 0 sein.");
            }
        }

        if (value == null) {
            return;
        }

        var generalAssigneePreference = value.getGeneralAssigneePreference();
        if (generalAssigneePreference != null && !_isGeneralAssigneePreference(generalAssigneePreference)) {
            throw new ValidationException(this, "Ungültige Bevorzugung bei der Zuweisung.");
        }

        var repeatExecutionAssigneePreference = value.getRepeatExecutionAssigneePreference();
        if (repeatExecutionAssigneePreference != null && !_isRepeatExecutionAssigneePreference(repeatExecutionAssigneePreference)) {
            throw new ValidationException(this, "Ungültige Bevorzugung bei erneuter Ausführung.");
        }

        var normalizedValues = value
                .getDomainAndUserSelection();
        var hasSelection = normalizedValues != null && !normalizedValues.isEmpty();
        var hasPreference = generalAssigneePreference != null || repeatExecutionAssigneePreference != null;

        if (!hasSelection && hasPreference) {
            throw new ValidationException(this, "Für eine Bevorzugung muss ein Personenkreis ausgewählt sein.");
        }

        if (!hasSelection && Boolean.TRUE.equals(getRequired())) {
            throw new ValidationException(this, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
        }

        if (!hasSelection) {
            return;
        }

        if (minItems != null && minItems > 0 && normalizedValues.size() < minItems) {
            throw new ValidationException(this, "Mindestens " + minItems + " Einträge erforderlich.");
        }

        if (maxItems != null && maxItems > 0 && normalizedValues.size() > maxItems) {
            throw new ValidationException(this, "Maximal " + maxItems + " Einträge erlaubt.");
        }

        if (normalizedValues.stream().anyMatch(v -> !isAllowedType(v.getType()) || v.getId() == null || v.getId().isBlank())) {
            throw new ValidationException(this, "Ungültiger Eintrag in der Auswahl.");
        }

        if (normalizedValues.stream().anyMatch(v -> !isTypeEnabled(v.getType()))) {
            throw new ValidationException(this, "Eintragstyp ist laut Konfiguration nicht erlaubt.");
        }

        var distinctKeys = normalizedValues
                .stream()
                .map(DomainAndUserSelectInputElementValue::toComparableKey)
                .distinct()
                .count();

        if (distinctKeys != normalizedValues.size()) {
            throw new ValidationException(this, "Mehrfach vorhandene Einträge sind nicht erlaubt.");
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable AssignmentContextInputElementValue value) {
        if (value == null || value.isEmpty()) {
            return "Keine Angabe";
        }

        var sections = new ArrayList<String>();

        var selectedValues = value.getDomainAndUserSelection();
        if (selectedValues != null && !selectedValues.isEmpty()) {
            var selectedDisplay = selectedValues
                    .stream()
                    .filter(Objects::nonNull)
                    .map(AssignmentContextInputElement::formatDisplayToken)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Keine Angabe");
            sections.add("Personenkreis: " + selectedDisplay);
        }

        var preferences = new ArrayList<String>();
        if (value.prefersPreviousProcessStepAssignee()) {
            preferences.add("Bevorzuge Bearbeiter:in des vorherigen Prozessschritts");
        } else if (value.prefersUninvolvedUser()) {
            preferences.add("Bevorzuge eine neue, unbeteiligte Mitarbeiter:in");
        } else if (value.prefersProcessInstanceAssignee()) {
            preferences.add("Bevorzuge die dem Vorgang zugewiesene Mitarbeiter:in");
        }

        if (value.prefersPreviousIterationAssignee()) {
            preferences.add("Bevorzuge zuletzt zuständige Mitarbeiter:in");
        }

        if (value.prefersDifferentFromPreviousIterationAssignee()) {
            preferences.add("Bevorzuge eine andere Mitarbeiter:in");
        }

        if (!preferences.isEmpty()) {
            sections.add("Präferenzen: " + String.join(", ", preferences));
        }

        if (sections.isEmpty()) {
            return "Keine Angabe";
        }

        return String.join(" | ", sections);
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        var valueA = _formatValue(referencedValue);

        if (valueA == null || valueA.isEmpty()) {
            return operator == ConditionOperator.Empty || operator == ConditionOperator.NotIncludes;
        }

        if (operator == ConditionOperator.NotEmpty) {
            return true;
        }

        if (operator == ConditionOperator.Empty) {
            return false;
        }

        var selectedValues = valueA.getDomainAndUserSelection();
        if (selectedValues == null || selectedValues.isEmpty()) {
            return operator == ConditionOperator.NotIncludes;
        }

        var valueAKeys = selectedValues
                .stream()
                .map(DomainAndUserSelectInputElementValue::toComparableKey)
                .collect(Collectors.toSet());

        var comparedKeys = _formatComparedKeys(comparedValue);
        if (comparedKeys.isEmpty()) {
            return operator == ConditionOperator.NotIncludes;
        }

        return switch (operator) {
            case Includes -> comparedKeys.stream().allMatch(valueAKeys::contains);
            case NotIncludes -> comparedKeys.stream().allMatch(key -> !valueAKeys.contains(key));
            default -> false;
        };
    }

    @Nullable
    public static AssignmentContextInputElementValue _formatValue(@Nullable Object value) {
        var result = switch (value) {
            case null -> null;
            case AssignmentContextInputElementValue val -> val;
            case Map<?, ?> map -> _formatSingleValue(map);
            case DomainAndUserSelectInputElementValue singleValue -> new AssignmentContextInputElementValue()
                    .setDomainAndUserSelection(List.of(singleValue));
            case Collection<?> cValue -> new AssignmentContextInputElementValue()
                    .setDomainAndUserSelection(_formatDomainSelection(cValue));
            case String token -> {
                var parsed = DomainAndUserSelectInputElement._formatValue(List.of(token));
                if (parsed == null) {
                    yield null;
                }

                yield new AssignmentContextInputElementValue()
                        .setDomainAndUserSelection(parsed);
            }
            default -> null;
        };

        if (result == null) {
            return null;
        }

        var normalizedSelection = _formatDomainSelection(result.getDomainAndUserSelection());

        result
                .setDomainAndUserSelection(normalizedSelection)
                .setGeneralAssigneePreference(_formatGeneralAssigneePreference(result.getGeneralAssigneePreference()))
                .setRepeatExecutionAssigneePreference(_formatRepeatExecutionAssigneePreference(result.getRepeatExecutionAssigneePreference()));

        return result.isEmpty() ? null : result;
    }

    @Nullable
    private static AssignmentContextInputElementValue _formatSingleValue(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case AssignmentContextInputElementValue val -> val;
            case Map<?, ?> map -> new AssignmentContextInputElementValue()
                    .setDomainAndUserSelection(_formatDomainSelection(map.get("domainAndUserSelection")))
                    .setGeneralAssigneePreference(_formatGeneralAssigneePreference(map.get("generalAssigneePreference")))
                    .setRepeatExecutionAssigneePreference(_formatRepeatExecutionAssigneePreference(map.get("repeatExecutionAssigneePreference")));
            case Collection<?> cValue -> new AssignmentContextInputElementValue()
                    .setDomainAndUserSelection(_formatDomainSelection(cValue));
            case String token -> {
                var parsed = DomainAndUserSelectInputElement._formatValue(List.of(token));
                if (parsed == null) {
                    yield null;
                }

                yield new AssignmentContextInputElementValue()
                        .setDomainAndUserSelection(parsed);
            }
            default -> null;
        };
    }

    @Nullable
    private static List<DomainAndUserSelectInputElementValue> _formatDomainSelection(@Nullable Object value) {
        var values = DomainAndUserSelectInputElement._formatValue(value);

        if (values == null || values.isEmpty()) {
            return null;
        }

        var normalized = values
                .stream()
                .filter(Objects::nonNull)
                .filter(v -> !v.isEmpty())
                .toList();

        return normalized.isEmpty() ? null : normalized;
    }

    @Nullable
    private static String _formatGeneralAssigneePreference(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        var normalized = value.toString().trim();
        if (normalized.isBlank() || "none".equalsIgnoreCase(normalized)) {
            return null;
        }
        if (AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE.equalsIgnoreCase(normalized)) {
            return AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE;
        }
        if (AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_UNINVOLVED_USER.equalsIgnoreCase(normalized)) {
            return AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_UNINVOLVED_USER;
        }
        if (AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PROCESS_INSTANCE_ASSIGNEE.equalsIgnoreCase(normalized)) {
            return AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PROCESS_INSTANCE_ASSIGNEE;
        }

        return normalized;
    }

    @Nullable
    private static String _formatRepeatExecutionAssigneePreference(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        var normalized = value.toString().trim();
        if (normalized.isBlank() || "none".equalsIgnoreCase(normalized)) {
            return null;
        }
        if (AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE.equalsIgnoreCase(normalized)) {
            return AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE;
        }
        if (AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_DIFFERENT_FROM_PREVIOUS_ITERATION_ASSIGNEE.equalsIgnoreCase(normalized)) {
            return AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_DIFFERENT_FROM_PREVIOUS_ITERATION_ASSIGNEE;
        }

        return normalized;
    }

    private static boolean _isGeneralAssigneePreference(@Nonnull String value) {
        return AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE.equals(value)
                || AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_UNINVOLVED_USER.equals(value)
                || AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PROCESS_INSTANCE_ASSIGNEE.equals(value);
    }

    private static boolean _isRepeatExecutionAssigneePreference(@Nonnull String value) {
        return AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE.equals(value)
                || AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_DIFFERENT_FROM_PREVIOUS_ITERATION_ASSIGNEE.equals(value);
    }

    @Nonnull
    private static Set<String> _formatComparedKeys(@Nullable Object comparedValue) {
        var comparedValues = switch (comparedValue) {
            case Map<?, ?> map -> {
                if (map.containsKey("domainAndUserSelection")) {
                    yield _formatDomainSelection(map.get("domainAndUserSelection"));
                }

                yield DomainAndUserSelectInputElement._formatValue(comparedValue);
            }
            case AssignmentContextInputElementValue val -> val.getDomainAndUserSelection();
            default -> DomainAndUserSelectInputElement._formatValue(comparedValue);
        };

        if (comparedValues == null) {
            return Set.of();
        }

        return comparedValues
                .stream()
                .map(DomainAndUserSelectInputElementValue::toComparableKey)
                .collect(Collectors.toSet());
    }

    private static boolean isAllowedType(@Nullable String type) {
        return "orgUnit".equals(type) || "team".equals(type) || "user".equals(type);
    }

    private boolean isTypeEnabled(@Nullable String type) {
        if (!isAllowedType(type)) {
            return false;
        }

        if (allowedTypes == null) {
            return true;
        }

        return allowedTypes.contains(type);
    }

    @Nonnull
    private static String formatDisplayToken(@Nonnull DomainAndUserSelectInputElementValue token) {
        if ("orgUnit".equals(token.getType())) {
            return "Organisationseinheit (ID: " + token.getId() + ")";
        }

        if ("team".equals(token.getType())) {
            return "Team (ID: " + token.getId() + ")";
        }

        if ("user".equals(token.getType())) {
            return "Mitarbeiter:in (ID: " + token.getId() + ")";
        }

        return token.toComparableKey();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AssignmentContextInputElement that = (AssignmentContextInputElement) o;
        return Objects.equals(headline, that.headline) && Objects.equals(text, that.text) && Objects.equals(placeholder, that.placeholder) && Objects.equals(minItems, that.minItems) && Objects.equals(maxItems, that.maxItems) && Objects.equals(allowedTypes, that.allowedTypes) && Objects.equals(processAccessConstraint, that.processAccessConstraint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), headline, text, placeholder, minItems, maxItems, allowedTypes, processAccessConstraint);
    }

    @Nullable
    public String getHeadline() {
        return headline;
    }

    public AssignmentContextInputElement setHeadline(@Nullable String headline) {
        this.headline = headline;
        return this;
    }

    @Nullable
    public String getText() {
        return text;
    }

    public AssignmentContextInputElement setText(@Nullable String text) {
        this.text = text;
        return this;
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public AssignmentContextInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public Integer getMinItems() {
        return minItems;
    }

    public AssignmentContextInputElement setMinItems(@Nullable Integer minItems) {
        this.minItems = minItems;
        return this;
    }

    @Nullable
    public Integer getMaxItems() {
        return maxItems;
    }

    public AssignmentContextInputElement setMaxItems(@Nullable Integer maxItems) {
        this.maxItems = maxItems;
        return this;
    }

    @Nullable
    public List<String> getAllowedTypes() {
        return allowedTypes;
    }

    public AssignmentContextInputElement setAllowedTypes(@Nullable List<String> allowedTypes) {
        this.allowedTypes = allowedTypes;
        return this;
    }

    @Nullable
    public DomainAndUserSelectProcessAccessConstraint getProcessAccessConstraint() {
        return processAccessConstraint;
    }

    public AssignmentContextInputElement setProcessAccessConstraint(@Nullable DomainAndUserSelectProcessAccessConstraint processAccessConstraint) {
        this.processAccessConstraint = processAccessConstraint;
        return this;
    }
}
