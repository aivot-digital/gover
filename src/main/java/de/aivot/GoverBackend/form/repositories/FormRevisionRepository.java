package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.form.entities.FormRevision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface FormRevisionRepository extends JpaRepository<FormRevision, BigInteger> {
    Optional<FormRevision> getFirstByFormIdOrderByTimestampAsc(Integer formId);

    Page<FormRevision> getAllByFormIdOrderByTimestampDesc(Integer formId, Pageable pageable);

    Collection<FormRevision> getAllByFormIdAndTimestampIsAfterOrderByTimestampDesc(Integer id, LocalDateTime timestamp);
}
