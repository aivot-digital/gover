package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.audit.enums.AuditAction;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.permissions.models.PermissionEntry;
import de.aivot.prosuna.backend.process.dtos.ProcessInstanceAccessRuleDTO;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAccessControlEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAccessControlRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_UPDATE;

@Service
@Transactional(rollbackFor = ResponseException.class)
public class ProcessInstanceAccessControlService implements EntityService<ProcessInstanceAccessControlEntity, Integer> {
    private final ProcessInstanceAccessControlRepository repository;

    private final ProcessInstanceAccessGuard guard;
    private final PermissionService permissions;
    private final ScopedAuditService audit;

    public ProcessInstanceAccessControlService(ProcessInstanceAccessControlRepository repository,
                                               ProcessInstanceAccessGuard guard, PermissionService permissions,
                                               AuditService audit) {
        this.repository = repository;
        this.guard = guard;
        this.permissions = permissions;
        this.audit = audit.createScopedAuditService(ProcessInstanceAccessControlService.class, "Vorgänge");
    }

    @Nonnull
    public ProcessInstanceAccessControlEntity createAuthorized(@Nonnull UserEntity actor, @Nonnull ProcessInstanceAccessControlEntity entity) throws ResponseException {
        guard.lock(entity.getTargetProcessInstanceId());
        permissions.requireProcessInstancePermission(actor.getId(), entity.getTargetProcessInstanceId(), PROCESS_INSTANCE_UPDATE);
        return create(entity);
    }

    @Nonnull
    public ProcessInstanceAccessControlEntity updateAuthorized(@Nonnull UserEntity actor, @Nonnull Integer id,
                                                               @Nonnull ProcessInstanceAccessControlEntity entity) throws ResponseException {
        var existing = retrieve(id).orElseThrow(ResponseException::notFound);
        guard.lock(existing.getTargetProcessInstanceId());
        permissions.requireProcessInstancePermission(actor.getId(), existing.getTargetProcessInstanceId(), PROCESS_INSTANCE_UPDATE);
        return performUpdate(id, entity, existing);
    }

    @Nonnull
    public ProcessInstanceAccessControlEntity deleteAuthorized(@Nonnull UserEntity actor, @Nonnull Integer id) throws ResponseException {
        var existing = retrieve(id).orElseThrow(ResponseException::notFound);
        guard.lock(existing.getTargetProcessInstanceId());
        permissions.requireProcessInstancePermission(actor.getId(), existing.getTargetProcessInstanceId(), PROCESS_INSTANCE_UPDATE);
        performDelete(existing);
        return existing;
    }

    @Nonnull
    public List<ProcessInstanceAccessControlEntity> replace(@Nonnull UserEntity actor, @Nonnull Long instanceId,
                                                            @Nonnull List<ProcessInstanceAccessRuleDTO> rules) throws ResponseException {
        permissions.requireProcessInstancePermission(actor.getId(), instanceId, PROCESS_INSTANCE_UPDATE);
        var beforeAccess = guard.lockAndSnapshot(instanceId);
        // Recheck after waiting for concurrent changes, including withdrawal of the actor's own access.
        permissions.requireProcessInstancePermission(actor.getId(), instanceId, PROCESS_INSTANCE_UPDATE);
        var canRead = permissions.hasProcessInstancePermission(actor.getId(), instanceId, ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);
        var recipients = new HashSet<String>();
        for (var rule : rules) {
            if (rule == null) throw ResponseException.badRequest("Die Auswahl enthält einen ungültigen Berechtigungseintrag.");
            validate(rule.sourceDepartmentId(), rule.sourceTeamId(), rule.permissions());
            if (!recipients.add(recipient(rule.sourceDepartmentId(), rule.sourceTeamId()))) {
                throw ResponseException.badRequest("Organisationseinheiten und Teams dürfen jeweils nur einmal ausgewählt werden.");
            }
        }
        var existing = repository.findAllByTargetProcessInstanceIdOrderById(instanceId);
        var before = existing.stream().map(this::toRule).toList();
        repository.deleteAll(existing.stream().filter(entry -> !recipients.contains(recipient(entry.getSourceDepartmentId(), entry.getSourceTeamId()))).toList());
        for (var rule : rules) {
            var entry = existing.stream().filter(value -> Objects.equals(value.getSourceDepartmentId(), rule.sourceDepartmentId())
                            && Objects.equals(value.getSourceTeamId(), rule.sourceTeamId())).findFirst()
                    .orElseGet(() -> new ProcessInstanceAccessControlEntity().setTargetProcessInstanceId(instanceId)
                            .setSourceDepartmentId(rule.sourceDepartmentId()).setSourceTeamId(rule.sourceTeamId()));
            entry.setPermissions(List.copyOf(rule.permissions()));
            repository.save(entry);
        }
        // The normal permission views now see the proposed state inside this transaction only.
        // A checked ResponseException rolls back every rule when an assignee would lose access.
        repository.flush();
        guard.requireRetainedAccess(instanceId, beforeAccess, canRead);
        var result = repository.findAllByTargetProcessInstanceIdOrderById(instanceId);
        var after = result.stream().map(this::toRule).toList();
        if (!new HashSet<>(before).equals(new HashSet<>(after))) {
            audit.create().withUser(actor).withAuditAction(AuditAction.Update, ProcessInstanceEntity.class, instanceId, "id")
                    .withDiff(Map.of("accessControls", before), Map.of("accessControls", after))
                    .withMessage("Die Berechtigungen des Vorgangs wurden geändert.").log();
        }
        return result;
    }

