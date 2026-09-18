package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessSlugHistoryEntity;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessSlugHistoryRepository;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessService implements EntityService<ProcessEntity, Integer> {
    private static final int MAX_SLUG_LENGTH = 128;

    private final ProcessRepository processDefinitionRepository;
    private final ProcessSlugHistoryRepository processSlugHistoryRepository;
    private final PermissionService permissionService;

    @Autowired
    public ProcessService(ProcessRepository processDefinitionRepository,
                          ProcessSlugHistoryRepository processSlugHistoryRepository,
                          PermissionService permissionService) {
        this.processDefinitionRepository = processDefinitionRepository;
        this.processSlugHistoryRepository = processSlugHistoryRepository;
        this.permissionService = permissionService;
    }

    @Nonnull
    @Override
    public ProcessEntity create(@Nonnull ProcessEntity entity) throws ResponseException {
        entity.setId(null);
        entity.setAccessKey(UUID.randomUUID());

        var slug = normalizeAndValidateSlug(entity.getSlug());
        validateSlugIsAvailable(slug, null);
        entity.setSlug(slug);

        return processDefinitionRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessEntity> performList(@Nonnull Pageable pageable,
                                           @Nullable Specification<ProcessEntity> specification,
                                           @Nullable Filter<ProcessEntity> filter) throws ResponseException {
        return processDefinitionRepository.findAll(specification, pageable);
    }

    public Page<ProcessEntity> listAllByAccessibleForUser(@Nonnull Pageable pageable,
                                                          @Nonnull String userId,
                                                          @Nullable Specification<ProcessEntity> specification) throws ResponseException {
        if (permissionService.hasSystemPermission(userId, ProcessPermissionProvider.PROCESS_DEFINITION_READ)) {
            return processDefinitionRepository.findAll(specification, pageable);
        }

        // A process can be visible through its owning department or through an explicit process grant.
        var accessibleDepartmentIds = permissionService
                .getDepartmentsWithPermission(userId, ProcessPermissionProvider.PROCESS_DEFINITION_READ)
                .stream()
                .filter(Objects::nonNull)
                .toList();

        var accessibleProcessIds = processDefinitionRepository
                .getProcessIdsWithPermission(userId, ProcessPermissionProvider.PROCESS_DEFINITION_READ)
                .stream()
                .filter(Objects::nonNull)
                .toList();

        if (accessibleDepartmentIds.isEmpty() && accessibleProcessIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Specification<ProcessEntity> userAccessSpec = (root, query, criteriaBuilder) -> {
            var predicates = new LinkedList<Predicate>();

            if (!accessibleDepartmentIds.isEmpty()) {
                predicates.add(root.get("departmentId").in(accessibleDepartmentIds));
            }

            if (!accessibleProcessIds.isEmpty()) {
                predicates.add(root.get("id").in(accessibleProcessIds));
            }

            return criteriaBuilder.or(predicates.toArray(Predicate[]::new));
        };

        Specification<ProcessEntity> combinedSpec = (specification == null) ? userAccessSpec : specification.and(userAccessSpec);

        return processDefinitionRepository.findAll(combinedSpec, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return processDefinitionRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessEntity> retrieve(@Nonnull Specification<ProcessEntity> specification) throws ResponseException {
        return processDefinitionRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return processDefinitionRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessEntity> specification) {
        return processDefinitionRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessEntity performUpdate(@Nonnull Integer id,
                                       @Nonnull ProcessEntity entity,
                                       @Nonnull ProcessEntity existingEntity) throws ResponseException {
        var previousSlug = existingEntity.getSlug();
        var updatedSlug = normalizeAndValidateSlug(entity.getSlug());

        validateSlugIsAvailable(updatedSlug, existingEntity.getId());

        if (StringUtils.isNotNullOrEmpty(previousSlug) && !Objects.equals(previousSlug, updatedSlug)) {
            if (processSlugHistoryRepository.existsById(updatedSlug)) {
                processSlugHistoryRepository.deleteById(updatedSlug);
            }

            // Process slugs are external entry points. Keep the old namespace as an alias
            // so published links survive process-level slug changes.
            processSlugHistoryRepository.save(new ProcessSlugHistoryEntity(previousSlug, existingEntity.getId()));
        }

        existingEntity.setInternalTitle(entity.getInternalTitle());
        existingEntity.setDepartmentId(entity.getDepartmentId());
        existingEntity.setSlug(updatedSlug);
        return processDefinitionRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessEntity entity) throws ResponseException {
        processDefinitionRepository.delete(entity);
    }

    public Optional<ProcessEntity> retrieveByAccessKey(UUID processAccessKey) throws ResponseException {
        return processDefinitionRepository
                .findByAccessKey(processAccessKey);
    }

    public Optional<ProcessEntity> retrieveBySlug(@Nonnull String slug) throws ResponseException {
        var normalizedSlug = normalizeSlug(slug);
        return processDefinitionRepository
                .findBySlug(normalizedSlug);
    }

    public Optional<ProcessEntity> retrieveBySlugOrHistory(@Nonnull String slug) throws ResponseException {
        var normalizedSlug = normalizeSlug(slug);
        var process = processDefinitionRepository.findBySlug(normalizedSlug);
        if (process.isPresent()) {
            return process;
        }

        return processSlugHistoryRepository
                .findBySlug(normalizedSlug)
                .flatMap(history -> processDefinitionRepository.findById(history.getProcessId()));
    }

    public List<ProcessSlugHistoryEntity> listSlugHistory(@Nonnull Integer processId) throws ResponseException {
        if (!processDefinitionRepository.existsById(processId)) {
            throw ResponseException.notFound();
        }

        return processSlugHistoryRepository.findAllByProcessIdOrderByCreatedDesc(processId);
    }

    @Transactional
    public void clearSlugHistory(@Nonnull Integer processId) throws ResponseException {
        if (!processDefinitionRepository.existsById(processId)) {
            throw ResponseException.notFound();
        }

        // Clearing history intentionally releases former public namespaces for reuse by other processes.
        processSlugHistoryRepository.deleteAllByProcessId(processId);
    }

    public boolean isSlugAvailable(@Nonnull String slug,
                                   @Nullable Integer processId) throws ResponseException {
        var normalizedSlug = normalizeAndValidateSlug(slug);

        if (processId == null) {
            return !processDefinitionRepository.existsBySlug(normalizedSlug) &&
                    !processSlugHistoryRepository.existsById(normalizedSlug);
        }

        return !processDefinitionRepository.existsBySlugAndIdIsNot(normalizedSlug, processId) &&
                !processSlugHistoryRepository.existsBySlugAndProcessIdIsNot(normalizedSlug, processId);
    }

    @Nonnull
    private String normalizeAndValidateSlug(@Nullable String slug) throws ResponseException {
        if (StringUtils.isNullOrEmpty(slug)) {
            throw ResponseException.badRequest("Der URL-Namespace des Prozesses darf nicht leer sein.");
        }

        var normalizedSlug = normalizeSlug(slug);

        if (normalizedSlug.length() < 3 || normalizedSlug.length() > MAX_SLUG_LENGTH) {
            throw ResponseException.badRequest("Der URL-Namespace des Prozesses muss zwischen 3 und 128 Zeichen lang sein.");
        }

        if (!normalizedSlug.matches("^[a-z0-9-]+$")) {
            throw ResponseException.badRequest("Der URL-Namespace des Prozesses darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen.");
        }

        return normalizedSlug;
    }

    @Nonnull
    private String normalizeSlug(@Nonnull String slug) {
        return slug.trim().toLowerCase(Locale.ROOT);
    }

    private void validateSlugIsAvailable(@Nonnull String slug,
                                         @Nullable Integer processId) throws ResponseException {
        if (processId == null) {
            if (processDefinitionRepository.existsBySlug(slug)) {
                throw ResponseException.conflict("Es existiert bereits ein Prozess mit diesem URL-Namespace.");
            }

            if (processSlugHistoryRepository.existsById(slug)) {
                throw ResponseException.conflict("Es existiert bereits ein Prozess, der diesen URL-Namespace zuvor verwendet hat.");
            }

            return;
        }

        if (processDefinitionRepository.existsBySlugAndIdIsNot(slug, processId)) {
            throw ResponseException.conflict("Es existiert bereits ein Prozess mit diesem URL-Namespace.");
        }

        if (processSlugHistoryRepository.existsBySlugAndProcessIdIsNot(slug, processId)) {
            throw ResponseException.conflict("Es existiert bereits ein Prozess, der diesen URL-Namespace zuvor verwendet hat.");
        }
    }
}
