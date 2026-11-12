package de.aivot.prosuna.backend.system.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.system.dtos.DashboardActivityDTO;
import de.aivot.prosuna.backend.system.dtos.DashboardOverviewDTO;
import de.aivot.prosuna.backend.system.services.DashboardService;
import de.aivot.prosuna.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/dashboard/")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class DashboardController {
    private final UserService userService;
    private final DashboardService dashboardService;

    public DashboardController(UserService userService, DashboardService dashboardService) {
        this.userService = userService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("overview/")
    public DashboardOverviewDTO getOverview(@Nullable @AuthenticationPrincipal Jwt jwt) throws ResponseException {
        var user = userService.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        return dashboardService.getOverview(user);
    }

    @GetMapping("tasks/")
    @Operation(summary = "Retrieve the dashboard tasks", description = "Returns the open tasks assigned to the current user, as shown in the task panel of the overview. Lets the dashboard reload the panel after a change without loading the whole overview.")
    public DashboardOverviewDTO.TaskSummary getTasks(@Nullable @AuthenticationPrincipal Jwt jwt) throws ResponseException {
        var user = userService.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        return dashboardService.getTaskSummary(user);
    }

    @GetMapping("activity/")
    public DashboardActivityDTO getActivity(@Nullable @AuthenticationPrincipal Jwt jwt) throws ResponseException {
        var user = userService.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        return dashboardService.getActivity(user);
    }
}