    private ProcessInstanceAccessRuleDTO toRule(ProcessInstanceAccessControlEntity entry) {
        return new ProcessInstanceAccessRuleDTO(entry.getSourceDepartmentId(), entry.getSourceTeamId(), List.copyOf(entry.getPermissions()));
    }

    private String recipient(Integer departmentId, Integer teamId) {
        return departmentId != null ? "department:" + departmentId : "team:" + teamId;
    }

    private void validate(Integer departmentId, Integer teamId, List<String> keys) throws ResponseException {
        if ((departmentId == null) == (teamId == null)) {
            throw ResponseException.badRequest("Wählen Sie entweder eine Organisationseinheit oder ein Team aus.");
        }
        var allowed = Arrays.stream(new ProcessInstancePermissionProvider().getPermissions()).map(PermissionEntry::permission).toList();
        if (keys == null || keys.stream().anyMatch(key -> !allowed.contains(key))) {
            throw ResponseException.badRequest("Die Auswahl enthält ungültige Vorgangsberechtigungen.");
        }
    }

    @Nonnull
    @Override
    public ProcessInstanceAccessControlEntity create(@Nonnull ProcessInstanceAccessControlEntity entity) throws ResponseException {
        guard.lock(entity.getTargetProcessInstanceId());
        validate(entity.getSourceDepartmentId(), entity.getSourceTeamId(), entity.getPermissions());
        entity.setId(null);
        return repository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessInstanceAccessControlEntity> performList(@Nonnull Pageable pageable,
                                                                @Nullable Specification<ProcessInstanceAccessControlEntity> specification,
                                                                @Nullable Filter<ProcessInstanceAccessControlEntity> filter) throws ResponseException {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAccessControlEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAccessControlEntity> retrieve(@Nonnull Specification<ProcessInstanceAccessControlEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessInstanceAccessControlEntity> specification) {
        return repository.exists(specification);
    }

    // Declare inherited write entry points on the transactional class: interface defaults are
    // otherwise invoked without the transaction needed for the lock and checked-exception rollback.
    @Nonnull
    @Override
    public ProcessInstanceAccessControlEntity update(@Nonnull Integer id, @Nonnull ProcessInstanceAccessControlEntity entity) throws ResponseException {
        return EntityService.super.update(id, entity);
    }

    @Nonnull
    @Override
    public ProcessInstanceAccessControlEntity delete(@Nonnull Integer id) throws ResponseException {
        return EntityService.super.delete(id);
    }

    @Nonnull
    @Override
    public ProcessInstanceAccessControlEntity deleteEntity(@Nonnull ProcessInstanceAccessControlEntity entity) throws ResponseException {
        return EntityService.super.deleteEntity(entity);
    }

    @Nonnull
    @Override
    public ProcessInstanceAccessControlEntity performUpdate(@Nonnull Integer id,
                                                            @Nonnull ProcessInstanceAccessControlEntity entity,
                                                            @Nonnull ProcessInstanceAccessControlEntity existingEntity) throws ResponseException {
        var before = guard.lockAndSnapshot(existingEntity.getTargetProcessInstanceId());
        validate(existingEntity.getSourceDepartmentId(), existingEntity.getSourceTeamId(), entity.getPermissions());
        existingEntity.setPermissions(entity.getPermissions());
        var result = repository.saveAndFlush(existingEntity);
        guard.requireRetainedAccess(existingEntity.getTargetProcessInstanceId(), before);
        return result;
    }

    @Override
    public void performDelete(@Nonnull ProcessInstanceAccessControlEntity entity) throws ResponseException {
        var before = guard.lockAndSnapshot(entity.getTargetProcessInstanceId());
        repository.delete(entity);
        repository.flush();
        guard.requireRetainedAccess(entity.getTargetProcessInstanceId(), before);
    }
}

