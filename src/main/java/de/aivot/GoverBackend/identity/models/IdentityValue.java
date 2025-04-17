package de.aivot.GoverBackend.identity.models;

import de.aivot.GoverBackend.utils.StringUtils;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public record IdentityValue(
        @Nonnull
        String identityProviderKey,
        @Nonnull
        String metadataIdentifier,
        @Nonnull
        Map<String, String> userInfo
) {
    public Map<String, Object> toMap() {
        return Map.of(
                "identityProviderKey", identityProviderKey,
                "metadataIdentifier", metadataIdentifier,
                "userInfo", userInfo
        );
    }

    public static IdentityValue fromMap(Map<?, ?> map) {
        var identityProviderKey = map.get("identityProviderKey");
        String sIdentityProviderKey;
        if (identityProviderKey instanceof String) {
            sIdentityProviderKey = (String) identityProviderKey;
        } else {
            throw new IllegalArgumentException("identityProviderKey must be a String");
        }

        var metadataIdentifier = map.get("metadataIdentifier");
        String sMetadataIdentifier;
        if (metadataIdentifier instanceof String) {
            sMetadataIdentifier = (String) metadataIdentifier;
        } else {
            throw new IllegalArgumentException("metadataIdentifier must be a String");
        }

        var userinfo = map.get("userInfo");
        Map<String, String> mUserInfo = new HashMap<>();
        if (userinfo instanceof Map<?, ?> unkUserInfo) {
            for (var entry : unkUserInfo.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();

                if (!(key instanceof String)) {
                    throw new IllegalArgumentException("userInfo key must be a String");
                }

                if (!(value instanceof String)) {
                    throw new IllegalArgumentException("userInfo value must be a String");
                }

                mUserInfo.put((String) key, (String) value);
            }
        } else {
            throw new IllegalArgumentException("userInfo must be a Map");
        }

        return new IdentityValue(
                sIdentityProviderKey,
                sMetadataIdentifier,
                mUserInfo
        );
    }

    public boolean isEmpty() {
        return (
                StringUtils.isNullOrEmpty(identityProviderKey) ||
                StringUtils.isNullOrEmpty(metadataIdentifier) ||
                userInfo().isEmpty()
        );
    }
}
