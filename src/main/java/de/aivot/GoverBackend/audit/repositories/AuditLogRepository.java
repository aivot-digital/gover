package de.aivot.GoverBackend.audit.repositories;

import de.aivot.GoverBackend.audit.entities.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long>, JpaSpecificationExecutor<AuditLogEntity> {
    @Query("""
            SELECT DISTINCT a.module
            FROM AuditLogEntity a
            WHERE a.module IS NOT NULL AND TRIM(a.module) <> ''
            ORDER BY a.module ASC
            """)
    List<String> findDistinctModules();

    @Query("""
            SELECT DISTINCT a.triggerType
            FROM AuditLogEntity a
            WHERE a.triggerType IS NOT NULL AND TRIM(a.triggerType) <> ''
            ORDER BY a.triggerType ASC
            """)
    List<String> findDistinctTriggerTypes();

    @Query("""
            SELECT DISTINCT a.actorId
            FROM AuditLogEntity a
            WHERE a.actorId IS NOT NULL AND TRIM(a.actorId) <> ''
            ORDER BY a.actorId ASC
            """)
    List<String> findDistinctActorIds();
}
