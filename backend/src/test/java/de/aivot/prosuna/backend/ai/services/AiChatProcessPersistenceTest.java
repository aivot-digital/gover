package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import de.aivot.prosuna.backend.ai.models.AiProcessChatContext;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import de.aivot.prosuna.backend.audit.services.AuditLogService;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.*;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:ai-process;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS CHARACTER VARYING",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"
})
@ContextConfiguration(classes = AiChatProcessPersistenceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AiChatProcessPersistenceTest {
    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(basePackageClasses = ProcessNodeRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {ProcessNodeRepository.class, ProcessEdgeRepository.class, ProcessVersionRepository.class}))
    @Import({AiChatProcessService.class, AiProcessFormService.class, ProcessEdgeService.class, AuditService.class})
    static class Config {
        @Bean PersistenceManagedTypes managedTypes() {
            return PersistenceManagedTypes.of(ProcessNodeEntity.class.getName(), ProcessEdgeEntity.class.getName(), ProcessVersionEntity.class.getName());
        }
        @Bean JsonMapper mapper() { return JsonMapperTestUtils.createMapper(); }
        @Bean Validator validator() { return Validation.buildDefaultValidatorFactory().getValidator(); }
    }

    @Autowired AiChatProcessService service;
    @Autowired ProcessNodeRepository nodes;
    @Autowired ProcessEdgeRepository edges;
    @Autowired ProcessVersionRepository versions;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean PermissionService permissions;
    @MockitoBean AiChatSessionRepository sessions;
    @MockitoBean UserService users;
    @MockitoBean ProcessNodeService nodeService;
    @MockitoBean ProcessVersionService versionService;
    @MockitoBean ProcessNodeDefinitionService definitions;
    @MockitoBean AiProcessConfigurationService configuration;
    @MockitoBean AiProcessOptionsService options;
    @MockitoBean AuditLogService auditLogs;
    private final AiProcessChatContext scope = new AiProcessChatContext("owner", "session", 7, 2);
    private Integer id;

    @BeforeEach
    void setUp() throws Exception {
        when(sessions.findByUserIdAndSessionId("owner", "session")).thenReturn(Optional.of(new AiChatSessionEntity()));
        when(users.retrieve("owner")).thenReturn(Optional.of(new UserEntity().setId("owner")));
        var definition = mock(ProcessNodeDefinition.class);
        when(definitions.getProcessNodeDefinition(any(ProcessNodeEntity.class))).thenReturn(Optional.of(definition));
        when(configuration.patch(any(), any(), any(), any())).thenAnswer(i ->
                new AiProcessConfigurationService.PatchResult(
                        ((ProcessNodeEntity) i.getArgument(0)).getConfiguration(), List.of()));
        when(nodeService.validate(any(), any(), anyBoolean(), any())).thenReturn(Optional.empty());
        when(nodeService.updateForAuthoring(anyInt(), any(), any(), any())).thenAnswer(i -> {
            ProcessNodeEntity candidate = i.getArgument(1);
            ProcessNodeEntity existing = i.getArgument(2);
            existing.setName(candidate.getName()).setConfiguration(candidate.getConfiguration());
            return nodes.save(existing);
        });
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            edges.deleteAll(); nodes.deleteAll(); versions.deleteAll();
            versions.save(new ProcessVersionEntity().setProcessId(7).setProcessVersion(2).setStatus(ProcessVersionStatus.Drafted).setPublicTitle("Prozess"));
            id = nodes.saveAndFlush(new ProcessNodeEntity().setProcessId(7).setProcessVersion(2).setDataKey("counter")
                    .setProcessNodeDefinitionKey("counter").setProcessNodeDefinitionVersion(1).setName("Before")
                    .setConfiguration(new AuthoredElementValues()).setOutputMappings(Map.of())).getId();
        });
    }

    @Test
    void commitsImmediatelyAndReloadsCurrentStateOnEachCall() throws Exception {
        service.updateNode(scope, id, Map.of("name", "Saved"), List.of(), List.of());
        assertThat(nodes.findById(id).orElseThrow().getName()).isEqualTo("Saved");
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var node = nodes.findById(id).orElseThrow();
            node.getConfiguration().putLiteral("external", "Concurrent editor");
            nodes.saveAndFlush(node);
        });
        service.updateNode(scope, id, Map.of("name", "Changed again"), List.of(), List.of());
        var loaded = nodes.findById(id).orElseThrow();
        assertThat(loaded.getName()).isEqualTo("Changed again");
        assertThat(loaded.getConfiguration().getLiteral("external")).isEqualTo("Concurrent editor");
        verify(auditLogs, times(2)).create(any());
    }

    @Test
    void rollsBackCheckedFailuresAfterFlushWithoutUndoingPreviousCall() throws Exception {
        service.updateNode(scope, id, Map.of("name", "Successful earlier call"), List.of(), List.of());
        clearInvocations(auditLogs);
        doAnswer(i -> {
            ProcessNodeEntity existing = i.getArgument(2);
            existing.setName("Must roll back");
            nodes.saveAndFlush(existing);
            throw ResponseException.badRequest("Fehler nach dem Schreiben");
        }).when(nodeService).updateForAuthoring(anyInt(), any(), any(), any());
        assertThatThrownBy(() -> service.updateNode(scope, id, Map.of("name", "Failed call"), List.of(), List.of()))
                .isInstanceOf(ResponseException.class);
        assertThat(nodes.findById(id).orElseThrow().getName()).isEqualTo("Successful earlier call");
        verifyNoInteractions(auditLogs);
    }
}
