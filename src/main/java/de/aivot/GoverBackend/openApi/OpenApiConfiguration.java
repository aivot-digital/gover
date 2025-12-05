package de.aivot.GoverBackend.openApi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Gover API",
                version = "${build.buildVersion}",
                contact = @Contact(
                        name = "Aivot", email = "mail@aivot.de", url = "https://aivot.de/gover"
                ),
                license = @License(
                        name = "Sustainable Use License", url = "https://github.com/aivot-digital/gover?tab=License-1-ov-file"
                )
        )
)
@SecurityScheme(
        name = OpenApiConfiguration.Security,
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfiguration {
    public static final String Security = "JWT";
}
