package de.aivot.prosuna.backend.process.exceptions;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class ManualAssignmentRecipientUnavailableException extends ResponseException {
    public static final String FIELD_ID = "assignedUserId";

    public ManualAssignmentRecipientUnavailableException(@Nonnull String rejectedValue) {
        super(HttpStatus.BAD_REQUEST, "Die ausgewählte Person steht für diesen Vorgang nicht mehr zur Verfügung.",
                Map.of("code", "manual_assignment_recipient_unavailable", "fieldId", FIELD_ID, "rejectedValue", rejectedValue));
    }
}
