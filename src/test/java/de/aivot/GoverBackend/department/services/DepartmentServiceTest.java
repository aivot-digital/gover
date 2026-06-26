package de.aivot.GoverBackend.department.services;

import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.system.services.SystemService;
import de.aivot.GoverBackend.theme.repositories.ThemeRepository;
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

        var service = new DepartmentService(
                departmentRepository,
                mock(FormRepository.class),
                mock(ThemeRepository.class),
                mock(SystemService.class)
        );

        var entity = new DepartmentEntity()
                .setName("Organisationseinheit")
                .setParentDepartmentId(42);

        var exception = assertThrows(ResponseException.class, () -> service.create(entity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Die ausgewählte übergeordnete Organisationseinheit existiert nicht.", exception.getTitle());
        verify(departmentRepository, never()).save(any(DepartmentEntity.class));
    }
}
