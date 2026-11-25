package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.prosuna.backend.user.services.UserDeputyService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class ProcessNodeExecutionLoggerFactoryTest {
    private final ProcessInstanceHistoryEventRepository events = mock(ProcessInstanceHistoryEventRepository.class);
    private final UserDeputyService deputies = mock(UserDeputyService.class);
    private final ProcessNodeExecutionLoggerFactory factory = new ProcessNodeExecutionLoggerFactory(events, deputies);
    private final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity().setId(9L).setAssignedUserId("philipp");

    @Test
    void eventsOfAnActiveDeputyRecordTheRepresentedAssignee() {
        when(deputies.isActiveDeputyFor("daniel", "philipp")).thenReturn(true);

        factory.createForStaffTask(42L, task, "daniel").withTaskId(9L)
                .logf(ProcessNodeExecutionLogLevel.Info, false, true, "Gespeichert", Map.of("field", "value"), "Gespeichert");

        var event = savedEvent();
        assertEquals("daniel", event.getTriggeringUserId());
        assertEquals(Map.of("field", "value", "representedUserId", "philipp"), event.getDetails());
    }

    @Test
    void eventsOfOtherStaffDoNotRecordARepresentedUser() {
        factory.createForStaffTask(42L, task, "other")
                .logf(ProcessNodeExecutionLogLevel.Info, false, true, "Gespeichert", "Gespeichert");

        assertFalse(savedEvent().getDetails().containsKey("representedUserId"));
    }

    @Test
    void eventsOfTheAssigneeSkipTheDeputyLookup() {
        factory.createForStaffTask(42L, task, "philipp")
                .logf(ProcessNodeExecutionLogLevel.Info, false, true, "Gespeichert", "Gespeichert");

        assertFalse(savedEvent().getDetails().containsKey("representedUserId"));
        verifyNoInteractions(deputies);
    }

    private ProcessInstanceEventEntity savedEvent() {
        var captor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(events).save(captor.capture());
        return captor.getValue();
    }
}
