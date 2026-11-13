package de.aivot.prosuna.backend.plugins.core.v1.operators.user;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DomainAndUserSelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DomainAndUserSelectInputElementValue;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compares domain and user selections by their {@code type:id} key, like the legacy Includes condition of the
 * domain/user selection and assignment context fields. Whole-object equality would fail as soon as one side carries
 * additional fields, and a static operand can only be authored as text.
 */
public class NoCodeSelectionIncludesOperator extends NoCodeOperator {
    private static final String ASSIGNMENT_CONTEXT_SELECTION_KEY = "domainAndUserSelection";

    @Override
    public String getIdentifier() {
        return "selection-includes";
    }

    @Override
    public String getLabel() {
        return "Auswahl beinhaltet";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob eine Auswahl von Organisationseinheiten, Teams und Personen bestimmte Einträge enthält.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Auswahl beinhaltet“** prüft, ob eine Auswahl aus einem Feld zur Auswahl von Organisationseinheiten, Teams und Personen \s
                oder aus einem Zuweisungskontext alle gesuchten Einträge enthält. Die Auswahl darf weitere Einträge enthalten.

                Die gesuchten Einträge können aus einem anderen Auswahlfeld stammen oder als fester Wert angegeben werden. \s
                Ein fester Wert hat die Form `Typ:ID`, zum Beispiel `user:4f2c…`, `orgUnit:12` oder `team:7`.

                # Beispiele:
                - Auswahl enthält `orgUnit:12` und `user:abc`, gesucht wird `orgUnit:12` → **wahr**
                - Auswahl enthält `orgUnit:12`, gesucht wird `team:7` → **falsch**
                - Auswahl ist leer → **falsch**

                Um zu prüfen, dass ein Eintrag nicht ausgewählt ist, kombinieren Sie den Operator mit **„Nicht“**.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Auswahl",
                                "Die Auswahl oder der Zuweisungskontext, in dem gesucht werden soll."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Gesuchte Einträge",
                                "Ein Eintrag in der Form Typ:ID oder eine Auswahl, deren Einträge alle enthalten sein müssen."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "„#0“ beinhaltet „#1“";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var selectedKeys = toComparableKeys(unwrapAssignmentContext(args[0]));
        var searchedKeys = toComparableKeys(args[1]);

        // Legacy Includes is false when either side is empty.
        if (selectedKeys.isEmpty() || searchedKeys.isEmpty()) {
            return new NoCodeResult(false);
        }

        return new NoCodeResult(selectedKeys.containsAll(searchedKeys));
    }

    @Nullable
    private static Object unwrapAssignmentContext(@Nullable Object value) {
        return switch (value) {
            case AssignmentContextInputElementValue context -> context.getDomainAndUserSelection();
            case Map<?, ?> map when map.containsKey(ASSIGNMENT_CONTEXT_SELECTION_KEY) -> map.get(ASSIGNMENT_CONTEXT_SELECTION_KEY);
            case null, default -> value;
        };
    }

    @Nonnull
    private static Set<String> toComparableKeys(@Nullable Object value) {
        var values = DomainAndUserSelectInputElement._formatValue(value);
        if (values == null) {
            return Set.of();
        }

        return values
                .stream()
                .map(DomainAndUserSelectInputElementValue::toComparableKey)
                .collect(Collectors.toSet());
    }
}
