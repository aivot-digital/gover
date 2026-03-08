package de.aivot.GoverBackend.audit.services;

import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogService auditLogService;

    @Autowired
    public AuditService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    public ScopedAuditService createScopedAuditService(@Nonnull Class<?> cls, @Nonnull String module) {
        return new ScopedAuditService(cls, module, auditLogService);
    }
}
