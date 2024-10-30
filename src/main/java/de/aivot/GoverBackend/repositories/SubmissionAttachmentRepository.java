package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.SubmissionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;

public interface SubmissionAttachmentRepository extends JpaRepository<SubmissionAttachment, String> {
    
    Collection<SubmissionAttachment> findAllBySubmissionId(String submissionId);

    
    Optional<SubmissionAttachment> findByIdAndSubmissionId(String id, String submissionId);
}
