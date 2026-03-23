package de.aivot.GoverBackend.system.properties;

import de.aivot.GoverBackend.utils.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "cors")
public class CORSProperties {
    private List<String> allowedOrigins;

    public static final String[] DEFAULT_ALLOWED_METHODS = new String[]{"HEAD", "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"};

    public boolean isEnabled() {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return false;
        }

        for (String origin : allowedOrigins) {
           if (StringUtils.isNullOrEmpty(origin)) {
               throw new IllegalArgumentException("CORS allowedOrigins cannot contain empty values");
           }

           try {
               new URI(origin.trim());
           } catch (Exception e) {
               throw new IllegalArgumentException("CORS allowedOrigins contains an invalid URI: " + origin, e);
           }
        }

        return true;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
