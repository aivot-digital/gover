package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import de.aivot.gover.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.gover.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.gover.backend.enums.ElementType;

import java.util.*;

@LayoutElementPOJOBinding(id = HttpActionNodeV1Config.HTTP_CONFIG, type = ElementType.ConfigLayout)
public class HttpActionNodeV1Config {
    public static final String HTTP_CONFIG = "config";
    
    public static final String HTTP_METHOD_OPT_GET = "GET";
    public static final String HTTP_METHOD_OPT_POST = "POST";
    public static final String HTTP_METHOD_OPT_PUT = "PUT";
    public static final String HTTP_METHOD_OPT_PATCH = "PATCH";
    public static final String HTTP_METHOD_OPT_DELETE = "DELETE";
    public static final String HTTP_METHOD_FIELD_ID = "httpMethod";
    @InputElementPOJOBinding(id = HTTP_METHOD_FIELD_ID , type = ElementType.Select, properties = {})
    public String httpMethod;
    
    public static final String URL_FIELD_ID = "url";
    @InputElementPOJOBinding(id = URL_FIELD_ID , type = ElementType.Text, properties = {})
    public String url;
    
    public static final String AUTH_METHOD_OPT_BASIC_NUTZERNAME_UND_PASSWORT = "basic";
    public static final String AUTH_METHOD_OPT_BEARER_HEADERTOKEN = "bearer";
    public static final String AUTH_METHOD_FIELD_ID = "authMethod";
    @InputElementPOJOBinding(id = AUTH_METHOD_FIELD_ID , type = ElementType.Select, properties = {})
    public String authMethod;
    
    public static final String BASIC_AUTH_CONFIG = "gp_nuIGFl68sm";
    @LayoutElementPOJOBinding(id = BASIC_AUTH_CONFIG, type = ElementType.GroupLayout)
    public static class BasicAuthConfig {
        public static final String USERNAME_FIELD_ID = "username";
        @InputElementPOJOBinding(id = USERNAME_FIELD_ID , type = ElementType.Text, properties = {})
        public String username;
        
        public static final String PASSWORD_SECRET_KEY_FIELD_ID = "passwordSecretKey";
        @InputElementPOJOBinding(id = PASSWORD_SECRET_KEY_FIELD_ID , type = ElementType.Select, properties = {})
        public String passwordSecretKey;
        
    }
    public BasicAuthConfig basicAuthConfig;
    
    public static final String BEARER_AUTH_CONFIG = "gp_335S8aTN0c";
    @LayoutElementPOJOBinding(id = BEARER_AUTH_CONFIG, type = ElementType.GroupLayout)
    public static class BearerAuthConfig {
        public static final String BEARER_TOKEN_FIELD_ID = "bearerToken";
        @InputElementPOJOBinding(id = BEARER_TOKEN_FIELD_ID , type = ElementType.Text, properties = {})
        public String bearerToken;
        
    }
    public BearerAuthConfig bearerAuthConfig;
    
    public static final String REQUEST_DATA = "gp_1QrsEeDLfv";
    @LayoutElementPOJOBinding(id = REQUEST_DATA, type = ElementType.GroupLayout)
    public static class RequestData {
        public static final String REQUEST_CONTENT_TYPE_OPT_JSON = "application/json";
        public static final String REQUEST_CONTENT_TYPE_OPT_MULTIPARTFORMDATA = "multipart/form-data";
        public static final String REQUEST_CONTENT_TYPE_OPT_FORMURLENCODED = "application/x-www-form-urlencoded";
        public static final String REQUEST_CONTENT_TYPE_OPT_MANUELL = "custom";
        public static final String REQUEST_CONTENT_TYPE_FIELD_ID = "requestContentType";
        @InputElementPOJOBinding(id = REQUEST_CONTENT_TYPE_FIELD_ID , type = ElementType.Select, properties = {})
        public String requestContentType;
        
