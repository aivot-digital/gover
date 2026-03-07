export interface AuditLogEntity {
    id: number;
    timestamp: string;
    actorType: string;
    actorId?: string;
    triggerType: string;
    triggerRef?: string;
    triggerRefType?: string;
    module: string;
    diff?: Record<string, unknown>;
    metadata: Record<string, unknown>;
    ipAddress?: string;
}
