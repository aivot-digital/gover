package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.prosuna.backend.elements.models.elements.form.input.StoragePathSelectorInputElementValue;
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
    public static final String DESTINATION_TYPE_CONFIG_KEY = "destination_type";
    public static final String DESTINATION_TYPE_OPTION_ONLINE_SERVICE = "online_service";
    public static final String DESTINATION_TYPE_OPTION_ADMINISTRATION = "administration";
    public static final String DESTINATION_ID_CONFIG_KEY = "destination_id";
    public static final String SUBSCRIBER_CLIENT_ID_CONFIG_KEY = "subscriber_client_id";
    public static final String SUBSCRIBER_CLIENT_SECRET_CONFIG_KEY = "subscriber_client_secret";
    public static final String PRIVATE_SIGNING_KEY_CONFIG_KEY = "private_signing_key";
    public static final String PRIVATE_DECRYPTION_KEYS_CONFIG_KEY = "private_decryption_keys";
    public static final String CALLBACK_SECRET_KEY = "callback_secret";
    public static final String COPY_TO_PROCESS_DATA_CONFIG_KEY = "copy_to_process_data";

    /** URL segment that identifies this trigger within a process version. */
    @InputElementPOJOBinding(id = SLUG_CONFIG_KEY, type = ElementType.Text, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "URL-Segment des Zustellpunkts"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Dieses Segment wird an den URL-Namespace des Prozesses angehängt."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
    })
    public String slug = "";

    /** FIT-Connect environment used for all subscriber API calls. */
    @InputElementPOJOBinding(id = ENVIRONMENT_CONFIG_KEY, type = ElementType.Select, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "FIT-Connect-Umgebung"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Die Umgebung, aus der Einreichungen abgerufen werden."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
    })
    public String environment = DEFAULT_ENVIRONMENT;

    /** Type of system represented by this trigger; a missing value is an invalid configuration. */
    @InputElementPOJOBinding(id = DESTINATION_TYPE_CONFIG_KEY, type = ElementType.Radio, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Art des Zustellpunkts"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie, ob der Trigger als Onlinedienst- oder Verwaltungs-Zustellpunkt verwendet wird."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
    })
    public String destinationType;

    /** Destination UUID expected in every callback handled by this trigger. */
    @InputElementPOJOBinding(id = DESTINATION_ID_CONFIG_KEY, type = ElementType.Text, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Zustellpunkt-ID"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "UUID des FIT-Connect-Zustellpunkts."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
    })
    public String destinationId;

    /** Client ID used to authenticate the subscriber against FIT-Connect. */
    @InputElementPOJOBinding(id = SUBSCRIBER_CLIENT_ID_CONFIG_KEY, type = ElementType.Text, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Subscriber-Client-ID"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Client-ID des FIT-Connect-Empfängers."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
    })
    public String subscriberClientId;

    /** Secret reference whose decrypted value is the subscriber client secret. */
    @InputElementPOJOBinding(id = SUBSCRIBER_CLIENT_SECRET_CONFIG_KEY, type = ElementType.SecretSelectInput, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Subscriber-Client-Secret"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Geheimnis des FIT-Connect-Empfängers."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
    })
    public String subscriberClientSecret;

    /** Asset containing the private signing JWK used for subscriber events; required only for administration destinations. */
    @Nullable
    @InputElementPOJOBinding(id = PRIVATE_SIGNING_KEY_CONFIG_KEY, type = ElementType.StoragePathSelector, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Privater Signaturschlüssel"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Asset-Datei mit dem privaten Signatur-JWK des Subscribers."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "allowReadOnlyStorageProviders", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
    })
    public StoragePathSelectorInputElementValue privateSigningKey;

    /** Private decryption JWKs; administration destinations require at least one entry, while online service destinations may leave the list empty. */
    @Nullable
    public List<PrivateDecryptionKeyConfig> privateDecryptionKeys = new LinkedList<>();

    /** Secret reference used exclusively to verify incoming callback HMACs. */
    @InputElementPOJOBinding(id = CALLBACK_SECRET_KEY, type = ElementType.SecretSelectInput, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Callback-Secret"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Geheimnis für die Authentifizierung eingehender Callback-Anfragen."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
    })
    public String callbackSecret;

    /** Whether an object-shaped JSON payload replaces the initial process data; null is treated as false. */
    @Nullable
    @InputElementPOJOBinding(id = COPY_TO_PROCESS_DATA_CONFIG_KEY, type = ElementType.Checkbox, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "JSON in Vorgangsdaten kopieren"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Kopiert die Eigenschaften des empfangenen JSON-Objekts in die Vorgangsdaten."),
            @ElementPOJOBindingProperty(key = "variant", strValue = "switch"),
            @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
    })
    public Boolean copyToProcessData = false;

    /** One asset reference containing a private decryption JWK. */
    @ReplicatingContainerLayoutElementElementPOJOBinding(id = PRIVATE_DECRYPTION_KEYS_CONFIG_KEY, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Private Entschlüsselungsschlüssel"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Hinterlegen Sie alle aktuell gültigen privaten Entschlüsselungs-JWKs."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "Entschlüsselungsschlüssel #"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Schlüssel hinzufügen"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Schlüssel entfernen"),
    })
    public static class PrivateDecryptionKeyConfig {
        public static final String KEY_FILE_CONFIG_KEY = "key_file";

        /** Asset containing a private decryption JWK; null is invalid for a persisted row. */
        @Nullable
        @InputElementPOJOBinding(id = KEY_FILE_CONFIG_KEY, type = ElementType.StoragePathSelector, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "JWK-Datei"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Asset-Datei mit einem privaten Entschlüsselungs-JWK."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "allowReadOnlyStorageProviders", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
        })
        public StoragePathSelectorInputElementValue keyFile;
    }
}
