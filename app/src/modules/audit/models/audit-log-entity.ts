export interface AuditLogEntity {
    id: number;
    timestamp: string;
    actorType: string;
    actorId?: string | null;
    triggerType: string | null;
    entityType?: string | null;
    entityRef?: string | null;
    entityRefType?: string | null;
    module: string;
    message?: string | null;
    diff?: Record<string, unknown> | null;
    metadata: Record<string, unknown>;
    ipAddress?: string;
}
