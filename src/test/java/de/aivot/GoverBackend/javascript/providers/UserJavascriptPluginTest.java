package de.aivot.GoverBackend.javascript.providers;

import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.plugins.core.v1.javascript.UserJavascriptV1;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserJavascriptPluginTest {
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
    }

    @Test
    void get() {
        try (var jsService = new JavascriptEngine(new UserJavascriptV1(userService))) {
            when(userService.retrieve("user-1"))
                    .thenReturn(Optional.of(new UserEntity()
                            .setId("user-1")
                            .setEmail("user@example.org")
                            .setFirstName("Max")
                            .setLastName("Mustermann")
                            .setFullName("Max Mustermann")
                            .setEnabled(true)
                            .setVerified(true)
                            .setDeletedInIdp(false)
                            .setSystemRoleId(3)));

            var result = jsService.evaluateCode(new JavascriptCode().setCode("_users_v1.get('user-1');"));
            var user = assertInstanceOf(Map.class, result.asObject());

            assertEquals("user-1", user.get("id"));
            assertEquals("user@example.org", user.get("email"));
            assertEquals("Max", user.get("firstName"));
            assertEquals("Mustermann", user.get("lastName"));
            assertEquals("Max Mustermann", user.get("fullName"));
            assertEquals(true, user.get("enabled"));
            assertEquals(true, user.get("verified"));
            assertEquals(false, user.get("deletedInIdp"));
            assertEquals(3, user.get("systemRoleId"));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getReturnsNullForUnknownUser() {
        try (var jsService = new JavascriptEngine(new UserJavascriptV1(userService))) {
            when(userService.retrieve("unknown-user"))
                    .thenReturn(Optional.empty());

            var result = jsService.evaluateCode(new JavascriptCode().setCode("_users_v1.get('unknown-user');"));

            assertTrue(result.isNull());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getReturnsNullForBlankUserId() {
        try (var jsService = new JavascriptEngine(new UserJavascriptV1(userService))) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("_users_v1.get('');"));

            assertTrue(result.isNull());
        } catch (Exception e) {
            fail(e);
        }
    }
}
