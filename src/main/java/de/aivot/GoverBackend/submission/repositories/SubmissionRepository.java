package de.aivot.GoverBackend.submission.repositories;

import de.aivot.GoverBackend.submission.entities.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SubmissionRepository extends JpaRepository<Submission, String>, JpaSpecificationExecutor<Submission> {
}
