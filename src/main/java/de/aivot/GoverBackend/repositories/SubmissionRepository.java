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
    Collection<Submission> findAllByApplicationIdAndArchivedIsNullOrderByCreatedDesc(Integer applicationId);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdAndIsTestSubmissionFalseOrderByCreatedDesc(Integer applicationId);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdAndAssigneeIdAndIsTestSubmissionFalseOrderByCreatedDesc(Integer applicationId, Integer assigneeId);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdAndAssigneeIdOrderByCreatedDesc(Integer applicationId, Integer assignee);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdAndAssigneeIdAndArchivedIsNullOrderByCreatedDesc(Integer applicationId, Integer assignee);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdAndAssigneeIdAndArchivedIsNullAndIsTestSubmissionFalseOrderByCreatedDesc(Integer applicationId, Integer assignee);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdOrderByCreatedDesc(Integer applicationId);

    @Transactional(readOnly = true)
    Collection<Submission> findAllByApplicationIdAndArchivedIsNullAndIsTestSubmissionFalseOrderByCreatedDesc(Integer applicationId);

    @Transactional(readOnly = true)
    boolean existsByApplication_IdInAndArchivedIsNull(Collection<Integer> applications);

    @Transactional(readOnly = true)
    boolean existsByApplication_IdAndArchivedIsNull(Integer application);
}
