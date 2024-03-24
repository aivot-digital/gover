package de.aivot.GoverBackend.utils;

import de.aivot.GoverBackend.models.entities.Department;
import de.aivot.GoverBackend.repositories.DepartmentRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class TestUtils {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PermissionService permissionService;

    public TestUtils(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            ApiKeyRepository apiKeyRepository,
            PermissionService permissionService
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.permissionService = permissionService;
    }

    public User createTestUser(boolean admin) {
        User testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail(generateRandomMailAddress());
        testUser.setPassword("invalid-password");
        testUser.setAdmin(admin);
        return userRepository.save(testUser);
    }

    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    public ApiKey createTestApiKey(
            Permissions permissions
    ) {
        ApiKey testApiKey = new ApiKey();

        testApiKey.setKey(UUID.randomUUID().toString());
        testApiKey.setTitle("Test Api Key");
        testApiKey.setDescription("Test Api Key Description");
        testApiKey.setExpires(LocalDateTime.now().plusHours(3));

        testApiKey.setApiKeys(permissions.getApiKeys());
        testApiKey.setApplications(permissions.getApplications());
        testApiKey.setAssets(permissions.getAssets());
        testApiKey.setDepartments(permissions.getDepartments());
        testApiKey.setDestinations(permissions.getDestinations());
        testApiKey.setPresets(permissions.getPresets());
        testApiKey.setProviderLinks(permissions.getProviderLinks());
        testApiKey.setSubmissions(permissions.getSubmissions());
        testApiKey.setSystemConfigs(permissions.getSystemConfigs());
        testApiKey.setThemes(permissions.getThemes());
        testApiKey.setUsers(permissions.getUsers());

        return apiKeyRepository.save(testApiKey);
    }

    public void deleteApiKey(ApiKey apiKey) {
        apiKeyRepository.delete(apiKey);
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
        return departmentRepository.save(testDepartment);
    }

    public void deleteDepartment(Department department) {
        departmentRepository.delete(department);
    }

    public Authentication createAuthentication(User user) {
        return createAuthentication((Object) user);
    }

    public Authentication createAuthentication(ApiKey apiKey) {
        return createAuthentication((Object) apiKey);
    }

    private Authentication createAuthentication(Object source) {
        Optional<Principal> optPrincipal = permissionService.createPrincipal(source);
        return new UsernamePasswordAuthenticationToken(optPrincipal.orElseGet(() -> null), null, null);
    }

    public String generateRandomMailAddress() {
        return "test-" + UUID.randomUUID() + "@aivot.de";
    }
}
