package de.aivot.gover.backend.plugins.core.v1.operators.user;

import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.nocode.enums.NoCodeDataType;
import de.aivot.gover.backend.nocode.exceptions.NoCodeException;
import de.aivot.gover.backend.nocode.models.NoCodeOperator;
import de.aivot.gover.backend.nocode.models.NoCodeParameter;
import de.aivot.gover.backend.nocode.models.NoCodeResult;
import de.aivot.gover.backend.nocode.models.NoCodeSignatur;
import de.aivot.gover.backend.user.entities.UserEntity;
import de.aivot.gover.backend.user.repositories.UserRepository;
import jakarta.annotation.Nullable;

public class NoCodeUserEmailOperator extends NoCodeOperator {
    @Nullable
    private final UserRepository userRepository;

    public NoCodeUserEmailOperator(@Nullable UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Deprecated
    public NoCodeUserEmailOperator() {
        this(null);
    }

    @Override
    public String getIdentifier() {
        return "user-email";
    }

    @Override
    public String getLabel() {
        return "Mitarbeiter:innen-E-Mail";
    }

    @Override
    public String getAbstract() {
        return "Gibt die E-Mail-Adresse einer Mitarbeiter:in anhand ihrer ID zurück.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Mitarbeiter:innen-E-Mail“** gibt die E-Mail-Adresse einer Mitarbeiter:in anhand ihrer ID zurück. \s
                Wenn keine Mitarbeiter:in mit dieser ID gefunden wird oder die ID leer ist, wird **null** zurückgegeben.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten die E-Mail-Adresse der Mitarbeiter:in ermitteln, die einem Vorgang zugeordnet ist. \s
                
                Mit dem Operator **„Mitarbeiter:innen-E-Mail“** wird diese Logik so formuliert: \s
                `Mitarbeiter:innen-E-Mail Zugewiesene Mitarbeiter:innen-ID`
                
                Beispielwerte: \s
                - **Mitarbeiter:innen-ID:** "8b4a1a1e-4a7d-4cf7-b6f8-2f99a2d7c7e1"
                
                **Ergebnis:**
                - **Rückgabewert:** "max.mustermann@example.org"
                
                # Weitere Beispiele:
                - `Mitarbeiter:innen-E-Mail 8b4a1a1e-4a7d-4cf7-b6f8-2f99a2d7c7e1` \s
                  **Ergebnis:** "max.mustermann@example.org"
                
                # Wann verwenden Sie den Operator „Mitarbeiter:innen-E-Mail“?
                Verwenden Sie **„Mitarbeiter:innen-E-Mail“**, wenn Ihnen die ID einer Mitarbeiter:in vorliegt und Sie daraus die E-Mail-Adresse ermitteln möchten, z. B. für Benachrichtigungen, Prüfungen oder Ausgaben.
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
                                "Die ID der Mitarbeiter:in, deren E-Mail-Adresse ermittelt werden soll."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "ermittle die E-Mail-Adresse von „#0“";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (userRepository == null) {
            throw new NoCodeException("Der Operator „Mitarbeiter:innen-E-Mail“ wurde ohne UserRepository initialisiert.");
        }

        var userId = castToString(args[0]);
        if (userId.isBlank()) {
            return new NoCodeResult(null);
        }

        var email = userRepository
                .findById(userId)
                .map(UserEntity::getEmail)
                .orElse(null);

        return new NoCodeResult(email);
    }
}
