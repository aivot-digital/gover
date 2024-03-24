package de.aivot.GoverBackend.models.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class KeyCloakDetailsUser extends KeyCloakListUser {
    private Collection<KeyCloakRole> roles = new LinkedList<>();
    private Collection<KeyCloakGroup> groups = new LinkedList<>();

    public static KeyCloakDetailsUser fromJwt(Jwt jwt) {
        KeyCloakDetailsUser user = new KeyCloakDetailsUser();

        user.setId(jwt.getClaimAsString("sub"));
        user.setUsername(jwt.getClaimAsString("preferred_username"));
        user.setFirstName(jwt.getClaimAsString("given_name"));
        user.setLastName(jwt.getClaimAsString("family_name"));
        user.setEmail(jwt.getClaimAsString("email"));
        user.setEnabled(true);

        var claims = KeyCloakJwtClaims.fromJwt(jwt);
        user.setRoles(claims.getRoles().stream().map(role -> {
            KeyCloakRole keyCloakRole = new KeyCloakRole();
            keyCloakRole.setName(role);
            keyCloakRole.setDescription("");
            return keyCloakRole;
        }).collect(Collectors.toList()));
        user.setGroups(claims.getGroups().stream().map(role -> {
            KeyCloakGroup keyCloakGroup = new KeyCloakGroup();
            keyCloakGroup.setName(role);
            keyCloakGroup.setPath("");
            return keyCloakGroup;
        }).collect(Collectors.toList()));

        return user;
    }

    public static KeyCloakDetailsUser fromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        KeyCloakDetailsUser user;
        try {
            user = mapper.readValue(json, KeyCloakDetailsUser.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    public boolean isAdmin() {
        return hasRole("admin");
    }

    public boolean isNotAnAdmin() {
        return !isAdmin();
    }

    private boolean hasRole(String testRole) {
        return roles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(testRole));
    }

    public void ifNotAdminThrow(Supplier<? extends RuntimeException> exceptionSupplier) {
        ifNotHasRoleThrow("admin", exceptionSupplier);
    }

    public void ifNotHasRoleThrow(String testRole, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!hasRole(testRole)) {
            throw exceptionSupplier.get();
        }
    }

    public Collection<KeyCloakGroup> getGroups() {
        return groups;
    }

    public void setGroups(Collection<KeyCloakGroup> groups) {
        this.groups = groups;
    }

    public Collection<KeyCloakRole> getRoles() {
        return roles;
    }

    public void setRoles(Collection<KeyCloakRole> roles) {
        this.roles = roles;
    }
}
