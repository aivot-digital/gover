package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.webhook;

import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nullable;


@LayoutElementPOJOBinding(id = WebhookTriggerNodeV1.NODE_KEY, type = ElementType.ConfigLayout)
public class WebhookTriggerConfigV1 {
    // The basic slug for this webhook node
    public static final String SLUG_CONFIG_KEY = "slug";

    // The allowed request method for this node
    public static final String REQUEST_METHOD_CONFIG_KEY = "request_method";
    public static final String REQUEST_METHOD_OPTION_GET = "GET";
    public static final String REQUEST_METHOD_OPTION_POST = "POST";
    public static final String REQUEST_METHOD_OPTION_PUT = "PUT";
    public static final String REQUEST_METHOD_OPTION_PATCH = "PATCH";
    public static final String REQUEST_METHOD_OPTION_DELETE = "DELETE";

    // Flag if authentication is required
    public static final String AUTH_REQUIRED_CONFIG_KEY = "auth_required";
    // Authentication configuration group
    public static final String AUTH_CONFIG_GROUP_ID = "auth_config";
    public static final String AUTH_METHOD_CONFIG_KEY = "auth_method";
    // Options for authentication method
    public static final String AUTH_METHOD_OPTION_BASIC = "basic";
    // public static final String AUTH_METHOD_OPTION_BEARER = "bearer"; // Bearer is removed because it clashes with the spring boot jwt bearer token handling.
    public static final String AUTH_METHOD_OPTION_QUERY_PARAM = "query_param";
    // Basic auth credentials
    public static final String AUTH_USERNAME_CONFIG_KEY = "auth_username";
    public static final String AUTH_PASSWORD_CONFIG_KEY = "auth_password";
    // Token passed as query parameter
    public static final String AUTH_TOKEN_CONFIG_KEY = "auth_token";

    // Additional request body configuration group
    public static final String REQUEST_BODY_CONFIG_GROUP_ID = "request_body_config";
    // Request body type
    public static final String REQUEST_BODY_TYPE_CONFIG_KEY = "request_body_type";
    // Options for request body type
    public static final String REQUEST_BODY_TYPE_OPTION_JSON = "json";
    public static final String REQUEST_BODY_TYPE_OPTION_FORM = "form";
    public static final String REQUEST_BODY_TYPE_OPTION_XML = "xml";

    // Flag to copy data to process data
    public static final String COPY_TO_PROCESS_DATA_CONFIG_KEY = "copy_to_process_data";

    @Nullable
    @InputElementPOJOBinding(id = REQUEST_METHOD_CONFIG_KEY, type = ElementType.Select, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "HTTP-Methode"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie dieselbe HTTP-Methode, die das aufrufende System verwendet."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
    })
    public String requestMethod = REQUEST_METHOD_OPTION_POST;

    @Nullable
    public WebhookRequestBodyConfig requestBodyConfig;

    @Nullable
    @InputElementPOJOBinding(id = SLUG_CONFIG_KEY, type = ElementType.Text, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "URL-Segment des Webhooks"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Legt den Namen dieses Webhooks innerhalb der Adresse fest. Die vollständige Adresse hängt von HTTP-Methode und Datenformat ab; Sie können sie am Feld kopieren."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "destinationKey", strValue = WebhookTriggerConfigV1.SLUG_CONFIG_KEY),
    })
    public String slug = "";

    @Nullable
    @InputElementPOJOBinding(id = AUTH_REQUIRED_CONFIG_KEY, type = ElementType.Checkbox, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Authentifizierung erforderlich"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Nur Aufrufe mit den hier hinterlegten Zugangsdaten können einen Vorgang starten. Ohne diese Option sind Aufrufe ohne Zugangsdaten möglich."),
            @ElementPOJOBindingProperty(key = "variant", strValue = "switch"),
    })
    public Boolean authRequired = false;

    @Nullable
    public WebhookConfigAuth authConfig;

    @LayoutElementPOJOBinding(id = AUTH_CONFIG_GROUP_ID, type = ElementType.GroupLayout)
    public static class WebhookConfigAuth {
        @Nullable
        @InputElementPOJOBinding(id = AUTH_METHOD_CONFIG_KEY, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Authentifizierungsmethode"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Legen Sie fest, wie das andere System die Zugangsdaten mitsendet."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String authMethod;

        @Nullable
        @InputElementPOJOBinding(id = AUTH_USERNAME_CONFIG_KEY, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Benutzername"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der für diesen Webhook vereinbarte Benutzername für Basic Auth."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String authUsername;

        @Nullable
        @InputElementPOJOBinding(id = AUTH_PASSWORD_CONFIG_KEY, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Passwort"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Das für diesen Webhook vereinbarte Passwort für Basic Auth."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String authPassword;

        @Nullable
        @InputElementPOJOBinding(id = AUTH_TOKEN_CONFIG_KEY, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Token"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Das aufrufende System muss diesen Wert im URL-Parameter „token“ mitsenden."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String authToken;
    }

    @LayoutElementPOJOBinding(id = REQUEST_BODY_CONFIG_GROUP_ID, type = ElementType.GroupLayout)
    public static class WebhookRequestBodyConfig {
        @Nullable
        @InputElementPOJOBinding(id = REQUEST_BODY_TYPE_CONFIG_KEY, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Datenformat"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie das Format, in dem das andere System die Daten überträgt. Die kopierte Webhook-Adresse enthält den passenden Zusatz."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String requestBodyType;

        @Nullable
        @InputElementPOJOBinding(id = COPY_TO_PROCESS_DATA_CONFIG_KEY, type = ElementType.Checkbox, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Daten in Vorgangsdaten übernehmen"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Übernimmt die Werte aus dem Anfrageinhalt zusätzlich in die Vorgangsdaten. Ohne diese Option bleiben die Vorgangsdaten leer; die Werte stehen weiterhin in den Elementdaten bereit."),
                @ElementPOJOBindingProperty(key = "variant", strValue = "switch"),
        })
        public Boolean copyToProcessData;
    }
}
