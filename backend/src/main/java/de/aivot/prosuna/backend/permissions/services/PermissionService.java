package de.aivot.prosuna.backend.permissions.services;

import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.repositories.DepartmentRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.repositories.VUserDepartmentPermissionRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserTeamPermissionRepository;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.teams.entities.TeamEntity;
import de.aivot.prosuna.backend.teams.repositories.TeamRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {
    /*
     * Resource checks intentionally treat the matching system permission as a global override.
     * This keeps the permission key semantics identical across system, department, team,
     * process and process-instance grants.
     */
    private final VUserDepartmentPermissionRepository vUserDepartmentPermissionRepository;
    private final VUserTeamPermissionRepository vUserTeamPermissionRepository;
    private final VUserSystemPermissionRepository vUserSystemPermissionRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final ProcessRepository processRepository;
    private final ProcessInstanceRepository processInstanceRepository;

    public PermissionService(VUserDepartmentPermissionRepository vUserDepartmentPermissionRepository,
                             VUserTeamPermissionRepository vUserTeamPermissionRepository,
                             VUserSystemPermissionRepository vUserSystemPermissionRepository,
                             DepartmentRepository departmentRepository,
                             TeamRepository teamRepository,
                             ProcessRepository processRepository,
                             ProcessInstanceRepository processInstanceRepository) {
        this.vUserDepartmentPermissionRepository = vUserDepartmentPermissionRepository;
        this.vUserTeamPermissionRepository = vUserTeamPermissionRepository;
        this.vUserSystemPermissionRepository = vUserSystemPermissionRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.processRepository = processRepository;
        this.processInstanceRepository = processInstanceRepository;
    }

    public boolean hasSystemPermission(@Nullable String userId,
                                         @Nonnull String permission) {
        if (userId == null) {
            return false;
        }
        return vUserSystemPermissionRepository
                .hasPermission(userId, permission);
    }

    public boolean hasSystemPermission(@Nullable Jwt jwt,
                                         @Nonnull String permission) {
        return hasSystemPermission(UserService.getIdFromJWT(jwt), permission);
    }

    public boolean hasSystemPermission(@Nullable UserEntity user,
                                         @Nonnull String permission) {
        if (user == null) {
            return false;
        }
        return hasSystemPermission(user.getId(), permission);
    }

    public void requireSystemPermission(@Nullable String userId,
                                    @Nonnull String permission) throws ResponseException {
        if (!hasSystemPermission(userId, permission)) {
            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s auf Systemebene.",
                    StringUtils.quote(permission)
            );
        }
    }

    public void requireSystemPermission(@Nullable Jwt jwt,
                                    @Nonnull String permission) throws ResponseException {
        requireSystemPermission(UserService.getIdFromJWT(jwt), permission);
    }

    public boolean hasDepartmentPermission(@Nullable String userId,
                                             @Nonnull Integer departmentId,
                                             @Nonnull String permission) {
        if (userId == null) {
            return false;
        }

        return vUserDepartmentPermissionRepository.hasPermission(userId, departmentId, permission)
                || vUserSystemPermissionRepository.hasPermission(userId, permission);
    }

    public List<Integer> getDepartmentsWithPermission(@Nonnull String userId,
                                                      @Nonnull String permission) {
        // Returns resource-scoped grants only. List endpoints check the system permission before narrowing filters.
        return vUserDepartmentPermissionRepository
                .getDepartmentsWithPermission(userId, permission);
    }

    public void requireDepartmentPermission(@Nonnull String userId,
                                        @Nonnull Integer departmentId,
                                        @Nonnull String permission) throws ResponseException {
        if (!hasDepartmentPermission(userId, departmentId, permission)) {
            var departmentName = departmentRepository
                    .findById(departmentId)
                    .map(DepartmentEntity::getName)
                    .map(StringUtils::quote)
                    .orElse("mit der ID " + departmentId);

            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s für die Organisationseinheit %s.",
                    StringUtils.quote(permission),
                    departmentName
            );
        }
    }

    public boolean hasInAnyDepartmentPermission(@Nullable String userId,
                                                  @Nonnull String permission) {
        if (userId == null) {
            return false;
        }

        return vUserDepartmentPermissionRepository.hasPermissionInAnyDepartment(userId, permission)
                || vUserSystemPermissionRepository.hasPermission(userId, permission);
    }

    public void requireInAnyDepartmentPermission(@Nonnull String userId,
                                             @Nonnull String permission) throws ResponseException {
        if (!hasInAnyDepartmentPermission(userId, permission)) {
            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s in mindestens einer Organisationseinheit.",
                    StringUtils.quote(permission)
            );
        }
    }

    public boolean hasTeamPermission(@Nullable String userId,
                                       @Nonnull Integer teamId,
                                       @Nonnull String permission) {
        if (userId == null) {
            return false;
        }

        return vUserTeamPermissionRepository.hasPermission(userId, teamId, permission)
                || vUserSystemPermissionRepository.hasPermission(userId, permission);
    }

    public List<Integer> getTeamsWithPermission(@Nonnull String userId,
                                                @Nonnull String permission) {
        // Returns resource-scoped grants only. List endpoints check the system permission before narrowing filters.
        return vUserTeamPermissionRepository
                .getTeamsWithPermission(userId, permission);
    }

    public void requireTeamPermission(@Nonnull String userId,
                                  @Nonnull Integer teamId,
                                  @Nonnull String permission) throws ResponseException {
        if (!hasTeamPermission(userId, teamId, permission)) {
            var teamName = teamRepository
                    .findById(teamId)
                    .map(TeamEntity::getName)
                    .map(StringUtils::quote)
                    .orElse("mit der ID " + teamId);

            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s für das Team %s.",
                    StringUtils.quote(permission),
                    teamName
            );
        }
    }

    public boolean hasInAnyTeamPermission(@Nullable String userId,
                                            @Nonnull String permission) {
        if (userId == null) {
            return false;
        }

        return vUserTeamPermissionRepository.hasPermissionInAnyTeam(userId, permission)
                || vUserSystemPermissionRepository.hasPermission(userId, permission);
    }

    public void requireInAnyTeamPermission(@Nonnull String userId,
                                       @Nonnull String permission) throws ResponseException {
        if (!hasInAnyTeamPermission(userId, permission)) {
            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s in mindestens einem Team.",
                    StringUtils.quote(permission)
            );
        }
    }

    public boolean hasProcessPermission(@Nullable String userId,
                                          @Nonnull Integer processId,
                                          @Nonnull String permission) {
        if (userId == null) {
            return false;
        }

        return processRepository.hasPermission(userId, processId, permission)
                || vUserSystemPermissionRepository.hasPermission(userId, permission);
    }

    public List<Integer> getProcessesWithPermission(@Nonnull String userId,
                                                    @Nonnull String permission) {
        // Returns effective process-scoped grants, including team and department-derived access.
        // System-wide access is intentionally handled separately by callers.
        return processRepository
                .getProcessIdsWithPermission(userId, permission);
    }

    public void requireProcessPermission(@Nonnull String userId,
                                     @Nonnull Integer processId,
                                     @Nonnull String permission) throws ResponseException {
        if (!hasProcessPermission(userId, processId, permission)) {
            var processName = processRepository
                    .findById(processId)
                    .map(ProcessEntity::getInternalTitle)
                    .map(StringUtils::quote)
                    .orElse("mit der ID " + processId);

            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s für den Prozess %s.",
                    StringUtils.quote(permission),
                    processName
            );
        }
    }

    public boolean hasInAnyProcessPermission(@Nullable String userId,
                                               @Nonnull String permission) {
        if (userId == null) {
            return false;
        }

        return processRepository.hasPermissionInAnyProcess(userId, permission)
                || vUserSystemPermissionRepository.hasPermission(userId, permission);
    }

    public void requireInAnyProcessPermission(@Nonnull String userId,
                                          @Nonnull String permission) throws ResponseException {
        if (!hasInAnyProcessPermission(userId, permission)) {
            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s in mindestens einem Prozess.",
                    StringUtils.quote(permission)
            );
        }
    }

    public boolean hasProcessInstancePermission(@Nullable String userId,
                                                  @Nonnull Long processInstanceId,
                                                  @Nonnull String permission) {
        if (userId == null) {
            return false;
        }

        return processInstanceRepository.hasPermission(userId, processInstanceId, permission)
                || vUserSystemPermissionRepository.hasPermission(userId, permission);
    }

    public List<Long> getProcessInstancesWithPermission(@Nonnull String userId,
                                                        @Nonnull String permission) {
        // Returns process-instance-scoped grants only. Callers handle system-wide access separately.
        return processInstanceRepository
                .getProcessInstanceIdsWithPermission(userId, permission);
    }

    public void requireProcessInstancePermission(@Nonnull String userId,
                                             @Nonnull Long processInstanceId,
                                             @Nonnull String permission) throws ResponseException {
        if (!hasProcessInstancePermission(userId, processInstanceId, permission)) {
            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s für den Vorgang mit der ID %s.",
                    StringUtils.quote(permission),
                    StringUtils.quote(String.valueOf(processInstanceId))
            );
        }
    }

    public boolean hasInAnyProcessInstancePermission(@Nullable String userId,
                                                       @Nonnull String permission) {
        if (userId == null) {
            return false;
        }

        return processInstanceRepository.hasPermissionInAnyProcessInstance(userId, permission)
                || vUserSystemPermissionRepository.hasPermission(userId, permission);
    }

    public void requireInAnyProcessInstancePermission(@Nonnull String userId,
                                                  @Nonnull String permission) throws ResponseException {
        if (!hasInAnyProcessInstancePermission(userId, permission)) {
            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s in mindestens einem Vorgang.",
                    StringUtils.quote(permission)
            );
        }
    }
}
