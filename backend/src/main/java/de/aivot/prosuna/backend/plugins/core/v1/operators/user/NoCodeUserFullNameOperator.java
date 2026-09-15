package de.aivot.prosuna.backend.plugins.core.v1.operators.user;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import jakarta.annotation.Nullable;

public class NoCodeUserFullNameOperator extends NoCodeOperator {
    @Nullable
    private final UserRepository userRepository;

    public NoCodeUserFullNameOperator(@Nullable UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Deprecated
    public NoCodeUserFullNameOperator() {
        this(null);
    }

    @Override
    public String getIdentifier() {
        return "user-full-name";
    }

    @Override
    public String getLabel() {
        return "Mitarbeiter:innenname";
    }

    @Override
    public String getAbstract() {
        return "Gibt den vollständigen Namen einer Mitarbeiter:in anhand ihrer ID zurück.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Mitarbeiter:innenname“** gibt den vollständigen Namen einer Mitarbeiter:in anhand ihrer ID zurück. \s
                Wenn keine Mitarbeiter:in mit dieser ID gefunden wird oder die ID leer ist, wird **null** zurückgegeben.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten den Namen der Mitarbeiter:in anzeigen, die einem Vorgang zugeordnet ist. \s
                
                Mit dem Operator **„Mitarbeiter:innenname“** wird diese Logik so formuliert: \s
                `Mitarbeiter:innenname Zugewiesene Mitarbeiter:innen-ID`
                
                Beispielwerte: \s
                - **Mitarbeiter:innen-ID:** "8b4a1a1e-4a7d-4cf7-b6f8-2f99a2d7c7e1"
                
                **Ergebnis:**
                - **Rückgabewert:** "Max Mustermann"
                
                # Weitere Beispiele:
                - `Mitarbeiter:innenname 8b4a1a1e-4a7d-4cf7-b6f8-2f99a2d7c7e1` \s
                  **Ergebnis:** "Max Mustermann"
                
                # Wann verwenden Sie den Operator „Mitarbeiter:innenname“?
                Verwenden Sie **„Mitarbeiter:innenname“**, wenn Ihnen die ID einer Mitarbeiter:in vorliegt und Sie daraus den lesbaren vollständigen Namen ermitteln möchten, z. B. für Hinweise, Zusammenfassungen oder Benachrichtigungen.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.String,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Mitarbeiter:innen-ID",
                                "Die ID der Mitarbeiter:in, deren vollständiger Name ermittelt werden soll."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "ermittle den vollständigen Namen von „#0“";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (userRepository == null) {
            throw new NoCodeException("Der Operator „Mitarbeiter:innenname“ wurde ohne UserRepository initialisiert.");
        }

        var userId = castToString(args[0]);
        if (userId.isBlank()) {
            return new NoCodeResult(null);
        }

        var fullName = userRepository
                .findById(userId)
                .map(UserEntity::getFullName)
                .orElse(null);

        return new NoCodeResult(fullName);
    }
}
