package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.dtos.ProcessListFilter;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:process-lists",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"})
@ContextConfiguration(classes = ProcessListServiceTest.Config.class)
public class ProcessListServiceTest {
    @Autowired
    EntityManager em;
    private PermissionService permissions;
    private ProcessListService service;

    @BeforeEach
    void setup() {
        permissions = mock(PermissionService.class);
        var users = mock(UserRepository.class);
        var definitions = mock(ProcessNodeDefinitionService.class);
        when(definitions.getProcessNodeDefinition(anyString(), anyInt())).thenReturn(Optional.empty());
        var person = mock(UserEntity.class);
        when(person.getId()).thenReturn("someone");
        when(person.getFullName()).thenReturn("Andere Person");
        when(users.findAllById(any())).thenReturn(List.of(person));
        service = new ProcessListService(em, permissions, users, definitions);
        when(permissions.getProcessInstancesWithPermission("me", PROCESS_INSTANCE_READ)).thenReturn(List.of(1L, 2L));
        sql("create alias if not exists compact_case_number_search_key for 'de.aivot.prosuna.backend.process.services.ProcessListServiceTest.compactKey'");
        sql("drop table if exists process_instance_tasks");
        sql("drop table if exists process_instances");
        sql("drop table if exists process_nodes");
        sql("drop table if exists processes");
        sql("create table processes (id int primary key, internal_title varchar)");
        sql("create table process_nodes (id int primary key, name varchar, description varchar, process_node_definition_key varchar, process_node_definition_version int)");
        sql("create table process_instances (id bigint primary key, case_number varchar, assigned_file_numbers varchar array, process_id int, initial_process_version int, assigned_user_id varchar, status smallint, status_override varchar, started timestamp with time zone, finished timestamp with time zone, created_for_test_claim_id int)");
        sql("create table process_instance_tasks (id bigint primary key, process_instance_id bigint, process_node_id int, process_version int, assigned_user_id varchar, status smallint, status_override varchar, started timestamp with time zone, finished timestamp with time zone, deadline timestamp with time zone)");
        sql("insert into processes values (10, 'Anmeldung'), (20, 'Geschützt')");
        sql("insert into process_nodes values (100, 'Prüfung', 'Unterlagen prüfen', 'missing', 1)");
        sql("insert into process_instances values (1, 'ABC-100', ARRAY['AZ-2026', '100%_literal'], 10, 1, null, 1, 'Eigener Status', CURRENT_TIMESTAMP, null, null), (2, 'ABC-200', ARRAY['AZ-Alt'], 10, 2, 'someone', 3, null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1), (3, 'SECRET', ARRAY['AZ-secret'], 20, 1, 'secret-user', 5, null, CURRENT_TIMESTAMP, null, null)");
        sql("insert into process_instance_tasks values (11,1,100,1,'me',0,'Fachliche Prüfung',CURRENT_TIMESTAMP,null,DATEADD('DAY',-1,CURRENT_TIMESTAMP)), (12,1,100,1,'someone',7,null,CURRENT_TIMESTAMP,null,null), (13,2,100,2,null,2,null,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,DATEADD('DAY',-1,CURRENT_TIMESTAMP)), (14,3,100,1,'me',0,null,CURRENT_TIMESTAMP,null,DATEADD('DAY',-2,CURRENT_TIMESTAMP)), (15,1,100,1,'me',6,null,CURRENT_TIMESTAMP,null,null), (16,1,100,1,'me',1,null,CURRENT_TIMESTAMP,null,null)");
    }

    @Test
    void scopesInstancesBeforePaginationAndAllowsSystemOverride() throws Exception {
        var first = service.instances("me", PageRequest.of(0, 1, Sort.by("id")), filter("all", "all", null));
        assertEquals(2, first.getTotalElements());
        assertEquals(1L, first.getContent().getFirst().id());
        assertNull(first.getContent().getFirst().assignedUserName());
        var second = service.instances("me", PageRequest.of(1, 1, Sort.by("id")), filter("all", "all", null));
        assertEquals("Andere Person", second.getContent().getFirst().assignedUserName());
        when(permissions.getProcessInstancesWithPermission("me", PROCESS_INSTANCE_READ)).thenReturn(List.of());
        assertEquals(0, service.instances("me", PageRequest.of(0, 12), filter("all", "all", null)).getTotalElements());
        when(permissions.hasSystemPermission("me", PROCESS_INSTANCE_READ)).thenReturn(true);
        assertEquals(3, service.instances("me", PageRequest.of(0, 12), filter("all", "all", null)).getTotalElements());
    }

    @Test
    void openAndOverdueUseSystemStatusesAndAssignmentWithinReadableScope() throws Exception {
        var page = PageRequest.of(0, 12, Sort.by("deadline"));
        var mine = service.tasks("me", page, filter("open", "mine", null));
        assertEquals(List.of(11L, 15L, 16L), mine.getContent().stream().map(row -> row.id()).toList());
        assertEquals("Fachliche Prüfung", mine.getContent().getFirst().statusOverride());
        assertEquals(3, service.countOpenAssignedTasks("me"));
        assertEquals(4, service.tasks("me", page, filter("open", "all", null)).getTotalElements());
        assertEquals(1, service.tasks("me", page, filter("overdue", "all", null)).getTotalElements());
        assertEquals(ProcessTaskStatus.Completed, service.tasks("me", page, filter("all", "unassigned", null)).getContent().getFirst().status());
        assertEquals(1, service.tasks("me", page, filter("all", "someone", null)).getTotalElements());
    }

