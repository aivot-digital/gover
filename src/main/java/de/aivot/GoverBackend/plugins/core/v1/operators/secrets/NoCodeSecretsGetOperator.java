package de.aivot.GoverBackend.plugins.core.v1.operators.secrets;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;
import de.aivot.GoverBackend.secrets.services.SecretService;
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
                # Beschreibung:
                Der Operator **„Geheimnis abrufen“** gibt den entschlüsselten Wert eines gespeicherten Geheimnisses anhand seines Schlüssels zurück. \s
                Wenn kein Geheimnis mit diesem Schlüssel gefunden wird oder der Schlüssel leer ist, wird **null** zurückgegeben.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten einen API-Schlüssel aus den gespeicherten Geheimnissen in einer No-Code-Regel verwenden.
                
                Mit dem Operator **„Geheimnis abrufen“** wird diese Logik so formuliert: \s
                `Geheimnis abrufen Geheimnis-Schlüssel`
                
                Beispielwerte: \s
                - **Geheimnis-Schlüssel:** "8b4a1a1e-4a7d-4cf7-b6f8-2f99a2d7c7e1"
                
                **Ergebnis:**
                - **Rückgabewert:** "mein-geheimer-wert"
                
                # Wann verwenden Sie den Operator „Geheimnis abrufen“?
                Verwenden Sie **„Geheimnis abrufen“**, wenn eine No-Code-Regel einen sensiblen Wert benötigt, der in der Geheimnisverwaltung gespeichert ist.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.String,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Geheimnis-Schlüssel",
                                "Der Schlüssel des Geheimnisses, dessen entschlüsselter Wert abgerufen werden soll."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "rufe das Geheimnis „#0“ ab";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (secretService == null) {
            throw new NoCodeException("Der Operator „Geheimnis abrufen“ wurde ohne SecretService initialisiert.");
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
            throw new NoCodeException("Ungültiger Geheimnis-Schlüssel: " + key);
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
            throw new NoCodeException("Das Geheimnis konnte nicht entschlüsselt werden: " + key);
        }
    }
}
