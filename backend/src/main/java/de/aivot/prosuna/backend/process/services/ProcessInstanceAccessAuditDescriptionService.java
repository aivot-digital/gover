package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.repositories.DepartmentRepository;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAccessControlEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAccessControlPresetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.teams.entities.TeamEntity;
import de.aivot.prosuna.backend.teams.repositories.TeamRepository;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

@Service
public class ProcessInstanceAccessAuditDescriptionService {
    private final ProcessRepository processes;
    private final ProcessInstanceRepository instances;
    private final DepartmentRepository departments;
    private final TeamRepository teams;

    public ProcessInstanceAccessAuditDescriptionService(ProcessRepository processes,
                                                        ProcessInstanceRepository instances,
                                                        DepartmentRepository departments,
                                                        TeamRepository teams) {
        this.processes = processes;
        this.instances = instances;
        this.departments = departments;
        this.teams = teams;
    }

    @Nonnull
    public String describePreset(@Nonnull ProcessInstanceAccessControlPresetEntity entry) {
        var process = processes.findById(entry.getTargetProcessId())
                .map(ProcessEntity::getInternalTitle)
                .map(StringUtils::quote)
                .orElse("mit der ID " + entry.getTargetProcessId());
        return "die Standardberechtigungen für neue Vorgänge des Prozesses " + process
                + " (Version " + entry.getTargetProcessVersion() + ") für "
                + describeRecipient(entry.getSourceDepartmentId(), entry.getSourceTeamId());
    }

    @Nonnull
    public String describeAccess(@Nonnull ProcessInstanceAccessControlEntity entry) {
        var instance = instances.findById(entry.getTargetProcessInstanceId())
                .map(ProcessInstanceEntity::getCaseNumber)
                .map(StringUtils::quote)
                .orElse("mit der ID " + entry.getTargetProcessInstanceId());
        return "die Berechtigungen am Vorgang " + instance + " für "
                + describeRecipient(entry.getSourceDepartmentId(), entry.getSourceTeamId());
    }

    @Nonnull
    private String describeRecipient(@Nullable Integer departmentId, @Nullable Integer teamId) {
        if (departmentId != null) {
            return "die Organisationseinheit " + departments.findById(departmentId)
                    .map(DepartmentEntity::getName)
                    .map(StringUtils::quote)
                    .orElse("mit der ID " + departmentId);
        }
        if (teamId != null) {
            return "das Team " + teams.findById(teamId)
                    .map(TeamEntity::getName)
                    .map(StringUtils::quote)
                    .orElse("mit der ID " + teamId);
        }
        return "eine nicht mehr zugeordnete Organisationseinheit oder ein nicht mehr zugeordnetes Team";
    }
}
