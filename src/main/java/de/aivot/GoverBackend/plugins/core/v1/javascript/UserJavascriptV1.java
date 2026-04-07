package de.aivot.GoverBackend.plugins.core.v1.javascript;

import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class provides JavaScript functions for retrieving users.
 * The functions are exposed to the JavaScript environment through the GraalVM Polyglot API.
 */
@Component
public class UserJavascriptV1 implements JavascriptFunctionProvider {
    private final UserService userService;

    @Autowired
    public UserJavascriptV1(UserService userService) {
        this.userService = userService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "users";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Mitarbeiter:innen";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für Mitarbeiter:innen.";
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[]{
                "get(id: string | null): {id: string; email: string | null; firstName: string | null; lastName: string | null; fullName: string; enabled: boolean; verified: boolean; deletedInIdp: boolean; systemRoleId: number | null;} | null;"
        };
    }

    @Nullable
    @HostAccess.Export
    public ProxyObject get(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        UserEntity user;
        try {
            user = userService
                    .retrieve(id)
                    .orElse(null);
        } catch (ResponseException e) {
            return null;
        }

        if (user == null) {
            return null;
        }

        return JavascriptEngine
                .mapToProxyObject(toUserMap(user));
    }

    @Nonnull
    private static Map<String, Object> toUserMap(@Nonnull UserEntity user) {
        var data = new LinkedHashMap<String, Object>();

        data.put("id", user.getId());
        data.put("email", user.getEmail());
        data.put("firstName", user.getFirstName());
        data.put("lastName", user.getLastName());
        data.put("fullName", user.getFullName());
        data.put("enabled", user.getEnabled());
        data.put("verified", user.getVerified());
        data.put("deletedInIdp", user.getDeletedInIdp());
        data.put("systemRoleId", user.getSystemRoleId());

        return data;
    }
}
