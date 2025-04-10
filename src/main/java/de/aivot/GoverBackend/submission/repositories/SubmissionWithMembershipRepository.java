package de.aivot.GoverBackend.submission.repositories;

import de.aivot.GoverBackend.submission.entities.SubmissionWithMembership;
import de.aivot.GoverBackend.submission.entities.SubmissionWithMembershipId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionWithMembershipRepository extends JpaRepository<SubmissionWithMembership, SubmissionWithMembershipId>, JpaSpecificationExecutor<SubmissionWithMembership> {
}
