package de.aivot.prosuna.backend.nocode.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.Nullable;

/**
 * Internal authoring information. Never used by evaluation or sent to customer clients or exports.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NoCodeEditorMetadata(
        @Nullable String notes,
        @Nullable NoCodeTestData testData
) implements java.io.Serializable {
}