            public static final String REQUEST_CONTENT_TYPE_JSON_CONFIG = "gp_wIXoMcKYPn";
        @LayoutElementPOJOBinding(id = REQUEST_CONTENT_TYPE_JSON_CONFIG, type = ElementType.GroupLayout)
        public static class RequestContentTypeJsonConfig {
            public static final String REQUEST_JSON_SOURCE_OPT_VORGANGSDATEN = "processData";
            public static final String REQUEST_JSON_SOURCE_OPT_LOWCODE = "lowCode";
            public static final String REQUEST_JSON_SOURCE_FIELD_ID = "requestJsonSource";
            @InputElementPOJOBinding(id = REQUEST_JSON_SOURCE_FIELD_ID , type = ElementType.Radio, properties = {})
            public String requestJsonSource;
            
            public static final String REQUEST_JSON_PROCESS_DATA_KEY_FIELD_ID = "requestJsonProcessDataKey";
            @InputElementPOJOBinding(id = REQUEST_JSON_PROCESS_DATA_KEY_FIELD_ID , type = ElementType.ProcessDataKeyInput, properties = {})
            public String requestJsonProcessDataKey;
            
            public static final String REQUEST_JSON_LOW_CODE_FIELD_ID = "requestJsonLowCode";
            @InputElementPOJOBinding(id = REQUEST_JSON_LOW_CODE_FIELD_ID , type = ElementType.CodeInput, properties = {})
            public String requestJsonLowCode;
            
        }
        public RequestContentTypeJsonConfig requestContentTypeJsonConfig;
        
        public static final String REQUEST_FORM_FIELDS_FIELD_ID = "requestFormFields";
        @InputElementPOJOBinding(id = REQUEST_FORM_FIELDS_FIELD_ID , type = ElementType.Table, properties = {})
        public List<Map<String, Object>> requestFormFields;
        
        public static final String REQUEST_FORM_ATTACHMENT_SET_DATA_KEYS_FIELD_ID = "requestFormAttachments";
        @InputElementPOJOBinding(id = REQUEST_FORM_ATTACHMENT_SET_DATA_KEYS_FIELD_ID , type = ElementType.ProcessInstanceAttachmentSetSelect, properties = {})
        public List<String> requestFormAttachmentSetDataKeys;
        
    }
    public RequestData requestData;
    
    public static final String RESPONSE_CONFIG = "gp_uco56RTmgy";
    @LayoutElementPOJOBinding(id = RESPONSE_CONFIG, type = ElementType.GroupLayout)
    public static class ResponseConfig {
        public static final String RESPONSE_STATUS_CODE_FIELD_ID = "responseStatusCode";
        @InputElementPOJOBinding(id = RESPONSE_STATUS_CODE_FIELD_ID , type = ElementType.ChipInput, properties = {})
        public List<String> responseStatusCode;
        
        public static final String RESPONSE_BODY_TYPE_OPT_JSON = "json";
        public static final String RESPONSE_BODY_TYPE_OPT_TEXT = "text";
        public static final String RESPONSE_BODY_TYPE_OPT_DATEI = "file";
        public static final String RESPONSE_BODY_TYPE_FIELD_ID = "responseBodyType";
        @InputElementPOJOBinding(id = RESPONSE_BODY_TYPE_FIELD_ID , type = ElementType.Select, properties = {})
        public String responseBodyType;
        
        public static final String RESPONSE_FILE_NAME_FIELD_ID = "responseFileName";
        @InputElementPOJOBinding(id = RESPONSE_FILE_NAME_FIELD_ID , type = ElementType.Text, properties = {})
        public String responseFileName;
        
    }
    public ResponseConfig responseConfig;
    
    public static final String ADDITIONAL_SETTINGS = "gp_rHh87nZvvM";
    @LayoutElementPOJOBinding(id = ADDITIONAL_SETTINGS, type = ElementType.GroupLayout)
    public static class AdditionalSettings {
        public static final String ADDITIONAL_HEADERS_FIELD_ID = "additionalHeaders";
        @InputElementPOJOBinding(id = ADDITIONAL_HEADERS_FIELD_ID , type = ElementType.Table, properties = {})
        public List<Map<String, Object>> additionalHeaders;
        
    }
    public AdditionalSettings additionalSettings;
    
}
