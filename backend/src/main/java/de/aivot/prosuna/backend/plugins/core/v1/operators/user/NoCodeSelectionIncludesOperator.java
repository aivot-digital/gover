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
        return "Auswahl enthält Einträge";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob eine Auswahl alle gesuchten Organisationseinheiten, Teams oder Personen enthält.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Prüfen Sie, ob eine Auswahl bestimmte Organisationseinheiten, Teams oder Personen enthält, beispielsweise ein zuständiges Team. Auch die Auswahl aus einem Zuweisungskontext kann geprüft werden.

                # Welche Werte werden benötigt?

                - **Auswahl:** Die zu prüfenden Einträge, beispielsweise aus einem Formularelement oder einem Zuweisungskontext.
                - **Gesuchte Einträge:** Ein einzelner gesuchter Eintrag oder eine weitere Auswahl, deren Einträge alle enthalten sein sollen.

                Beide Eingaben können aus einer anderen Wertquelle oder einer Funktion stammen. Einen einzelnen Eintrag können Sie auch frei als Text eingeben: `orgUnit:12` für die Organisationseinheit mit ID `12`, `team:7` für das Team mit ID `7` oder `user:abc` für das Konto mit ID `abc`. Eine Auswahl mit Namen wird hier nicht angeboten. Für mehrere Einträge verwenden Sie eine Wertquelle oder eine Funktion, die eine entsprechende Auswahl liefert.

                # Welches Ergebnis erhalten Sie?

                `Wahr`, wenn alle gesuchten Einträge in der Auswahl enthalten sind. Die Auswahl darf weitere Einträge enthalten. Sind Einträge nicht enthalten oder ist eine der beiden Auswahlen leer, erhalten Sie `Falsch`.

                # Beispiel im No-Code-Editor

                1. Wählen Sie **„Auswahl enthält Einträge“**.
                2. Verwenden Sie bei **„Auswahl“** das Formularelement **„Zuständige Teams“**.
                3. Legen Sie bei **„Gesuchte Einträge“** den Text `team:7` selbst fest.

                Enthält das Formularelement das Team mit ID `7`, ist das Ergebnis `Wahr`. Zugeklappt und in der Vorschau lesen Sie: `Wert von „Zuständige Teams“ enthält alle Einträge aus „team:7“`.

                # Was ist zu beachten?

                Verglichen werden Art und ID eines Eintrags, nicht seine sichtbare Bezeichnung. Eine Organisationseinheit und ein Team mit derselben ID gelten als unterschiedliche Einträge. Die Reihenfolge und mehrfache Vorkommen desselben Eintrags sind für den Vergleich unerheblich.

                Einträge ohne ID oder ohne unterstützte Art werden beim Vergleich ausgelassen. Enthält eine der beiden Auswahlen dadurch keinen gültigen Eintrag, erhalten Sie `Falsch`.

                Mit **„Nicht“** können Sie prüfen, ob nicht alle gesuchten Einträge enthalten sind. Bei mehreren gesuchten Einträgen bedeutet das nicht, dass keiner davon enthalten ist.
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
                        ).withSentence("wenn die Auswahl"),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Gesuchte Einträge",
                                "Ein Eintrag in der Form Typ:ID oder eine Auswahl, deren Einträge alle enthalten sein müssen."
                        ).withSentence("alle Einträge aus", "enthält")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 enthält alle Einträge aus #1";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var selectedKeys = toComparableKeys(unwrapAssignmentContext(args[0]));
        var searchedKeys = toComparableKeys(unwrapAssignmentContext(args[1]));

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
                .filter(entry -> DomainAndUserSelectInputElement.isSupportedType(entry.getType()))
                .map(DomainAndUserSelectInputElementValue::toComparableKey)
                .collect(Collectors.toSet());
    }

    @Nonnull
    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }
}
