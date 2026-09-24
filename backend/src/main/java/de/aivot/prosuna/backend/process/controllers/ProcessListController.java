package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.process.dtos.ProcessListDTO;
import de.aivot.prosuna.backend.process.dtos.ProcessListFilter;
import de.aivot.prosuna.backend.process.services.ProcessListService;
import de.aivot.prosuna.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/process-lists/")
@Tag(name = "Process lists")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessListController {
    private final UserService users;
    private final ProcessListService lists;

    public ProcessListController(@Nonnull UserService users, @Nonnull ProcessListService lists) {
        this.users = users;
        this.lists = lists;
    }

    @GetMapping("instances/")
    @Operation(summary = "List process instance summaries", description = "Returns paged metadata filtered by effective process_instance.read permissions.")
    @Nonnull
    public Page<ProcessListDTO.Instance> instances(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                   @Nonnull @ParameterObject @PageableDefault(size = 12) Pageable page,
                                                   @Nonnull @ParameterObject @Valid ProcessListFilter filter) throws ResponseException {
        return lists.instances(userId(jwt), page, filter);
    }

    @GetMapping("tasks/")
    @Operation(summary = "List task summaries", description = "Returns paged metadata for tasks in readable instances, including tasks assigned to others.")
    @Nonnull
    public Page<ProcessListDTO.Task> tasks(@Nullable @AuthenticationPrincipal Jwt jwt,
                                           @Nonnull @ParameterObject @PageableDefault(size = 12) Pageable page,
                                           @Nonnull @ParameterObject @Valid ProcessListFilter filter) throws ResponseException {
        return lists.tasks(userId(jwt), page, filter);
    }

    @GetMapping("instances/options/")
    @Operation(summary = "List instance filter options", description = "Names only processes and assignees occurring in readable instances.")
    @Nonnull
    public ProcessListDTO.Options instanceOptions(@Nullable @AuthenticationPrincipal Jwt jwt) throws ResponseException {
        return lists.options(userId(jwt), false, null);
    }

    @GetMapping("tasks/options/")
    @Operation(summary = "List task filter options", description = "Names only processes and assignees occurring in tasks of readable instances.")
    @Nonnull
    public ProcessListDTO.Options taskOptions(@Nullable @AuthenticationPrincipal Jwt jwt,
                                              @Nullable @RequestParam(required = false) Long instanceId) throws ResponseException {
        return lists.options(userId(jwt), true, instanceId);
    }

    @Nonnull
    private String userId(@Nullable Jwt jwt) throws ResponseException {
        return users.fromJWT(jwt).orElseThrow(ResponseException::unauthorized).getId();
    }
}
