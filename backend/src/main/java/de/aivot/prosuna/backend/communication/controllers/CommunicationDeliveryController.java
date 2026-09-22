package de.aivot.prosuna.backend.communication.controllers;

import de.aivot.prosuna.backend.communication.models.CommunicationDeliveryView;
import de.aivot.prosuna.backend.communication.services.CommunicationDeliveryReadService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.UUID;

@RestController
@Tag(name = "Communication Deliveries")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class CommunicationDeliveryController {
    private final CommunicationDeliveryReadService service;

    public CommunicationDeliveryController(CommunicationDeliveryReadService service) { this.service = service; }

    @GetMapping("/api/process-instance-tasks/{taskId}/delivery/")
    @Operation(summary = "Read task delivery status", description = "Requires process_instance.read for the task's process instance, including system-level grants.")
    public ResponseEntity<CommunicationDeliveryView> task(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                         @Nonnull @PathVariable Long taskId) throws ResponseException {
        var result = service.forTask(jwt, taskId);
        return result == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
    }

    @GetMapping("/api/communication-providers/{providerId}/tests/{id}/")
    @Operation(summary = "Read communication test delivery status", description = "Requires communication_provider.read. Only test deliveries of this provider are returned.")
    public CommunicationDeliveryView test(@Nullable @AuthenticationPrincipal Jwt jwt,
                                          @Nonnull @PathVariable Integer providerId,
                                          @Nonnull @PathVariable UUID id) throws ResponseException {
        return service.forTest(jwt, providerId, id);
    }
}
