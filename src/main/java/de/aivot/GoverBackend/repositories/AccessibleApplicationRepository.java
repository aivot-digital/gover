package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.AccessibleApplication;

import java.util.Collection;

public interface AccessibleApplicationRepository extends ReadOnlyRepository<AccessibleApplication, AccessibleApplication.AccessibleApplicationKey> {
    Collection<AccessibleApplication> findAllByKey_UserId(Integer user);
    Collection<AccessibleApplication> findAllByKey_MembershipId(Integer membership);
    boolean existsByKey_ApplicationIdAndKey_UserId(Integer application, Integer user);
    Collection<AccessibleApplication> findAllByKey_UserIdAndKey_DepartmentId(Integer user, Integer department);
}
