package de.aivot.GoverBackend.submission.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.submission.entities.SubmissionWithMembership;
import de.aivot.GoverBackend.submission.entities.SubmissionWithMembershipId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SubmissionWithMembershipRepository extends ReadOnlyRepository<SubmissionWithMembership, SubmissionWithMembershipId>, JpaSpecificationExecutor<SubmissionWithMembership> {
}
