package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;

/** Configuration required to receive and import FIT-Connect submissions. */
@LayoutElementPOJOBinding(id = FitConnectTriggerNodeV1.NODE_KEY, type = ElementType.ConfigLayout)
public class FitConnectTriggerConfigV1 {
    public static final String DEFAULT_ENVIRONMENT = "TEST";

    public static final String SLUG_CONFIG_KEY = "slug";
    public static final String ENVIRONMENT_CONFIG_KEY = "environment";
    public static final String DESTINATION_ID_CONFIG_KEY = "destination_id";
    public static final String SUBSCRIBER_CLIENT_ID_CONFIG_KEY = "subscriber_client_id";
    public static final String SUBSCRIBER_CLIENT_SECRET_CONFIG_KEY = "subscriber_client_secret";
    public static final String PRIVATE_SIGNING_KEY_CONFIG_KEY = "private_signing_key";
    public static final String PRIVATE_DECRYPTION_KEYS_CONFIG_KEY = "private_decryption_keys";
    public static final String CALLBACK_SECRET_KEY = "callback_secret";
    public static final String COPY_TO_PROCESS_DATA_CONFIG_KEY = "copy_to_process_data";

    /** URL segment that identifies this trigger within a process version. */
    @InputElementPOJOBinding(id = SLUG_CONFIG_KEY, type = ElementType.Text, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Callback-URL (URL-Segment)"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Legen Sie hier den letzten Teil der Callback-URL fest. Kopieren Sie über das Kopiersymbol die vollständige Adresse und hinterlegen Sie diese im FIT-Connect-Zustellpunkt als Callback-URL."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
    })
    public String slug = "";

    /** Secret reference used exclusively to verify incoming callback HMACs. */
    @InputElementPOJOBinding(id = CALLBACK_SECRET_KEY, type = ElementType.SecretSelectInput, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Callback-Secret"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie ein in Prosuna gespeichertes Geheimnis und hinterlegen Sie dessen Wert auch im FIT-Connect-Zustellpunkt als Callback-Secret. Auf beiden Seiten muss derselbe Wert verwendet werden."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
    })
    public String callbackSecret;

    /** FIT-Connect environment used for all organisation API calls. */
    @InputElementPOJOBinding(id = ENVIRONMENT_CONFIG_KEY, type = ElementType.Select, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "FIT-Connect-Umgebung"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie die Umgebung, in der der Zustellpunkt eingerichtet ist. Der Prosuna-Testmodus ändert diese Auswahl nicht."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
    })
    public String environment = DEFAULT_ENVIRONMENT;

    /** Destination UUID expected in every callback handled by this trigger. */
    @InputElementPOJOBinding(id = DESTINATION_ID_CONFIG_KEY, type = ElementType.Text, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Zustellpunkt-ID"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Die bei FIT-Connect vergebene Kennung des Zustellpunkts im UUID-Format."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
    })
    public String destinationId;

    /** Client ID used to authenticate the receiving organisation against FIT-Connect. */
    @InputElementPOJOBinding(id = SUBSCRIBER_CLIENT_ID_CONFIG_KEY, type = ElementType.Text, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Client-ID"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Die Client-ID des empfangenden Systems (Subscriber-Client), die Sie in FIT-Connect unter den Zugangsdaten finden."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
    })
    public String subscriberClientId;

    /** Secret reference whose decrypted value is the receiving organisation's client secret. */
    @InputElementPOJOBinding(id = SUBSCRIBER_CLIENT_SECRET_CONFIG_KEY, type = ElementType.SecretSelectInput, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Client-Secret"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie das zur Client-ID gehörende FIT-Connect-Geheimnis aus. Dieses wird für den Zugriff auf FIT-Connect verwendet und ist vom Callback-Secret getrennt."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
    })
    public String subscriberClientSecret;

    /** Asset containing the private signing JWK used for organisation events. */
    @InputElementPOJOBinding(id = PRIVATE_SIGNING_KEY_CONFIG_KEY, type = ElementType.AssetSelectInput, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Privater Signaturschlüssel"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie die private Signaturschlüsseldatei im JWK-Format (JSON Web Key). Prosuna signiert damit Nachrichten an FIT-Connect."),
            @ElementPOJOBindingProperty(key = "dialogTitle", strValue = "Signaturschlüssel auswählen"),
            @ElementPOJOBindingProperty(key = "placeholder", strValue = "Keine JWK-Datei ausgewählt"),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
    })
    public String privateSigningKey;

    /** Private decryption JWKs; at least one entry is required and multiple entries support key rollover. */
    public List<PrivateDecryptionKeyConfig> privateDecryptionKeys = new LinkedList<>();

    /** Whether an object-shaped JSON payload replaces the initial process data; null is treated as false. */
    @Nullable
    @InputElementPOJOBinding(id = COPY_TO_PROCESS_DATA_CONFIG_KEY, type = ElementType.Checkbox, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Fachdaten in Vorgangsdaten übernehmen"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Übernimmt die Eigenschaften der JSON-Fachdaten zusätzlich in die Vorgangsdaten. Dafür ist ein JSON-Objekt erforderlich. Ohne diese Option bleiben die Fachdaten in den Elementdaten verfügbar und die Vorgangsdaten leer."),
            @ElementPOJOBindingProperty(key = "variant", strValue = "switch"),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
    })
    public Boolean copyToProcessData = false;

    /** One asset reference containing a private decryption JWK. */
    @ReplicatingContainerLayoutElementElementPOJOBinding(id = PRIVATE_DECRYPTION_KEYS_CONFIG_KEY, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Private Entschlüsselungsschlüssel"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Hinterlegen Sie mindestens eine passende private Entschlüsselungsschlüsseldatei im JWK-Format. Mehrere Einträge ermöglichen den Empfang während eines Schlüsselwechsels."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "Entschlüsselungsschlüssel #"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Schlüssel hinzufügen"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Schlüssel entfernen"),
    })
    public static class PrivateDecryptionKeyConfig {
        public static final String KEY_FILE_CONFIG_KEY = "key_file";

        /** Asset containing a private decryption JWK; null is invalid for a persisted row. */
        @Nullable
        @InputElementPOJOBinding(id = KEY_FILE_CONFIG_KEY, type = ElementType.AssetSelectInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "JWK-Datei"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie die private JWK-Datei zum Entschlüsseln der Einreichungen."),
                @ElementPOJOBindingProperty(key = "dialogTitle", strValue = "Entschlüsselungsschlüssel auswählen"),
                @ElementPOJOBindingProperty(key = "placeholder", strValue = "Keine JWK-Datei ausgewählt"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
        })
        public String keyFile;
    }
}
