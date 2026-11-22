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
        return "Name zum Konto";
    }

    @Override
    public String getAbstract() {
        return "Liest den hinterlegten vollständigen Namen eines internen Kontos anhand seiner ID.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Lesen Sie den vollständigen Namen eines internen Kontos, beispielsweise um ihn in einem Hinweis oder einer Zusammenfassung zu verwenden.

                # Welche Werte werden benötigt?
                **Konto-ID:** Die eindeutige ID des internen Kontos. Sie können die ID frei eingeben, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Eine Auswahl von Konten wird hier nicht angeboten.

                # Welches Ergebnis erhalten Sie?
                Den zum Konto gespeicherten vollständigen Namen als Text. Ist die ID leer oder das Konto nicht vorhanden, erhalten Sie keinen Wert (`null`).

                # Beispiel im No-Code-Editor
                1. Wählen Sie **„Name zum Konto“**.
                2. Verwenden Sie bei **„Konto-ID“** eine Wertquelle, die die ID des gewünschten Kontos liefert.
                3. Für ein Konto mit dem hinterlegten Namen `Erika Muster` erhalten Sie den Text `Erika Muster`.

                Zugeklappt und in der Vorschau lesen Sie beispielsweise: **Name zum Konto „Zugewiesenes Konto“**, wenn Sie eine Variable mit dieser Bezeichnung verwenden.

                # Was ist zu beachten?
                Die Eingabe muss die Konto-ID liefern, nicht den Namen oder eine vollständige Auswahl aus Organisationseinheiten, Teams und Personen. Die Funktion liest das gespeicherte Namensfeld; sie setzt keinen Namen aus anderen Angaben zusammen. Die Vorschau ruft den Namen nicht ab.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt den Namen";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.String,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Konto-ID",
                                "Die ID des internen Kontos, dessen hinterlegten vollständigen Namen Sie benötigen."
                        ).withSentence("zum Konto")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "Name zum Konto #0";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (userRepository == null) {
            throw new NoCodeException("Die Funktion „Name zum Konto“ ist hier nicht verfügbar.");
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
