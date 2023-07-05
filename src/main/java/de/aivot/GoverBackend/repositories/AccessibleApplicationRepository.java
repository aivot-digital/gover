package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.AccessibleApplication;

import java.util.Collection;

public interface AccessibleApplicationRepository extends ReadOnlyRepository<AccessibleApplication, AccessibleApplication.AccessibleApplicationKey> {
    Collection<AccessibleApplication> findAllByKey_UserId(Integer userId);
    boolean existsByKey_ApplicationIdAndKey_UserId(Integer applicationId, Integer userId);
}
