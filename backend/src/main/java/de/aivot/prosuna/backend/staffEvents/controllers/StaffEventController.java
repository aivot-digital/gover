package de.aivot.prosuna.backend.staffEvents.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.staffEvents.services.StaffEventService;
import de.aivot.prosuna.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;

@RestController
@RequestMapping("/api/staff-events/")
@Tag(name = "Staff events")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class StaffEventController {
    private final UserService users;
    private final StaffEventService events;

    public StaffEventController(@Nonnull UserService users, @Nonnull StaffEventService events) {
        this.users = users;
        this.events = events;
    }

    // Without "produces": the application negotiates every request as JSON (ServerConfiguration), which a
    // text/event-stream condition rejects with 406. The SseEmitter sets the text/event-stream content type itself.
    @Nonnull
    @GetMapping("")
    @Operation(summary = "Subscribe to staff events", description = "Streams server-sent events that tell open staff views to reload, e.g. after a process instance changed. Each event is only sent to users with the permission to read its resource, e.g. `" + PROCESS_INSTANCE_READ + "` for a process instance. Events only name a resource type and, if any, its ID; the resource is loaded through its endpoint. The stream ends after 30 minutes, and within 25 seconds once the user is disabled or deleted in Prosuna. Changes made directly in the identity provider take effect with the next user synchronization, which runs every 5 minutes.")
    public SseEmitter subscribe(@Nullable @AuthenticationPrincipal Jwt jwt,
                                @Nonnull HttpServletResponse response) throws ResponseException {
        var user = users.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        // nginx buffers proxied responses by default, which would hold back the events.
        response.setHeader("X-Accel-Buffering", "no");
        return events.subscribe(user.getId());
    }
}
