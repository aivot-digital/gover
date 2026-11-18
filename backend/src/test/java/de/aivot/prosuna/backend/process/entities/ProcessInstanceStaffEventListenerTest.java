package de.aivot.prosuna.backend.process.entities;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.staffEvents.models.StaffEvent;
import de.aivot.prosuna.backend.staffEvents.services.StaffEventService;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:h2:mem:process-instance-staff-events",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"})
@ContextConfiguration(classes = ProcessInstanceStaffEventListenerTest.Config.class)
// Each write commits or rolls back its own transaction, as in production.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProcessInstanceStaffEventListenerTest {
    private static final String INSTANCE_7_CHANGED = "{\"type\":\"process-instance\",\"scope\":\"7\",\"userId\":null}";

    @Autowired
    private EntityManager em;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            sql("drop table if exists process_instance_attachment_sets");
            sql("drop sequence if exists process_instance_attachment_sets_id_seq");
            sql("create sequence process_instance_attachment_sets_id_seq");
            sql("""
                    create table process_instance_attachment_sets (
                        id integer primary key, name varchar(255), data_key varchar(255),
                        process_instance_id bigint, process_instance_task_id bigint
                    )
                    """);
        });
        clearInvocations(rabbitTemplate);
    }

    @Test
    void publishesTheChangedInstanceAfterTheCommit() {
        var created = transaction.execute(status -> {
            var attachmentSet = attachmentSet();
            em.persist(attachmentSet);
            verifyNoInteractions(rabbitTemplate);
            return attachmentSet;
        });
        verify(rabbitTemplate).convertAndSend(eq("prosuna.staff-events"), eq(""), eq(INSTANCE_7_CHANGED));

        // A detached entity, as services save it without open-in-view. Its update is flushed during the commit.
        transaction.executeWithoutResult(status -> em.merge(created.setName("Belege")));
        verify(rabbitTemplate, times(2)).convertAndSend(eq("prosuna.staff-events"), eq(""), eq(INSTANCE_7_CHANGED));
    }

    @Test
    void publishesEachChangedInstanceOncePerTransaction() {
        transaction.executeWithoutResult(status -> {
            var attachmentSet = attachmentSet();
            em.persist(attachmentSet);
            em.persist(attachmentSet());
            em.flush();
            attachmentSet.setName("Belege");
        });

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), Mockito.<Object>any());
    }

    @Test
    void publishesTheInstanceOfARemovedRow() {
        var created = transaction.execute(status -> {
            var attachmentSet = attachmentSet();
            em.persist(attachmentSet);
            return attachmentSet;
        });
        clearInvocations(rabbitTemplate);

        transaction.executeWithoutResult(status -> em.remove(em.find(ProcessInstanceAttachmentSetEntity.class, created.getId())));

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), Mockito.<Object>any());
        verify(rabbitTemplate).convertAndSend(eq("prosuna.staff-events"), eq(""), eq(INSTANCE_7_CHANGED));
    }

    @Test
    void letsTheListsOfAllUsersReloadWhenAnInstanceIsDeleted() {
        var events = mock(ApplicationEventPublisher.class);
        var listener = new ProcessInstanceStaffEventListener(events);

        listener.removed(new ProcessInstanceEntity().setId(7L));

        verify(events).publishEvent(new StaffEvent("process-instance", "7", null));
        verify(events).publishEvent(new StaffEvent("process-instance", null, null));
        verifyNoMoreInteractions(events);
    }

    @Test
    void publishesNothingWhenTheTransactionRollsBack() {
        transaction.executeWithoutResult(status -> {
            em.persist(attachmentSet());
            em.flush();
            status.setRollbackOnly();
        });

        verifyNoInteractions(rabbitTemplate);
    }

    private static ProcessInstanceAttachmentSetEntity attachmentSet() {
        return new ProcessInstanceAttachmentSetEntity().setName("Nachweise").setDataKey("proofs").setProcessInstanceId(7L);
    }

    private void sql(String statement) {
        em.createNativeQuery(statement).executeUpdate();
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan("de.aivot.prosuna.backend")
    static class Config {
        @Bean
        JsonMapper jsonMapper() {
            return JsonMapperTestUtils.createMapper();
        }

        @Bean
        RabbitTemplate rabbitTemplate() {
            return mock(RabbitTemplate.class);
        }

        @Bean
        StaffEventService staffEventService(RabbitTemplate rabbitTemplate, JsonMapper jsonMapper) {
            return new StaffEventService(rabbitTemplate, jsonMapper, List.of(), mock(UserRepository.class));
        }
    }
}
