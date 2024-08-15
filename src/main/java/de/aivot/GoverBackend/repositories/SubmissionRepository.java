package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.enums.XBezahldienstStatus;
import de.aivot.GoverBackend.models.entities.Submission;
import de.aivot.GoverBackend.models.entities.SubmissionListProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, String>, JpaSpecificationExecutor<SubmissionListProjection> {
    boolean existsByFormIdAndArchivedIsNullAndIsTestSubmissionIsFalse(Integer formId);

    Optional<Submission> findByIdAndFormIdIn(String id, Collection<Integer> formId);

    Collection<Submission> findAllByArchivedIsNotNull();

    Collection<Submission> findAllByPaymentRequestIsNotNullAndPaymentErrorIsNullAndStatus(SubmissionStatus status);
}
