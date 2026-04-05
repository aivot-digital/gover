package de.aivot.GoverBackend.audit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ScopedAuditServiceTest {
    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void addAuditEntryShouldDefaultToSystemWhenActorIsMissing() throws Exception {
        var request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        var auditLogService = mock(AuditLogService.class);
        var scopedAuditService = new ScopedAuditService(ScopedAuditServiceTest.class, "Tests", auditLogService);

        scopedAuditService.create()
                .setMessage("Test")
                .log();

        var captor = ArgumentCaptor.forClass(de.aivot.GoverBackend.audit.entities.AuditLogEntity.class);
        verify(auditLogService).create(captor.capture());
        assertEquals("System", captor.getValue().getActorType());
        assertNull(captor.getValue().getActorId());
    }

    @Test
    void addAuditEntryShouldDefaultToUserWhenActorIdIsPresent() throws Exception {
        var auditLogService = mock(AuditLogService.class);
        var scopedAuditService = new ScopedAuditService(ScopedAuditServiceTest.class, "Tests", auditLogService);

        scopedAuditService.create()
                .setActorId(" user-1 ")
                .setMessage("Test")
                .log();

        var captor = ArgumentCaptor.forClass(de.aivot.GoverBackend.audit.entities.AuditLogEntity.class);
        verify(auditLogService).create(captor.capture());
        assertEquals("User", captor.getValue().getActorType());
        assertEquals("user-1", captor.getValue().getActorId());
    }
}
