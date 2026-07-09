package de.aivot.gover.backend.department.services;

import de.aivot.gover.backend.department.entities.DepartmentEntity;
import de.aivot.gover.backend.department.repositories.DepartmentRepository;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.system.services.SystemService;
import de.aivot.gover.backend.theme.repositories.ThemeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepartmentServiceTest {
    @Test
    void createShouldRejectMissingParentDepartment() {
        var departmentRepository = mock(DepartmentRepository.class);
        when(departmentRepository.findById(42)).thenReturn(Optional.empty());

        var service = createService(departmentRepository);

        var entity = new DepartmentEntity()
                .setName("Organisationseinheit")
                .setParentDepartmentId(42);

        var exception = assertThrows(ResponseException.class, () -> service.create(entity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Die ausgewählte übergeordnete Organisationseinheit existiert nicht.", exception.getTitle());
        verify(departmentRepository, never()).save(any(DepartmentEntity.class));
    }

    @Test
    void createShouldNormalizeValidSupportPhoneNumbers() throws ResponseException {
        var departmentRepository = mock(DepartmentRepository.class);
        when(departmentRepository.findById(42)).thenReturn(Optional.of(parentDepartment()));
        when(departmentRepository.save(any(DepartmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var service = createService(departmentRepository);

        var entity = childDepartment()
                .setSpecialSupportPhone("+49 30 123456")
                .setTechnicalSupportPhone("+49 40 123456");

        var createdDepartment = service.create(entity);

        assertEquals("+4930123456", createdDepartment.getSpecialSupportPhone());
        assertEquals("+4940123456", createdDepartment.getTechnicalSupportPhone());
    }

    @Test
    void createShouldRejectInvalidSupportPhoneNumbers() {
        var departmentRepository = mock(DepartmentRepository.class);
        when(departmentRepository.findById(42)).thenReturn(Optional.of(parentDepartment()));

        var service = createService(departmentRepository);

        var entity = childDepartment()
                .setSpecialSupportPhone("030 123456");

        var exception = assertThrows(ResponseException.class, () -> service.create(entity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(
                "Bitte geben Sie für „Kontakt-Telefonnummer für fachliche Unterstützung“ eine gültige Telefonnummer mit Ländervorwahl ein.",
                exception.getTitle()
        );
        verify(departmentRepository, never()).save(any(DepartmentEntity.class));
    }

    @Test
    void createShouldRejectSupportPhoneNumbersWithExtensions() {
        var departmentRepository = mock(DepartmentRepository.class);
        when(departmentRepository.findById(42)).thenReturn(Optional.of(parentDepartment()));

        var service = createService(departmentRepository);

        var entity = childDepartment()
                .setSpecialSupportPhone("+49 30 123456 ext. 7");

        var exception = assertThrows(ResponseException.class, () -> service.create(entity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(departmentRepository, never()).save(any(DepartmentEntity.class));
    }

    @Test
    void updateShouldAllowUnchangedLegacySupportPhoneNumbers() throws ResponseException {
        var departmentRepository = mock(DepartmentRepository.class);
        var existingDepartment = childDepartment()
                .setId(5)
                .setSpecialSupportPhone("030 123456");
        when(departmentRepository.findById(5)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(42)).thenReturn(Optional.of(parentDepartment()));
        when(departmentRepository.save(any(DepartmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var service = createService(departmentRepository);

        var updatedDepartment = childDepartment()
                .setSpecialSupportPhone("030 123456");

        var savedDepartment = service.update(5, updatedDepartment);

        assertEquals("030 123456", savedDepartment.getSpecialSupportPhone());
    }

    @Test
    void updateShouldAllowTrimmedUnchangedLegacySupportPhoneNumbers() throws ResponseException {
        var departmentRepository = mock(DepartmentRepository.class);
        var existingDepartment = childDepartment()
                .setId(5)
                .setSpecialSupportPhone(" 030 123456 ");
        when(departmentRepository.findById(5)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(42)).thenReturn(Optional.of(parentDepartment()));
        when(departmentRepository.save(any(DepartmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var service = createService(departmentRepository);

        var updatedDepartment = childDepartment()
                .setSpecialSupportPhone("030 123456");

        var savedDepartment = service.update(5, updatedDepartment);

        assertEquals(" 030 123456 ", savedDepartment.getSpecialSupportPhone());
    }

    @Test
    void updateShouldRejectChangedLegacySupportPhoneNumbers() {
        var departmentRepository = mock(DepartmentRepository.class);
        var existingDepartment = childDepartment()
                .setId(5)
                .setSpecialSupportPhone("030 123456");
        when(departmentRepository.findById(5)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(42)).thenReturn(Optional.of(parentDepartment()));

        var service = createService(departmentRepository);

        var updatedDepartment = childDepartment()
                .setSpecialSupportPhone("030 654321");

        var exception = assertThrows(ResponseException.class, () -> service.update(5, updatedDepartment));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(departmentRepository, never()).save(any(DepartmentEntity.class));
    }

    private static DepartmentService createService(DepartmentRepository departmentRepository) {
        return new DepartmentService(
                departmentRepository,
                mock(ThemeRepository.class),
                mock(SystemService.class)
        );
    }

    private static DepartmentEntity parentDepartment() {
        return new DepartmentEntity()
                .setId(42)
                .setName("Parent Department")
                .setDepth(0);
    }

    private static DepartmentEntity childDepartment() {
        return new DepartmentEntity()
                .setName("Organisationseinheit")
                .setParentDepartmentId(42)
                .setDepth(1);
    }
}
