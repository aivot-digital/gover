package de.aivot.GoverBackend.submission.repositories;

import de.aivot.GoverBackend.submission.entities.SubmissionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;

public interface SubmissionAttachmentRepository extends JpaRepository<SubmissionAttachment, String>, JpaSpecificationExecutor<SubmissionAttachment> {

    Collection<SubmissionAttachment> findAllBySubmissionId(String submissionId);

}