    @Test
    void taskCountsOptionsAndRowsRespectAbsentGrantsAndSystemOverride() throws Exception {
        when(permissions.getProcessInstancesWithPermission("me", PROCESS_INSTANCE_READ)).thenReturn(List.of());
        assertEquals(0, service.tasks("me", PageRequest.of(0, 12), filter("all", "all", null)).getTotalElements());
        assertEquals(0, service.countOpenAssignedTasks("me"));
        assertTrue(service.options("me", true, null).processes().isEmpty());
        when(permissions.hasSystemPermission("me", PROCESS_INSTANCE_READ)).thenReturn(true);
        assertEquals(6, service.tasks("me", PageRequest.of(0, 12), filter("all", "all", null)).getTotalElements());
        assertEquals(4, service.countOpenAssignedTasks("me"));
        assertEquals(2, service.options("me", true, null).processes().size());
    }

    @Test
    void filtersProcessesAndVersionsAndChecksExplicitInstanceScope() throws Exception {
        var filter = new ProcessListFilter(null, "all", 10, 2, null, "all");
        assertEquals(1, service.instances("me", PageRequest.of(0, 12), filter).getTotalElements());
        assertEquals(1, service.tasks("me", PageRequest.of(0, 12), filter).getTotalElements());
        var forbidden = ResponseException.forbidden("Kein Zugriff");
        doThrow(forbidden).when(permissions).requireProcessInstancePermission("me", 3L, PROCESS_INSTANCE_READ);
        assertThrows(ResponseException.class, () -> service.tasks("me", PageRequest.of(0, 12), new ProcessListFilter(null, "all", null, null, 3L, "all")));
        assertThrows(ResponseException.class, () -> service.options("me", true, 3L));
    }

    @Test
    void optionsIncludeHistoricalAssignmentsButNeverUnreadableProcesses() throws Exception {
        var options = service.options("me", false, null);
        assertEquals(List.of("Anmeldung"), options.processes().stream().map(value -> value.label()).toList());
        assertEquals(List.of("someone"), options.assignees().stream().map(value -> value.value()).toList());
        assertEquals(1, service.instances("me", PageRequest.of(0, 12), filter("ended", "all", null)).getTotalElements());
        assertEquals(1, service.instances("me", PageRequest.of(0, 12), filter("active", "all", null)).getTotalElements());
        assertEquals(0, service.instances("me", PageRequest.of(0, 12), filter("failed", "all", null)).getTotalElements());
    }

    @Test
    void searchesCaseNumbersAndAllFileNumbersLiterallyIgnoringCase() throws Exception {
        sql("create alias if not exists array_to_string for 'de.aivot.prosuna.backend.process.services.ProcessListServiceTest.joinArray'");
        assertEquals(1, service.instances("me", PageRequest.of(0, 12), filter("all", "all", "abc-100")).getTotalElements());
        assertEquals(1, service.instances("me", PageRequest.of(0, 12), filter("all", "all", "az-alt")).getTotalElements());
        assertEquals(1, service.instances("me", PageRequest.of(0, 12), filter("all", "all", "%_")).getTotalElements());
        assertEquals(0, service.instances("me", PageRequest.of(0, 12), filter("all", "all", "secret")).getTotalElements());
        assertEquals(4, service.tasks("me", PageRequest.of(0, 12), filter("all", "all", "AZ-2026")).getTotalElements());
    }

    @Test
    void searchesCanonicalNumbersWithoutChangingLiteralFileNumbersOrAccess() throws Exception {
        sql("create alias if not exists array_to_string for 'de.aivot.prosuna.backend.process.services.ProcessListServiceTest.joinArray'");
        sql("update process_instances set case_number = '7K0M-9X1Q-0042' where id = 1");
        sql("update process_instances set case_number = '7KOM-9X1Q-0042' where id = 2");
        sql("update process_instances set case_number = '7K0M-9X1Q-0043' where id = 3");
        var page = PageRequest.of(0, 12);
        assertEquals(1, service.instances("me", page, filter("all", "all", "7kom 9xlq 0042")).getTotalElements());
        assertEquals(2, service.instances("me", page, filter("all", "all", "7KOM-9X1Q-0042")).getTotalElements());
        assertEquals(1, service.instances("me", page, filter("all", "all", "9xlq")).getTotalElements());
        assertEquals(4, service.tasks("me", page, filter("all", "all", "7kom9xiq0042")).getTotalElements());
        assertEquals(0, service.instances("me", page, filter("all", "all", "7kom9xiq0043")).getTotalElements());
        assertEquals(0, service.instances("me", page, filter("all", "all", "AZ-A1t")).getTotalElements());
        assertEquals(0, service.instances("me", page, filter("all", "all", "7kom9xiq0044")).getTotalElements());
    }

    public static String compactKey(String value) {
        return value != null && value.matches("[0-9A-HJKMNP-TV-Z]{4}-[0-9A-HJKMNP-TV-Z]{4}-[0-9A-HJKMNP-TV-Z]{4}") ? value.replace("-", "") : null;
    }

    public static String joinArray(String[] values, String delimiter) {
        return String.join(delimiter, values);
    }

    private ProcessListFilter filter(String view, String assignee, String search) {
        return new ProcessListFilter(search, view, null, null, null, assignee);
    }

    private void sql(String sql) {
        em.createNativeQuery(sql).executeUpdate();
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan("de.aivot.prosuna.backend")
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories("de.aivot.prosuna.backend.process.repositories")
    static class Config {
        @Bean
        JsonMapper jsonMapper() {
            return JsonMapperTestUtils.createMapper();
        }
    }
}
