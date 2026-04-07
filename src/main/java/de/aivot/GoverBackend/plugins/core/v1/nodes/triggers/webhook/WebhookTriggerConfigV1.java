package de.aivot.GoverBackend.plugins.core.v1.nodes.triggers.webhook;

import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.enums.ElementType;
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
    public static final String AUTH_METHOD_OPTION_BEARER = "bearer";
    public static final String AUTH_METHOD_OPTION_QUERY_PARAM = "query_param";
    // Basic auth credentials
    public static final String AUTH_USERNAME_CONFIG_KEY = "auth_username";
    public static final String AUTH_PASSWORD_CONFIG_KEY = "auth_password";
    // Bearer or query param token
    public static final String AUTH_TOKEN_CONFIG_KEY = "auth_token";

    // Additional request body configuration group
    public static final String REQUEST_BODY_CONFIG_GROUP_ID = "request_body_config";
    // Request body type
    public static final String REQUEST_BODY_TYPE_CONFIG_KEY = "request_body_type";
    // Options for request body type
    public static final String REQUEST_BODY_TYPE_OPTION_JSON = "json";
    public static final String REQUEST_BODY_TYPE_OPTION_FORM = "form";
    public static final String REQUEST_BODY_TYPE_OPTION_XML = "xml";

    // Processing configuration. Weather if automatic or code-based processing is used
    public static final String PROCESSING_TYPE_CONFIG_KEY = "processing_type";
    // Options for processing type
    public static final String PROCESSING_TYPE_OPTION_AUTOMATIC = "automatic";
    public static final String PROCESSING_TYPE_OPTION_CODE = "code";
    // Field for the code to process the incoming data
    public static final String PROCESSING_CODE_CONFIG_KEY = "processing_code";

    // Flag to copy data to process data
    public static final String COPY_TO_PROCESS_DATA_CONFIG_KEY = "copy_to_process_data";

    @Nullable
    @InputElementPOJOBinding(id = SLUG_CONFIG_KEY, type = ElementType.Text, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Webhook-URL"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Die URL, über die der Webhook angesprochen werden kann."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
    })
    public String slug = "";

    @Nullable
    @InputElementPOJOBinding(id = REQUEST_METHOD_CONFIG_KEY, type = ElementType.Select, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "HTTP-Methode"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Die HTTP-Methode, die für den Webhook verwendet werden soll."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
    })
    public String requestMethod = REQUEST_METHOD_OPTION_POST;

    @Nullable
    public WebhookRequestBodyConfig requestBodyConfig;

    @Nullable
    @InputElementPOJOBinding(id = AUTH_REQUIRED_CONFIG_KEY, type = ElementType.Checkbox, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Authentifizierung erforderlich"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie an, ob für den Zugriff auf den Webhook eine Authentifizierung erforderlich ist."),
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
                @ElementPOJOBindingProperty(key = "hint", strValue = "Die Methode der Authentifizierung, die für den Webhook verwendet werden soll."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String authMethod;

        @Nullable
        @InputElementPOJOBinding(id = AUTH_USERNAME_CONFIG_KEY, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Benutzername"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Benutzername für die Basic-Authentifizierung."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String authUsername;

        @Nullable
        @InputElementPOJOBinding(id = AUTH_PASSWORD_CONFIG_KEY, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Passwort"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Das Passwort für die Basic-Authentifizierung."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String authPassword;

        @Nullable
        @InputElementPOJOBinding(id = AUTH_TOKEN_CONFIG_KEY, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Token"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Das Token für die Bearer-Authentifizierung oder als Query-Parameter."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String authToken;
    }

    @LayoutElementPOJOBinding(id = REQUEST_BODY_CONFIG_GROUP_ID, type = ElementType.GroupLayout)
    public static class WebhookRequestBodyConfig {
        @Nullable
        @InputElementPOJOBinding(id = REQUEST_BODY_TYPE_CONFIG_KEY, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Request Body Typ"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Typ des Request Bodys, der für den Webhook verwendet werden soll."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String requestBodyType;

        @Nullable
        @InputElementPOJOBinding(id = PROCESSING_TYPE_CONFIG_KEY, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verarbeitungsart"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Die Art der Verarbeitung der empfangenen Webhook-Daten."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String processingType;

        @Nullable
        @InputElementPOJOBinding(id = PROCESSING_CODE_CONFIG_KEY, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verarbeitungscode"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der JavaScript-Code, der zur Verarbeitung der empfangenen Webhook-Daten verwendet werden soll."),
        })
        public String processingCode;

        @Nullable
        @InputElementPOJOBinding(id = COPY_TO_PROCESS_DATA_CONFIG_KEY, type = ElementType.Checkbox, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Daten in Vorgangsdaten kopieren"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie an, ob die empfangenen Webhook-Daten in die Vorgangsdaten kopiert werden sollen."),
                @ElementPOJOBindingProperty(key = "variant", strValue = "switch"),
        })
        public Boolean copyToProcessData;
    }
}
