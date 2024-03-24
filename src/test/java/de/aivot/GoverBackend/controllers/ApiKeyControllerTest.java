package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.enums.PermissionLevel;
import de.aivot.GoverBackend.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiKeyControllerTest {
    private final ApiKeyController controller;

    private final TestUtils testUtils;

    private final ApiKeyRepository apiKeyRepository;

    private User testAdmin;
    private User testUser;
    private ApiKey testApiKeyReadWrite;
    private ApiKey testApiKeyRead;
    private ApiKey testApiKeyNone;

    @Autowired
    ApiKeyControllerTest(
            ApiKeyController controller,
            TestUtils testUtils,
            ApiKeyRepository apiKeyRepository
    ) {
        this.controller = controller;
        this.testUtils = testUtils;
        this.apiKeyRepository = apiKeyRepository;
    }

    @BeforeEach
    void setUp() {
        testAdmin = testUtils.createTestUser(true);
        testUser = testUtils.createTestUser(false);
        testApiKeyReadWrite = testUtils.createTestApiKey(new Permissions().setApiKeys(PermissionLevel.Write));
        testApiKeyRead = testUtils.createTestApiKey(new Permissions().setApiKeys(PermissionLevel.Read));
        testApiKeyNone = testUtils.createTestApiKey(new Permissions().setApiKeys(PermissionLevel.None));
    }

    @AfterEach
    void tearDown() {
        testUtils.deleteUser(testAdmin);
        testUtils.deleteUser(testUser);
        testUtils.deleteApiKey(testApiKeyReadWrite);
        testUtils.deleteApiKey(testApiKeyRead);
        testUtils.deleteApiKey(testApiKeyNone);
    }

    @Test
    void testApiKeyValidation() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Function<ApiKey, Boolean> validate = (apiKey) -> validator.validate(apiKey).isEmpty();

        ApiKey newApiKey = createNewApiKey();
        newApiKey.setKey("");
        assertTrue(validate.apply(newApiKey));

        newApiKey.setKey(null);
        assertFalse(validate.apply(newApiKey));
        newApiKey.setKey("");

        newApiKey.setExpires(null);
        assertFalse(validate.apply(newApiKey));
        newApiKey.setExpires(LocalDateTime.now());

    }

    @Test
    void list() {
        Collection<ApiKey> result;

        //region User Tests

        result = controller.list(testUtils.createAuthentication(testAdmin));
        assertEquals(apiKeyRepository.findAll().size(), result.size());

        try {
            controller.list(testUtils.createAuthentication(testUser));
            assert false;
        } catch (ResponseStatusException ex) {
            assertEquals(403, ex.getStatus().value());
        } catch (Exception ex) {
            assert false;
        }

        //endregion

        //region API Key Tests

        result = controller.list(testUtils.createAuthentication(testApiKeyReadWrite));
        assertEquals(apiKeyRepository.findAll().size(), result.size());

        result = controller.list(testUtils.createAuthentication(testApiKeyRead));
        assertEquals(apiKeyRepository.findAll().size(), result.size());

        try {
            controller.list(testUtils.createAuthentication(testApiKeyNone));
            assert false;
        } catch (ResponseStatusException ex) {
            assertEquals(403, ex.getStatus().value());
        } catch (Exception ex) {
            assert false;
        }

        //endregion
    }

    @Test
    void create() {
        ApiKey newApiKey = createNewApiKey();

        ApiKey result;

        //region User Tests

        result = controller.create(testUtils.createAuthentication(testAdmin), newApiKey);
        assertTrue(apiKeyRepository.findById(result.getKey()).isPresent());

        try {
            controller.create(testUtils.createAuthentication(testUser), newApiKey);
            assert false;
        } catch (ResponseStatusException ex) {
            assertEquals(403, ex.getStatus().value());
        } catch (Exception ex) {
            assert false;
        }

        //endregion

        //region API Key Tests

        result = controller.create(testUtils.createAuthentication(testApiKeyReadWrite), newApiKey);
        assertTrue(apiKeyRepository.findById(result.getKey()).isPresent());

        try {
            controller.create(testUtils.createAuthentication(testApiKeyRead), newApiKey);
            assert false;
        } catch (ResponseStatusException ex) {
            assertEquals(403, ex.getStatus().value());
        } catch (Exception ex) {
            assert false;
        }

        try {
            controller.create(testUtils.createAuthentication(testApiKeyNone), newApiKey);
            assert false;
        } catch (ResponseStatusException ex) {
            assertEquals(403, ex.getStatus().value());
        } catch (Exception ex) {
            assert false;
        }

        //endregion
    }

    @Test
    void retrieve() {
        ApiKey result;

        //region User Tests

        result = controller.retrieve(testUtils.createAuthentication(testAdmin), testApiKeyNone.getKey());
        assertEquals(result.getKey(), testApiKeyNone.getKey());

        try {
            controller.retrieve(testUtils.createAuthentication(testUser), testApiKeyNone.getKey());
            assert false;
        } catch (ResponseStatusException ex) {
            assertEquals(403, ex.getStatus().value());
        } catch (Exception ex) {
            assert false;
        }

        //endregion

        //region API Key Tests

        result = controller.retrieve(testUtils.createAuthentication(testApiKeyReadWrite), testApiKeyNone.getKey());
        assertEquals(result.getKey(), testApiKeyNone.getKey());

        result = controller.retrieve(testUtils.createAuthentication(testApiKeyRead), testApiKeyNone.getKey());
        assertEquals(result.getKey(), testApiKeyNone.getKey());

        try {
            controller.retrieve(testUtils.createAuthentication(testApiKeyNone), testApiKeyNone.getKey());
            assert false;
        } catch (ResponseStatusException ex) {
            assertEquals(403, ex.getStatus().value());
        } catch (Exception ex) {
            assert false;
        }

        //endregion
    }

    @Test
    void update() {
        ApiKey existingApiKey = createNewApiKey();
        existingApiKey = apiKeyRepository.save(existingApiKey);

        ApiKey result;

        //region User Tests

        existingApiKey.setTitle("Updated Title Admin");
        result = controller.update(testUtils.createAuthentication(testAdmin), existingApiKey.getKey(), existingApiKey);
        assertEquals(result.getTitle(), existingApiKey.getTitle());
        assertEquals(apiKeyRepository.findById(existingApiKey.getKey()).get().getTitle(), existingApiKey.getTitle());

        existingApiKey.setTitle("Updated Title User");
        try {
            controller.update(testUtils.createAuthentication(testUser), existingApiKey.getKey(), existingApiKey);
            assert false;
        } catch (ResponseStatusException ex) {
            assertEquals(403, ex.getStatus().value());
            apiKeyRepository.findById(existingApiKey.getKey()).ifPresent(apiKey -> assertEquals(apiKey.getTitle(), "Updated Title Admin"));
        } catch (Exception ex) {
            assert false;
        }

        //endregion

        //region API Key Tests

        existingApiKey.setTitle("Updated Title Api Key ReadWrite");
        result = controller.update(testUtils.createAuthentication(testApiKeyReadWrite), existingApiKey.getKey(), existingApiKey);
        assertEquals(result.getTitle(), existingApiKey.getTitle());
        assertEquals(apiKeyRepository.findById(existingApiKey.getKey()).get().getTitle(), existingApiKey.getTitle());


        existingApiKey.setTitle("Updated Title Api Key Read");
        try {
            controller.update(testUtils.createAuthentication(testApiKeyRead), existingApiKey.getKey(), existingApiKey);
            assert false;
        } catch (ResponseStatusException ex) {
            assertEquals(403, ex.getStatus().value());
            apiKeyRepository.findById(existingApiKey.getKey()).ifPresent(apiKey -> assertEquals(apiKey.getTitle(), "Updated Title Api Key ReadWrite"));
        } catch (Exception ex) {
            assert false;
        }

        existingApiKey.setTitle("Updated Title Api Key None");
        try {
            controller.update(testUtils.createAuthentication(testApiKeyRead), existingApiKey.getKey(), existingApiKey);
            assert false;
        } catch (ResponseStatusException ex) {
            assertEquals(403, ex.getStatus().value());
            apiKeyRepository.findById(existingApiKey.getKey()).ifPresent(apiKey -> assertEquals(apiKey.getTitle(), "Updated Title Api Key ReadWrite"));
        } catch (Exception ex) {
            assert false;
        }

        //endregion
    }

    @Test
    void destroy() {
        ApiKey newApiKey;

        //region User Tests

        newApiKey = apiKeyRepository.save(createNewApiKey());
        controller.destroy(testUtils.createAuthentication(testAdmin), newApiKey.getKey());
        assertFalse(apiKeyRepository.existsById(newApiKey.getKey()));

        newApiKey = apiKeyRepository.save(createNewApiKey());
        try {
            controller.destroy(testUtils.createAuthentication(testUser), newApiKey.getKey());
            assert false;
        } catch (ResponseStatusException ex) {
            assertEquals(403, ex.getStatus().value());
            assertTrue(apiKeyRepository.existsById(newApiKey.getKey()));
        } catch (Exception ex) {
            assert false;
        }
        apiKeyRepository.delete(newApiKey);

        //endregion

        //region API Key Tests

        newApiKey = apiKeyRepository.save(createNewApiKey());
        controller.destroy(testUtils.createAuthentication(testApiKeyReadWrite), newApiKey.getKey());
        assertFalse(apiKeyRepository.existsById(newApiKey.getKey()));

        try {
            newApiKey = apiKeyRepository.save(createNewApiKey());
            controller.destroy(testUtils.createAuthentication(testApiKeyRead), newApiKey.getKey());
            assert false;
        } catch (ResponseStatusException ex) {
            assertEquals(403, ex.getStatus().value());
            assertTrue(apiKeyRepository.existsById(newApiKey.getKey()));
        } catch (Exception ex) {
            assert false;
        }
        apiKeyRepository.delete(newApiKey);

        try {
            newApiKey = apiKeyRepository.save(createNewApiKey());
            controller.destroy(testUtils.createAuthentication(testApiKeyNone), newApiKey.getKey());
            assert false;
        } catch (ResponseStatusException ex) {
            assertEquals(403, ex.getStatus().value());
            assertTrue(apiKeyRepository.existsById(newApiKey.getKey()));
        } catch (Exception ex) {
            assert false;
        }
        apiKeyRepository.delete(newApiKey);

        //endregion
    }

    private static ApiKey createNewApiKey() {
        ApiKey newApiKey = new ApiKey();

        newApiKey.setTitle("Test Api Key");
        newApiKey.setDescription("Test Api Key Description");
        newApiKey.setExpires(LocalDateTime.now().plusHours(3));

        newApiKey.setApiKeys(PermissionLevel.None);
        newApiKey.setApplications(PermissionLevel.None);
        newApiKey.setAssets(PermissionLevel.None);
        newApiKey.setDepartments(PermissionLevel.None);
        newApiKey.setDestinations(PermissionLevel.None);
        newApiKey.setPresets(PermissionLevel.None);
        newApiKey.setProviderLinks(PermissionLevel.None);
        newApiKey.setSubmissions(PermissionLevel.None);
        newApiKey.setSystemConfigs(PermissionLevel.None);
        newApiKey.setThemes(PermissionLevel.None);
        newApiKey.setUsers(PermissionLevel.None);

        return newApiKey;
    }
}
