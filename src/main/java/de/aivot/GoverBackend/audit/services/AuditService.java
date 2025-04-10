package de.aivot.GoverBackend.audit.services;

import org.springframework.stereotype.Service;

@Service
public class AuditService {
    public ScopedAuditService createScopedAuditService(Class<?> cls) {
        return new ScopedAuditService(cls);
    }
}
