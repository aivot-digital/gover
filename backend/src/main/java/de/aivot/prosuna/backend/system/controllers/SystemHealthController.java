package de.aivot.prosuna.backend.system.controllers;

import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.HttpCodeStatusMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Keeps the system status of the staff UI reachable, because the actuator only listens on the internal management port.
 * Like the actuator with show-details when_authorized, this shows the details to every authenticated user.
 */
@RestController
@RequestMapping("/api/system/health/")
@Tag(name = "System", description = "System status of the application")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class SystemHealthController {
    private final HealthEndpoint healthEndpoint;

    public SystemHealthController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping
    @Operation(summary = "Retrieve the system health", description = "Returns the health of the application and its components. Responds with 503 if the application is down.")
    @Nonnull
    public ResponseEntity<HealthDescriptor> getHealth() {
        var health = healthEndpoint.health();
        return ResponseEntity
                .status(HttpCodeStatusMapper.DEFAULT.getStatusCode(health.getStatus()))
                .body(health);
    }
}
