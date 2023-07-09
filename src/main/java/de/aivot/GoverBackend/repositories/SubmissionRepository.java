package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.Application;
import de.aivot.GoverBackend.models.entities.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, String> {
    @Transactional(readOnly = true)
    Optional<Submission> findByIdAndApplicationId(String id, Integer applicationId);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdAndArchivedIsNull(Integer applicationId);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdAndIsTestSubmissionFalse(Integer applicationId);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdAndAssigneeIdAndIsTestSubmissionFalse(Integer applicationId, Integer assigneeId);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdAndAssigneeId(Integer applicationId, Integer assignee);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdAndAssigneeIdAndArchivedIsNull(Integer applicationId, Integer assignee);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdAndAssigneeIdAndArchivedIsNullAndIsTestSubmissionFalse(Integer applicationId, Integer assignee);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationId(Integer applicationId);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdAndArchivedIsNullAndIsTestSubmissionFalse(Integer applicationId);

    @Transactional(readOnly = true)
    boolean existsByApplication_IdInAndArchivedIsNull(Collection<Integer> applications);

    @Transactional(readOnly = true)
    boolean existsByApplication_IdAndArchivedIsNull(Integer application);
}
