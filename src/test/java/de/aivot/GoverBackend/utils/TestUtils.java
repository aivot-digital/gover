package de.aivot.GoverBackend.utils;

import de.aivot.GoverBackend.models.entities.Department;
import de.aivot.GoverBackend.models.entities.User;
import de.aivot.GoverBackend.repositories.DepartmentRepository;
import de.aivot.GoverBackend.repositories.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TestUtils {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public TestUtils(
            UserRepository userRepository,
            DepartmentRepository departmentRepository
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    public User createTestUser(boolean admin) {
        User testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail(generateRandomMailAddress());
        testUser.setPassword("invalid-password");
        testUser.setAdmin(admin);
        userRepository.save(testUser);
        return testUser;
    }

    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    public Department createTestDepartment() {
        Department testDepartment = new Department();
        testDepartment.setName("Test Department");
        testDepartment.setSpecialSupportAddress(generateRandomMailAddress());
        testDepartment.setTechnicalSupportAddress(generateRandomMailAddress());
        testDepartment.setAddress("test");
        testDepartment.setPrivacy("test");
        testDepartment.setImprint("test");
        testDepartment.setAccessibility("test");
        departmentRepository.save(testDepartment);
        return testDepartment;
    }

    public void deleteDepartment(Department department) {
        departmentRepository.delete(department);
    }

    public String generateRandomMailAddress() {
        return "test-" + UUID.randomUUID() + "@aivot.de";
    }
}
