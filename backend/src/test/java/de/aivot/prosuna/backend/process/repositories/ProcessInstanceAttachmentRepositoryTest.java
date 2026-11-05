package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceAttachmentFilter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:process-instance-attachments;NON_KEYWORDS=KEY",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"})
@ContextConfiguration(classes = ProcessInstanceAttachmentRepositoryTest.Config.class)
class ProcessInstanceAttachmentRepositoryTest {
    private static final UUID FIRST = key(1);
    private static final UUID SECOND = key(2);
    private static final UUID REMOVED = key(3);
    private static final UUID OTHER_TASK = key(4);
    private static final UUID DUE = key(5);

    @Autowired
    private EntityManager em;
    private ProcessInstanceAttachmentRepository attachments;

    @BeforeEach
    void setup() {
        sql("drop table if exists process_instance_attachments");
        sql("""
                create table process_instance_attachments (
                    key uuid primary key, file_name varchar(255), original_file_name varchar(255), "group" text,
                    position int, attachment_set_id int, process_instance_id bigint, process_instance_task_id bigint,
                    storage_provider_id int, storage_path_from_root text, uploaded_by_user_id varchar(36),
                    removed_at timestamp with time zone, deletion_due_at timestamp with time zone
                )
                """);
        // Set 3 of task 9 holds two referenced uploads and one removed upload; set 4 belongs to task 8, whose removed
        // upload is due for deletion.
        sql("""
                insert into process_instance_attachments values
                    ('%s', 'b.pdf', 'b.pdf', null, 2, 3, 42, 9, 5, '/b.pdf', null, null, null),
                    ('%s', 'a.pdf', 'a.pdf', null, 1, 3, 42, 9, 5, '/a.pdf', null, null, null),
                    ('%s', 'c.pdf', 'c.pdf', null, 3, 3, 42, 9, 5, '/c.pdf', null, '2026-10-28 08:00:00+00', null),
                    ('%s', 'd.pdf', 'd.pdf', null, 1, 4, 42, 8, 5, '/d.pdf', null, null, null),
                    ('%s', 'e.pdf', 'e.pdf', null, 2, 4, 42, 8, 5, '/e.pdf', null, '2026-10-28 08:00:00+00', '2026-10-28 09:00:00+00')
                """.formatted(SECOND, FIRST, REMOVED, OTHER_TASK, DUE));
        attachments = new JpaRepositoryFactory(em).getRepository(ProcessInstanceAttachmentRepository.class);
    }

    @Test
    void attachmentSetReadsLeaveOutRemovedUploads() {
        assertEquals(List.of(FIRST, SECOND), keys(attachments.findAllByAttachmentSetIdAndRemovedAtIsNullOrderByPositionAscKeyAsc(3)));
    }

    @Test
    void processInstanceReadsLeaveOutRemovedUploadsUnlessAllRowsAreNeeded() {
        assertEquals(3, attachments.findAllByProcessInstanceIdAndRemovedAtIsNull(42L).size());
        assertEquals(5, attachments.findAllByProcessInstanceId(42L).size());
    }

    @Test
    void filterLeavesOutRemovedUploads() {
        var listed = attachments.findAll(ProcessInstanceAttachmentFilter.create().setProcessInstanceId(42L).build());

        assertFalse(keys(listed).contains(REMOVED));
        assertFalse(keys(listed).contains(DUE));
        assertEquals(3, listed.size());
    }

    @Test
    void taskReadsFindTheRemovedAndAllUploadsOfTheTask() {
        assertEquals(List.of(REMOVED), keys(attachments.findAllByProcessInstanceTaskIdAndRemovedAtIsNotNullAndDeletionDueAtIsNull(9L)));
        assertEquals(List.of(), keys(attachments.findAllByProcessInstanceTaskIdAndRemovedAtIsNotNullAndDeletionDueAtIsNull(8L)));
        assertEquals(3, attachments.findAllByProcessInstanceTaskIdAndAttachmentSetIdIn(9L, List.of(3, 4)).size());
        assertTrue(attachments.existsByAttachmentSetId(4));
        assertFalse(attachments.existsByAttachmentSetId(5));
    }

    @Test
    void deletionReadsFindOnlyAttachmentsDueForDeletion() {
        assertEquals(List.of(DUE), keys(attachments.findTop100ByDeletionDueAtLessThanOrderByDeletionDueAtAsc(Instant.parse("2026-10-28T10:00:00Z"))));
        assertEquals(List.of(), keys(attachments.findTop100ByDeletionDueAtLessThanOrderByDeletionDueAtAsc(Instant.parse("2026-10-28T09:00:00Z"))));
        assertTrue(attachments.findByKeyAndDeletionDueAtIsNotNull(DUE).isPresent());
        assertTrue(attachments.findByKeyAndDeletionDueAtIsNotNull(REMOVED).isEmpty());
    }

    private static List<UUID> keys(List<ProcessInstanceAttachmentEntity> entities) {
        return entities.stream().map(ProcessInstanceAttachmentEntity::getKey).toList();
    }

    private static UUID key(int index) {
        return UUID.fromString("00000000-0000-0000-0000-00000000000" + index);
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
    }
}
