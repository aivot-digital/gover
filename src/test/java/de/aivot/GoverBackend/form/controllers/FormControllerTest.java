package de.aivot.GoverBackend.form.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.entities.DepartmentMembershipEntity;
import de.aivot.GoverBackend.department.repositories.DepartmentMembershipRepository;
import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.form.dtos.FormRequestDTO;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.elements.models.RootElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

@SpringBootTest
public class FormControllerTest {
    @Autowired
    private FormController controller;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DepartmentMembershipRepository departmentMembershipRepository;

    @Mock
    private Jwt jwt;

    private DepartmentEntity department;

    @BeforeEach
    void setUp() {
        departmentRepository.deleteAll();
        departmentMembershipRepository.deleteAll();

        when(jwt.getId()).thenReturn("1");

        department = new DepartmentEntity();
        department.setName("Test Department");
        departmentRepository.save(department);

        var mem = new DepartmentMembershipEntity();
        mem.setDepartmentId(department.getId());
        mem.setUserId(jwt.getId());
        mem.setRole(UserRole.Admin);
        departmentMembershipRepository.save(mem);
    }

    @Test
    void contextLoads() throws Exception {
        assertThat(controller).isNotNull();
    }

    @Test
    void testCreateForm() throws ResponseException {
        var dto = new FormRequestDTO(
                "test-form",
                "1.0.0",
                "Test Formular",
                null,
                null,
                new RootElement(Map.of()),
                department.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(controller.create(jwt , dto)).isNotNull();
    }

    @Test
    void testGetForm() throws ResponseException {
        assertThat(controller.retrieve(jwt , 1)).isNotNull();
    }
}
