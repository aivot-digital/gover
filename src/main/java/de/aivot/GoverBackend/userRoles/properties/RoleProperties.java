package de.aivot.GoverBackend.userRoles.properties;

import de.aivot.GoverBackend.userRoles.models.RoleDefinition;
import jakarta.annotation.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "role")
public class RoleProperties {
    @Nullable
    private RoleDefinition[] roles;

    @Nullable
    public RoleDefinition[] getRoles() {
        return roles;
    }

    public RoleProperties setRoles(@Nullable RoleDefinition[] roles) {
        this.roles = roles;
        return this;
    }
}
