package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.models.entities.Department;
import de.aivot.GoverBackend.models.entities.DepartmentMembership;
import de.aivot.GoverBackend.repositories.DepartmentMembershipRepository;
import de.aivot.GoverBackend.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DepartmentMembershipControllerTest {
    private final TestUtils testUtils;

    private final DepartmentMembershipController controller;
    private final DepartmentMembershipRepository departmentMembershipRepository;

    private User testUser;
    private User testUser2;
    private User testAdmin;
    private Department testDepartment;
    private DepartmentMembership testMembership;

    @Autowired
    DepartmentMembershipControllerTest(
            TestUtils testUtils,
            DepartmentMembershipController controller,
            DepartmentMembershipRepository departmentMembershipRepository
    ) {
        this.testUtils = testUtils;
        this.controller = controller;
        this.departmentMembershipRepository = departmentMembershipRepository;
    }

    @BeforeEach
    void setUp() {
        testAdmin = testUtils.createTestUser(true);
        testUser = testUtils.createTestUser(false);
        testUser2 = testUtils.createTestUser(false);

        testDepartment = testUtils.createTestDepartment();

        testMembership = new DepartmentMembership();
        testMembership.setUser(testUser);
        testMembership.setDepartment(testDepartment);
        testMembership.setRole(UserRole.Editor);
        departmentMembershipRepository.save(testMembership);
    }

    @AfterEach
    void tearDown() {
        departmentMembershipRepository.delete(testMembership);
        testUtils.deleteDepartment(testDepartment);
        testUtils.deleteUser(testAdmin);
        testUtils.deleteUser(testUser);
        testUtils.deleteUser(testUser2);
    }

    @Test
    public void testList() {
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                testAdmin,
                null,
                new LinkedList<>()
        );

        var memberships = controller.list(adminAuth, null, null);
        assertEquals(1, memberships.size());

        Authentication userAuth = new UsernamePasswordAuthenticationToken(
                testUser,
                null,
                new LinkedList<>()
        );

        memberships = controller.list(userAuth, null, null);
        assertEquals(1, memberships.size());

        Authentication user2Auth = new UsernamePasswordAuthenticationToken(
                testUser2,
                null,
                new LinkedList<>()
        );

        memberships = controller.list(user2Auth, null, null);
        assertEquals(0, memberships.size());
    }
}
