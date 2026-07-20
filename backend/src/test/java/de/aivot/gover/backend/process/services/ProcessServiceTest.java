package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.process.entities.ProcessEntity;
import de.aivot.gover.backend.process.entities.ProcessSlugHistoryEntity;
import de.aivot.gover.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.gover.backend.process.repositories.ProcessRepository;
import de.aivot.gover.backend.process.repositories.ProcessSlugHistoryRepository;
import de.aivot.gover.backend.process.services.ProcessService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessServiceTest {
    private static final String USER_ID = "user-1";
    private static final String READ_PERMISSION = ProcessPermissionProvider.PROCESS_DEFINITION_READ;

    @Test
    void createShouldNormalizeExplicitSlug() throws Exception {
        var repository = mock(ProcessRepository.class);
        var processSlugHistoryRepository = mock(ProcessSlugHistoryRepository.class);
        var permissionService = mock(PermissionService.class);
        var service = new ProcessService(repository, processSlugHistoryRepository, permissionService);
        var process = new ProcessEntity()
                .setInternalTitle("Hundesteuer Antrag")
                .setDepartmentId(10)
                .setSlug("Hundesteuer-Antrag")
                .setVersionCount(0);

        when(repository.save(any(ProcessEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(process);

        assertEquals("hundesteuer-antrag", result.getSlug());
        assertEquals(10, result.getDepartmentId());
        verify(repository).save(process);
    }

    @Test
    void createShouldRejectMissingSlug() {
        var repository = mock(ProcessRepository.class);
        var processSlugHistoryRepository = mock(ProcessSlugHistoryRepository.class);
        var permissionService = mock(PermissionService.class);
        var service = new ProcessService(repository, processSlugHistoryRepository, permissionService);
        var process = new ProcessEntity()
                .setInternalTitle("Hundesteuer Antrag")
                .setDepartmentId(10)
                .setSlug("")
                .setVersionCount(0);

        assertThrows(ResponseException.class, () -> service.create(process));
        verify(repository, never()).save(any(ProcessEntity.class));
    }

    @Test
    void retrieveBySlugOrHistoryShouldResolveHistoricalSlug() throws Exception {
        var repository = mock(ProcessRepository.class);
        var processSlugHistoryRepository = mock(ProcessSlugHistoryRepository.class);
        var permissionService = mock(PermissionService.class);
        var service = new ProcessService(repository, processSlugHistoryRepository, permissionService);
        var process = new ProcessEntity()
                .setId(42)
                .setInternalTitle("Hundesteuer Antrag")
                .setDepartmentId(10)
                .setSlug("hundesteuer")
                .setVersionCount(1);

        when(repository.findBySlug("alter-slug"))
                .thenReturn(Optional.empty());
        when(processSlugHistoryRepository.findBySlug("alter-slug"))
                .thenReturn(Optional.of(new ProcessSlugHistoryEntity("alter-slug", 42)));
        when(repository.findById(42))
                .thenReturn(Optional.of(process));

        var result = service.retrieveBySlugOrHistory("ALTER-SLUG");

        assertTrue(result.isPresent());
        assertSame(process, result.get());
    }

    @Test
    void listSlugHistoryShouldReturnEntriesForExistingProcess() throws Exception {
        var repository = mock(ProcessRepository.class);
        var processSlugHistoryRepository = mock(ProcessSlugHistoryRepository.class);
        var permissionService = mock(PermissionService.class);
        var service = new ProcessService(repository, processSlugHistoryRepository, permissionService);
        var history = List.of(
                new ProcessSlugHistoryEntity("alter-slug", 42),
                new ProcessSlugHistoryEntity("noch-aelterer-slug", 42)
        );

        when(repository.existsById(42))
                .thenReturn(true);
        when(processSlugHistoryRepository.findAllByProcessIdOrderByCreatedDesc(42))
                .thenReturn(history);

        var result = service.listSlugHistory(42);

        assertSame(history, result);
        verify(processSlugHistoryRepository).findAllByProcessIdOrderByCreatedDesc(42);
    }

    @Test
    void clearSlugHistoryShouldDeleteEntriesForExistingProcess() throws Exception {
        var repository = mock(ProcessRepository.class);
        var processSlugHistoryRepository = mock(ProcessSlugHistoryRepository.class);
        var permissionService = mock(PermissionService.class);
        var service = new ProcessService(repository, processSlugHistoryRepository, permissionService);

        when(repository.existsById(42))
                .thenReturn(true);

        service.clearSlugHistory(42);

        verify(processSlugHistoryRepository).deleteAllByProcessId(42);
    }

    @Test
    void isSlugAvailableShouldAllowCurrentProcessSlug() throws Exception {
        var repository = mock(ProcessRepository.class);
        var processSlugHistoryRepository = mock(ProcessSlugHistoryRepository.class);
        var permissionService = mock(PermissionService.class);
        var service = new ProcessService(repository, processSlugHistoryRepository, permissionService);

        when(repository.existsBySlugAndIdIsNot("hundesteuer", 42))
                .thenReturn(false);
        when(processSlugHistoryRepository.existsBySlugAndProcessIdIsNot("hundesteuer", 42))
                .thenReturn(false);

        var result = service.isSlugAvailable("Hundesteuer", 42);

        assertTrue(result);
    }

    @Test
    void listAllByAccessibleForUserShouldReturnUnscopedResultForSystemReadPermission() throws Exception {
        var repository = mock(ProcessRepository.class);
        var processSlugHistoryRepository = mock(ProcessSlugHistoryRepository.class);
        var permissionService = mock(PermissionService.class);
        var service = new ProcessService(repository, processSlugHistoryRepository, permissionService);
        var pageable = PageRequest.of(0, 10);
        Specification<ProcessEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        var page = Page.<ProcessEntity>empty(pageable);

        when(permissionService.hasSystemPermission(USER_ID, READ_PERMISSION))
                .thenReturn(true);
        when(repository.findAll(specification, pageable))
                .thenReturn(page);

        var result = service.listAllByAccessibleForUser(pageable, USER_ID, specification);

        assertSame(page, result);
        verify(repository).findAll(specification, pageable);
        verify(permissionService, never()).getDepartmentsWithPermission(any(), any());
        verify(repository, never()).getProcessIdsWithPermission(any(), any());
    }

    @Test
    void listAllByAccessibleForUserShouldReturnEmptyPageWhenUserHasNoScopedAccess() throws Exception {
        var repository = mock(ProcessRepository.class);
        var processSlugHistoryRepository = mock(ProcessSlugHistoryRepository.class);
        var permissionService = mock(PermissionService.class);
        var service = new ProcessService(repository, processSlugHistoryRepository, permissionService);
        var pageable = PageRequest.of(0, 10);

        when(permissionService.hasSystemPermission(USER_ID, READ_PERMISSION))
                .thenReturn(false);
        when(permissionService.getDepartmentsWithPermission(USER_ID, READ_PERMISSION))
                .thenReturn(List.of());
        when(repository.getProcessIdsWithPermission(USER_ID, READ_PERMISSION))
                .thenReturn(List.of());

        var result = service.listAllByAccessibleForUser(pageable, USER_ID, null);

        assertTrue(result.isEmpty());
        assertEquals(pageable, result.getPageable());
        verify(repository, never()).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void listAllByAccessibleForUserShouldScopeByDepartmentsAndExplicitProcessAccess() throws Exception {
        var repository = mock(ProcessRepository.class);
        var processSlugHistoryRepository = mock(ProcessSlugHistoryRepository.class);
        var permissionService = mock(PermissionService.class);
        var service = new ProcessService(repository, processSlugHistoryRepository, permissionService);
        var pageable = PageRequest.of(0, 10);
        var page = Page.<ProcessEntity>empty(pageable);

        when(permissionService.hasSystemPermission(USER_ID, READ_PERMISSION))
                .thenReturn(false);
        when(permissionService.getDepartmentsWithPermission(USER_ID, READ_PERMISSION))
                .thenReturn(List.of(10));
        when(repository.getProcessIdsWithPermission(USER_ID, READ_PERMISSION))
                .thenReturn(List.of(21));
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var result = service.listAllByAccessibleForUser(pageable, USER_ID, null);

        assertSame(page, result);

        var specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(specificationCaptor.capture(), eq(pageable));
        assertAccessSpecificationMatchesDepartmentsOrProcesses(specificationCaptor.getValue());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertAccessSpecificationMatchesDepartmentsOrProcesses(Specification specification) {
        Root<ProcessEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path departmentIdPath = mock(Path.class);
        Path processIdPath = mock(Path.class);
        Predicate departmentPredicate = mock(Predicate.class);
        Predicate processPredicate = mock(Predicate.class);
        Predicate accessPredicate = mock(Predicate.class);

        when(root.get("departmentId")).thenReturn(departmentIdPath);
        when(root.get("id")).thenReturn(processIdPath);
        when(departmentIdPath.in(List.of(10))).thenReturn(departmentPredicate);
        when(processIdPath.in(List.of(21))).thenReturn(processPredicate);
        when(criteriaBuilder.or(any(Predicate[].class))).thenReturn(accessPredicate);

        assertSame(accessPredicate, specification.toPredicate(root, query, criteriaBuilder));

        var predicateCaptor = ArgumentCaptor.forClass(Predicate[].class);
        verify(criteriaBuilder).or(predicateCaptor.capture());
        assertArrayEquals(
                new Predicate[]{departmentPredicate, processPredicate},
                predicateCaptor.getValue()
        );
    }
}
