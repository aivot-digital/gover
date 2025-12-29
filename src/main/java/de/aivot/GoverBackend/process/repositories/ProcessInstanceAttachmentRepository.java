package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ProcessInstanceAttachmentRepository extends JpaRepository<ProcessInstanceAttachmentEntity, UUID>, JpaSpecificationExecutor<ProcessInstanceAttachmentEntity> {
}