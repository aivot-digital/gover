package de.aivot.GoverBackend.models.auth;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class KeyCloakJwtClaims {
    private List<String> roles = new LinkedList<>();
    private List<String> groups = new LinkedList<>();

    public static KeyCloakJwtClaims fromJwt(Jwt jwt) {
        KeyCloakJwtClaims keyCloakJwtClaims = new KeyCloakJwtClaims();

        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            var roles = realmAccess.get("roles");
            if (roles != null) {
                keyCloakJwtClaims.roles = (List<String>) roles;
            }
        }

        var groupAccess = jwt.getClaimAsMap("group_access");
        if (groupAccess != null) {
            var groups = groupAccess.get("groups");
            if (groups != null) {
                keyCloakJwtClaims.groups = (List<String>) groups;
            }
        }

        return keyCloakJwtClaims;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public void testRole(String role, Supplier<RuntimeException> exceptionSupplier) {
        if (!hasRole(role)) {
            throw exceptionSupplier.get();
        }
    }

    public boolean hasGroup(String group) {
        return groups.contains(group);
    }

    // region Getters & Setters

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    // endregion
}
