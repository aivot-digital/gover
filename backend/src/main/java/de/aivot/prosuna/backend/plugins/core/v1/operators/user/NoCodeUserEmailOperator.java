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
        return "E-Mail-Adresse zum Konto";
    }

    @Override
    public String getAbstract() {
        return "Liest die hinterlegte E-Mail-Adresse eines internen Kontos anhand seiner ID.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Lesen Sie die hinterlegte E-Mail-Adresse eines internen Kontos, beispielsweise um sie in einem weiteren Ausdruck zu verwenden.

                # Welche Werte werden benötigt?

                - **Konto-ID:** Die eindeutige ID des internen Kontos. Sie können die ID frei eingeben, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Eine Auswahl von Konten wird hier nicht angeboten.

                # Welches Ergebnis erhalten Sie?

                Die zum Konto gespeicherte E-Mail-Adresse als Text. Ist die ID leer oder das Konto nicht vorhanden, erhalten Sie keinen Wert (`null`).

                # Beispiel im No-Code-Editor

                1. Wählen Sie **„E-Mail-Adresse zum Konto“**.
                2. Verwenden Sie bei **„Konto-ID“** eine Wertquelle, die die ID des gewünschten Kontos liefert.

                Für ein Konto mit der hinterlegten E-Mail-Adresse `erika.muster@example.org` erhalten Sie den Text `erika.muster@example.org`.

                Zugeklappt und in der Vorschau lesen Sie beispielsweise: `E-Mail-Adresse zum Konto „Zugewiesenes Konto“`, wenn Sie eine Variable mit dieser Bezeichnung verwenden.

                # Was ist zu beachten?

                Die Eingabe muss die Konto-ID liefern, nicht den Namen oder eine vollständige Auswahl aus Organisationseinheiten, Teams und Personen. Die Funktion verwendet die bei der Ausführung hinterlegte Adresse; die Vorschau ruft sie nicht ab.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt die E-Mail-Adresse";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.String,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Konto-ID",
                                "Die ID des internen Kontos, dessen hinterlegte E-Mail-Adresse Sie benötigen."
                        ).withSentence("zum Konto")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "E-Mail-Adresse zum Konto #0";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (userRepository == null) {
            throw new NoCodeException("Die Funktion „E-Mail-Adresse zum Konto“ ist hier nicht verfügbar.");
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
