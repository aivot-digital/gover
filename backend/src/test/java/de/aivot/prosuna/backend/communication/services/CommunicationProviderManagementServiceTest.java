package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.audit.models.AuditLogPayload;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.communication.permissions.CommunicationProviderPermissionProvider;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import org.springframework.security.oauth2.jwt.Jwt;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderBindingRepository;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderRepository;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.content.AlertContentElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.enums.AlertType;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.repositories.IdentityProviderRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunicationProviderManagementServiceTest {
    private final CommunicationProviderRepository providerRepository = mock(CommunicationProviderRepository.class);
    private final CommunicationProviderBindingRepository bindingRepository = mock(CommunicationProviderBindingRepository.class);
    private final CommunicationProviderDefinitionService definitionService = mock(CommunicationProviderDefinitionService.class);
    private final CommunicationProviderConfigurationService configurationService = mock(CommunicationProviderConfigurationService.class);
    private final IdentityProviderRepository identityProviderRepository = mock(IdentityProviderRepository.class);
    private final CommunicationProviderDefinition<Object, Object> definition = mock(CommunicationProviderDefinition.class);

    private final PermissionService permissionService = mock(PermissionService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final ScopedAuditService scopedAuditService = mock(ScopedAuditService.class);
    private final Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("staff-1").build();
    private CommunicationProviderManagementService service;
    private CommunicationProviderEntity provider;
    private IdentityProviderEntity identityProvider;

    @BeforeEach
    void setUp() {
        when(auditService.createScopedAuditService(CommunicationProviderManagementService.class, "Kommunikationsanbindungen")).thenReturn(scopedAuditService);
        when(scopedAuditService.create()).thenAnswer(invocation -> AuditLogPayload.create(scopedAuditService));
        service = new CommunicationProviderManagementService(
                providerRepository,
                bindingRepository,
                definitionService,
                configurationService,
                identityProviderRepository,
                permissionService,
                auditService
        );
        provider = provider(7, true, false);
        identityProvider = identityProvider(true, false);

        when(providerRepository.findById(provider.getId())).thenReturn(Optional.of(provider));
        when(providerRepository.findByIdForUpdate(provider.getId())).thenReturn(Optional.of(provider));
        when(identityProviderRepository.findByKeyForUpdate(identityProvider.getKey()))
                .thenReturn(Optional.of(identityProvider));
        when(definitionService.retrieveProviderDefinition(
                provider.getCommunicationProviderDefinitionKey(),
                provider.getCommunicationProviderDefinitionVersion()
        )).thenReturn(Optional.of(definition));
        when(definition.getSupportedIdentityProviderTypes()).thenReturn(List.of(IdentityProviderType.BundId));
        when(definition.supportsIdentityProvider(identityProvider)).thenReturn(true);
        when(bindingRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(providerRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void mockReference(int id) {
        var reference = mock(CommunicationProviderBindingRepository.BindingReference.class);
        when(reference.getCommunicationProviderId()).thenReturn(provider.getId());
        when(reference.getIdentityProviderKey()).thenReturn(identityProvider.getKey());
        when(bindingRepository.findReferenceById(id)).thenReturn(Optional.of(reference));
    }

    @Test
    void appendsAfterExistingBindingsAndNormalizesLegacyPositions() throws Exception {
        var first = binding("First").setId(1).setPosition(4);
        var second = binding("Second").setId(2).setPosition(4).setEnabled(false);
        when(bindingRepository.findAllByIdentityProviderKeyOrderByPositionAscNameAscIdAsc(identityProvider.getKey())).thenReturn(List.of(first, second));
        var created = service.createBinding(binding("Third").setPosition(-10));
        assertEquals(0, first.getPosition());
        assertEquals(1, second.getPosition());
        assertEquals(2, created.getPosition());
        var order = inOrder(identityProviderRepository, bindingRepository);
        order.verify(identityProviderRepository).findByKeyForUpdate(identityProvider.getKey());
        order.verify(bindingRepository).findAllByIdentityProviderKeyOrderByPositionAscNameAscIdAsc(identityProvider.getKey());
    }

    @Test
    void editingDoesNotChangeTheSavedPosition() throws Exception {
        var existing = binding("Mail").setId(12).setPosition(8);
        mockReference(12);
        when(bindingRepository.findByIdForUpdate(12)).thenReturn(Optional.of(existing));
        service.updateBinding(12, binding("Updated").setPosition(0));
        assertEquals(8, existing.getPosition());
    }

    @Test
    void reordersActiveAndInactiveBindingsAndAuditsAfterSaving() throws Exception {
        var first = binding("First").setId(1).setPosition(5);
        var second = binding("Second").setId(2).setPosition(5).setEnabled(false);
        when(bindingRepository.findAllByIdentityProviderKeyOrderByPositionAscNameAscIdAsc(identityProvider.getKey())).thenReturn(List.of(first, second));
        var result = service.reorderBindings(jwt, identityProvider.getKey(), List.of(2, 1));
        assertEquals(List.of(second, first), result);
        assertEquals(0, second.getPosition());
        assertEquals(1, first.getPosition());
        var order = inOrder(permissionService, identityProviderRepository, bindingRepository, scopedAuditService);
        order.verify(permissionService).requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_UPDATE);
        order.verify(identityProviderRepository).findByKeyForUpdate(identityProvider.getKey());
        order.verify(bindingRepository).findAllByIdentityProviderKeyOrderByPositionAscNameAscIdAsc(identityProvider.getKey());
        order.verify(bindingRepository).saveAllAndFlush(result);
        order.verify(scopedAuditService).create();
        var payload = org.mockito.ArgumentCaptor.forClass(AuditLogPayload.class);
        verify(scopedAuditService).addAuditEntry(payload.capture());
        assertEquals("staff-1", payload.getValue().getActorId());
        assertEquals(identityProvider.getKey().toString(), payload.getValue().getEntityRef());
    }

    @Test
    void rejectsDuplicatesMissingAndForeignBindingsWithoutMutationOrAudit() {
        var first = binding("First").setId(1).setPosition(7);
        var second = binding("Second").setId(2).setPosition(8);
        when(bindingRepository.findAllByIdentityProviderKeyOrderByPositionAscNameAscIdAsc(identityProvider.getKey())).thenReturn(List.of(first, second));
        for (var ids : List.of(List.of(1, 1), List.of(1), List.of(1, 99), List.of(1, 2, 99))) {
            var error = assertThrows(ResponseException.class, () -> service.reorderBindings(jwt, identityProvider.getKey(), ids));
            assertEquals(HttpStatus.CONFLICT, error.getStatus());
        }
        assertEquals(7, first.getPosition());
        assertEquals(8, second.getPosition());
        verify(bindingRepository, never()).saveAllAndFlush(any());
        verifyNoInteractions(scopedAuditService);
    }

    @Test
    void rejectsReorderingWithoutPermissionBeforeReadingBindings() throws Exception {
        doThrow(ResponseException.forbidden()).when(permissionService).requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_UPDATE);
        assertThrows(ResponseException.class, () -> service.reorderBindings(jwt, identityProvider.getKey(), List.of()));
        verifyNoInteractions(bindingRepository, scopedAuditService);
        verify(identityProviderRepository, never()).findByKeyForUpdate(any());
    }

    @Test
    void doesNotAuditFailedReorders() {
        var first = binding("First").setId(1);
        when(bindingRepository.findAllByIdentityProviderKeyOrderByPositionAscNameAscIdAsc(identityProvider.getKey())).thenReturn(List.of(first));
        when(bindingRepository.saveAllAndFlush(any())).thenThrow(new IllegalStateException("Persistence failed"));
        assertThrows(IllegalStateException.class, () -> service.reorderBindings(jwt, identityProvider.getKey(), List.of(1)));
        verifyNoInteractions(scopedAuditService);
    }

    @Test
    void sameCommunicationProviderCanBeAddedToAnIdentityProviderMultipleTimes() throws Exception {
        service.createBinding(binding("Primary"));
        service.createBinding(binding("Secondary"));

        verify(bindingRepository, times(2)).saveAndFlush(any(CommunicationProviderBindingEntity.class));
    }

    @Test
    void disablingTheLastUsableBindingOfAnEnabledIdentityProviderIsAllowed() {
        var existing = binding("Mail").setId(12);
        var update = binding("Mail").setId(12).setEnabled(false);
        mockReference(12);
        when(bindingRepository.findByIdForUpdate(12)).thenReturn(Optional.of(existing));
        assertDoesNotThrow(() -> service.updateBinding(12, update));
        verify(bindingRepository).saveAndFlush(existing);
    }

    @Test
    void testProviderStatusCanBeChangedWhileBindingsExist() {
        var binding = binding("Mail").setId(12);
        var update = provider(7, true, true);
        when(bindingRepository.findAllByCommunicationProviderId(7)).thenReturn(List.of(binding));

        assertDoesNotThrow(() -> service.updateProvider(7, update));

        assertEquals(true, provider.getTestProvider());
        verify(providerRepository).saveAndFlush(provider);
    }

    @Test
    void invalidProviderConfigurationIsReturnedAsBadRequest() throws Exception {
        provider.setEnabled(false);
        doThrow(new CommunicationException("Die Konfiguration ist ungültig."))
                .when(configurationService)
                .mapProviderConfiguration(provider, definition);

        var exception = assertThrows(
                ResponseException.class,
                () -> service.createProvider(provider)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Die Konfiguration ist ungültig.", exception.getTitle());
        verify(providerRepository, never()).save(any());
    }

    @Test
    void inactiveProvidersCannotBeAdded() {
        provider.setEnabled(false);

        var exception = assertThrows(ResponseException.class, () -> service.createBinding(binding("Mail")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Wählen Sie einen aktiven Kommunikationsanbieter aus.", exception.getTitle());
        verify(bindingRepository, never()).saveAndFlush(any());
    }

    @Test
    void existingBindingsOfInactiveProvidersRemainEditable() throws Exception {
        provider.setEnabled(false);
        var existing = binding("Mail").setId(12);
        mockReference(12);
        when(bindingRepository.findByIdForUpdate(12)).thenReturn(Optional.of(existing));

        service.updateBinding(12, binding("Neuer Anzeigename"));

        assertEquals("Neuer Anzeigename", existing.getName());
        verify(bindingRepository).saveAndFlush(existing);
    }

    @Test
    void testAndProductionProvidersCanBeBound() throws Exception {
        provider.setTestProvider(true);

        var created = service.createBinding(binding("Mail"));

        assertEquals(provider.getId(), created.getCommunicationProviderId());
        assertEquals(identityProvider.getKey(), created.getIdentityProviderKey());
        verify(bindingRepository).saveAndFlush(created);
    }

    @Test
    void testingLayoutIsLoadedFromTheConfiguredDefinition() throws Exception {
        var layout = new GroupLayoutElement();
        when(definition.getTestingLayout()).thenReturn(layout);

        var result = service.getProviderTestingLayout(provider.getId());

        assertSame(layout, result);
        verify(definitionService).retrieveProviderDefinition(
                provider.getCommunicationProviderDefinitionKey(),
                provider.getCommunicationProviderDefinitionVersion()
        );
    }

    @Test
    void testMapsTheStoredConfigurationAndInvokesTheDefinition() throws Exception {
        var inputs = new AuthoredElementValues();
        inputs.putLiteral("recipient", "customer@example.test");
        var configuration = new Object();
        var expectedResult = new GroupLayoutElement();
        when(configurationService.mapProviderConfiguration(provider, definition)).thenReturn(configuration);
        when(definition.handleTest(provider, configuration, inputs)).thenReturn(expectedResult);

        var result = service.testProvider(provider.getId(), inputs);

        assertSame(expectedResult, result);
        verify(configurationService).mapProviderConfiguration(provider, definition);
        verify(definition).handleTest(provider, configuration, inputs);
    }

    @Test
    void invalidStoredConfigurationProducesAnErrorLayout() throws Exception {
        var inputs = new AuthoredElementValues();
        doThrow(new CommunicationException("Konfiguration ungültig"))
                .when(configurationService)
                .mapProviderConfiguration(provider, definition);

        var result = service.testProvider(provider.getId(), inputs);

        assertFailedTestResult(result, "Konfiguration ungültig");
        verify(definition, never()).handleTest(any(), any(), any());
    }

    @Test
    void communicationFailureDuringTestProducesAnErrorLayout() throws Exception {
        var inputs = new AuthoredElementValues();
        var configuration = new Object();
        when(configurationService.mapProviderConfiguration(provider, definition)).thenReturn(configuration);
        doThrow(new CommunicationException("Versand fehlgeschlagen"))
                .when(definition)
                .handleTest(provider, configuration, inputs);

        var result = service.testProvider(provider.getId(), inputs);

        assertFailedTestResult(result, "Versand fehlgeschlagen");
    }

    @Test
    void unexpectedFailureDuringTestBecomesInternalServerError() throws Exception {
        var inputs = new AuthoredElementValues();
        var configuration = new Object();
        when(configurationService.mapProviderConfiguration(provider, definition)).thenReturn(configuration);
        doThrow(new IllegalStateException("unexpected"))
                .when(definition)
                .handleTest(provider, configuration, inputs);

        var exception = assertThrows(
                ResponseException.class,
                () -> service.testProvider(provider.getId(), inputs)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("Der Kommunikationsanbieter Mail konnte nicht getestet werden.", exception.getTitle());
    }

    @Test
    void testingUnknownProviderReturnsNotFound() {
        var exception = assertThrows(
                ResponseException.class,
                () -> service.getProviderTestingLayout(999)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void testingProviderWithMissingDefinitionReturnsBadRequest() {
        when(definitionService.retrieveProviderDefinition(
                provider.getCommunicationProviderDefinitionKey(),
                provider.getCommunicationProviderDefinitionVersion()
        )).thenReturn(Optional.empty());

        var exception = assertThrows(
                ResponseException.class,
                () -> service.testProvider(provider.getId(), new AuthoredElementValues())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Die Kommunikationsanbieter-Definition ist nicht verfügbar.", exception.getTitle());
    }

    private static void assertFailedTestResult(GroupLayoutElement result, String expectedMessage) {
        assertEquals("communication-provider-test-result", result.getId());
        var alert = result
                .findChild("communication-provider-test-result-alert", AlertContentElement.class)
                .orElseThrow();
        assertEquals(AlertType.Error, alert.getAlertType());
        assertEquals("Test fehlgeschlagen", alert.getTitle());
        assertEquals(expectedMessage, alert.getText());
    }

    private CommunicationProviderBindingEntity binding(String name) {
        return new CommunicationProviderBindingEntity()
                .setIdentityProviderKey(identityProvider.getKey())
                .setCommunicationProviderId(provider.getId())
                .setName(name)
                .setDescription(name)
                .setEnabled(true)
                .setPosition(0)
                .setConfiguration(new AuthoredElementValues());
    }

    private static CommunicationProviderEntity provider(int id, boolean enabled, boolean testProvider) {
        var entity = new CommunicationProviderEntity();
        entity.setId(id);
        entity.setCommunicationProviderDefinitionKey("de.aivot.core.mail_communication_provider");
        entity.setCommunicationProviderDefinitionVersion(1);
        entity.setName("Mail");
        entity.setDescription("Mail");
        entity.setConfiguration(new AuthoredElementValues());
        entity.setEnabled(enabled);
        entity.setTestProvider(testProvider);
        return entity;
    }

    private static IdentityProviderEntity identityProvider(boolean enabled, boolean testProvider) {
        return new IdentityProviderEntity()
                .setKey(UUID.randomUUID())
                .setType(IdentityProviderType.BundId)
                .setName("BundID")
                .setDescription("BundID")
                .setIsEnabled(enabled)
                .setIsTestProvider(testProvider);
    }
}
