package de.aivot.prosuna.backend.identity.dtos;

import de.aivot.prosuna.backend.identity.models.IdentityAttributeMapping;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityProviderRequestDTOTest {
    @Test
    void uniqueIdAttributeIsRequired() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(request("   "));

            assertTrue(violations.stream().anyMatch(violation -> violation.getMessage().contains("ist erforderlich")));
        }
    }

    @Test
    void uniqueIdAttributeMustReferenceConfiguredAttribute() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(request("email"));

            assertTrue(violations.stream().anyMatch(violation -> violation.getMessage().contains(
                    "muss in den Attributszuweisungen enthalten sein"
            )));
        }
    }

    @Test
    void mappedUniqueIdAttributeIsAcceptedAndCopiedToEntity() {
        var request = request("sub");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request).isEmpty());
        }
        assertEquals("sub", request.toEntity().getUniqueIdAttribute());
    }

    private static IdentityProviderRequestDTO request(String uniqueIdAttribute) {
        return new IdentityProviderRequestDTO(
                "custom-provider",
                uniqueIdAttribute,
                "Custom Provider",
                "Identity provider used in this test.",
                null,
                "https://example.org/authorize",
                "https://example.org/token",
                "https://example.org/userinfo",
                null,
                "client-id",
                null,
                List.of(new IdentityAttributeMapping()
                        .setLabel("Subject Identifier")
                        .setDescription("Unique provider identity")
                        .setKeyInData("sub")
                        .setDisplayAttribute(false)),
                List.of("openid"),
                List.of(),
                true,
                false,
                null
        );
    }
}
