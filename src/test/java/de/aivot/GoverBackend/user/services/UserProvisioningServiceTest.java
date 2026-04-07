package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.mail.services.UserOnboardingMailService;
import de.aivot.GoverBackend.user.dtos.CreateUserRequestDTO;
import de.aivot.GoverBackend.userRoles.entities.SystemRoleEntity;
import de.aivot.GoverBackend.userRoles.repositories.SystemRoleRepository;
import de.aivot.GoverBackend.user.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserProvisioningServiceTest {
    @Mock
    private UserService userService;
    @Mock
    private SystemRoleRepository systemRoleRepository;
    @Mock
    private UserOnboardingMailService userOnboardingMailService;

    private UserProvisioningService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UserProvisioningService(
                userService,
                systemRoleRepository,
                userOnboardingMailService
        );
    }

    @Test
    void provision_ReturnsManualCredentials_WhenMailDispatchIsDisabled() throws Exception {
        var requestUser = createRequestUser();

        when(systemRoleRepository.findById(11)).thenReturn(Optional.of(createSystemRole()));
        when(userService.create(eq(requestUser), any(String.class), eq(List.of("VERIFY_EMAIL")))).thenReturn(createCreatedUser());

        var result = service.provision(new CreateUserRequestDTO(requestUser, false));

        assertFalse(result.initialCredentialsSentByEmail());
        assertNull(result.initialCredentialsDeliveryError());
        assertNotNull(result.initialCredentials());
        assertEquals("Jane Doe", result.initialCredentials().fullName());
        assertEquals("jane.doe@example.com", result.initialCredentials().email());
        assertEquals("Sachbearbeitung", result.initialCredentials().systemRoleName());
        assertEquals(16, result.initialCredentials().temporaryPassword().length());
        assertTrue(result.initialCredentials().temporaryPassword().matches(".*[A-Z].*"));
        assertTrue(result.initialCredentials().temporaryPassword().matches(".*[a-z].*"));
        assertTrue(result.initialCredentials().temporaryPassword().matches(".*\\d.*"));
        assertTrue(result.initialCredentials().temporaryPassword().matches(".*[!@$%*?&].*"));

        verify(userOnboardingMailService, never()).send(any(UserEntity.class), any());
    }

    @Test
    void provision_SendsMail_WhenMailDispatchIsEnabled() throws Exception {
        var requestUser = createRequestUser();

        when(systemRoleRepository.findById(11)).thenReturn(Optional.of(createSystemRole()));
        when(userService.create(eq(requestUser), any(String.class), eq(List.of("VERIFY_EMAIL")))).thenReturn(createCreatedUser());
        when(userOnboardingMailService.isSendingConfigured()).thenReturn(true);

        var result = service.provision(new CreateUserRequestDTO(requestUser, true));

        assertTrue(result.initialCredentialsSentByEmail());
        assertNull(result.initialCredentialsDeliveryError());
        assertNull(result.initialCredentials());

        verify(userOnboardingMailService).send(any(UserEntity.class), any());
    }

    @Test
    void provision_FallsBackToManualCredentials_WhenMailDispatchIsNotConfigured() throws Exception {
        var requestUser = createRequestUser();

        when(systemRoleRepository.findById(11)).thenReturn(Optional.of(createSystemRole()));
        when(userService.create(eq(requestUser), any(String.class), eq(List.of("VERIFY_EMAIL")))).thenReturn(createCreatedUser());
        when(userOnboardingMailService.isSendingConfigured()).thenReturn(false);

        var result = service.provision(new CreateUserRequestDTO(requestUser, true));

        assertFalse(result.initialCredentialsSentByEmail());
        assertEquals("Der automatische E-Mail-Versand ist derzeit nicht konfiguriert.", result.initialCredentialsDeliveryError());
        assertNotNull(result.initialCredentials());

        verify(userOnboardingMailService, never()).send(any(UserEntity.class), any());
    }

    private static UserEntity createRequestUser() {
        return new UserEntity()
                .setEmail("jane.doe@example.com")
                .setFirstName("Jane")
                .setLastName("Doe")
                .setEnabled(true)
                .setVerified(false)
                .setDeletedInIdp(false)
                .setSystemRoleId(11);
    }

    private static UserEntity createCreatedUser() {
        return new UserEntity()
                .setId("user-123")
                .setEmail("jane.doe@example.com")
                .setFirstName("Jane")
                .setLastName("Doe")
                .setFullName("Jane Doe")
                .setEnabled(true)
                .setVerified(false)
                .setDeletedInIdp(false)
                .setSystemRoleId(11);
    }

    private static SystemRoleEntity createSystemRole() {
        return new SystemRoleEntity()
                .setId(11)
                .setName("Sachbearbeitung")
                .setPermissions(List.of("user.read"));
    }
}
