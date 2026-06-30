package de.aivot.gover.backend.submission.repositories;

import de.aivot.gover.backend.enums.SubmissionStatus;
import de.aivot.gover.backend.submission.entities.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Deprecated
public interface SubmissionRepository extends JpaRepository<Submission, String>, JpaSpecificationExecutor<Submission> {
    Integer countAllByStatusIs(SubmissionStatus status);
}
