package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.FormRevision;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Limit;
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
