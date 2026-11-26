package de.aivot.prosuna.backend.plugins.core.v1.operators.secrets;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import jakarta.annotation.Nullable;

import java.util.UUID;

public class NoCodeSecretsGetOperator extends NoCodeOperator {
    @Nullable
    private final SecretService secretService;

    public NoCodeSecretsGetOperator(@Nullable SecretService secretService) {
        this.secretService = secretService;
    }

    @Deprecated
    public NoCodeSecretsGetOperator() {
        this(null);
    }

    @Override
    public String getIdentifier() {
        return "secrets-get";
    }

    @Override
    public String getLabel() {
        return "Geheimnis abrufen";
    }

    @Override
    public String getAbstract() {
        return "Gibt den entschlüsselten Wert eines Geheimnisses anhand seines Schlüssels zurück.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Verwenden Sie den gespeicherten Wert eines Geheimnisses in einem Ausdruck, beispielsweise einen API-Schlüssel für eine Anbindung.

                # Welche Werte werden benötigt?

                - **Geheimnis-Schlüssel:** Die ID des gespeicherten Geheimnisses. Sie können den Schlüssel im Bereich **„Geheimnisse“** kopieren und hier frei eintragen, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Eine Auswahl von Geheimnissen wird hier nicht angeboten.

                # Welches Ergebnis erhalten Sie?

                Den entschlüsselten Wert des Geheimnisses als Text. Ist der Schlüssel leer oder kein Geheimnis dazu vorhanden, erhalten Sie keinen Wert (`null`).

                # Beispiel im No-Code-Editor

                1. Wählen Sie **„Geheimnis abrufen“**.
                2. Kopieren Sie den Schlüssel eines vorhandenen Geheimnisses aus **„Geheimnisse“**.
                3. Legen Sie diesen Schlüssel bei **„Geheimnis-Schlüssel“** selbst fest.

                Für ein Geheimnis mit dem Schlüssel `8b4a1a1e-4a7d-4cf7-b6f8-2f99a2d7c7e1` und dem beispielhaften Inhalt `test-token` erhalten Sie bei der Ausführung den Text `test-token`.

                Zugeklappt und in der Vorschau lesen Sie: `Wert des Geheimnisses mit Schlüssel „8b4a1a1e-4a7d-4cf7-b6f8-2f99a2d7c7e1“`.

                # Was ist zu beachten?

                Geben Sie den Schlüssel ein, nicht den Namen oder den Inhalt des Geheimnisses. Ein ungültiger Schlüssel oder ein Fehler beim Entschlüsseln führt zu einer Fehlermeldung. Die Vorschau zeigt den zusammengestellten Ausdruck und ruft den geheimen Wert nicht ab.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt den gespeicherten Wert";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.String,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Geheimnis-Schlüssel",
                                "Die ID des Geheimnisses aus dem Bereich „Geheimnisse“. Sie können den Schlüssel dort kopieren."
                        ).withSentence("mit dem Schlüssel")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "Wert des Geheimnisses mit Schlüssel #0";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (secretService == null) {
            throw new NoCodeException("Die Funktion „Geheimnis abrufen“ ist hier nicht verfügbar.");
        }

        if (args.length != 1) {
            throw new NoCodeWrongArgumentCountException(1, args.length);
        }

        var key = castToString(args[0]);
        if (key.isBlank()) {
            return new NoCodeResult(null);
        }

        UUID secretKey;
        try {
            secretKey = UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            throw new NoCodeException("Der Geheimnis-Schlüssel ist ungültig. Prüfen Sie den kopierten Schlüssel aus „Geheimnisse“.");
        }

        var secret = secretService
                .retrieve(secretKey)
                .orElse(null);

        if (secret == null) {
            return new NoCodeResult(null);
        }

        try {
            return new NoCodeResult(secretService.decrypt(secret));
        } catch (Exception e) {
            throw new NoCodeException("Das Geheimnis mit dem Schlüssel „" + key + "“ konnte nicht entschlüsselt werden.");
        }
    }
}
