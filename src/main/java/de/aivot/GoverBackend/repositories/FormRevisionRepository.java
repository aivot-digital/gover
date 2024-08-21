package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.FormRevision;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.Collection;

public interface FormRevisionRepository extends JpaRepository<FormRevision, BigInteger> {
    Collection<FormRevision> getAllByFormIdOrderByTimestampDesc(Integer formId, Limit limit);
}
