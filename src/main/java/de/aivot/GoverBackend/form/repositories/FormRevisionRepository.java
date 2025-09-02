package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.form.entities.FormRevisionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface FormRevisionRepository extends JpaRepository<FormRevisionEntity, BigInteger> {
    Optional<FormRevisionEntity> getFirstByFormIdOrderByTimestampAsc(Integer formId);

    Page<FormRevisionEntity> getAllByFormIdOrderByTimestampDesc(Integer formId, Pageable pageable);

    Collection<FormRevisionEntity> getAllByFormIdAndTimestampIsAfterOrderByTimestampDesc(Integer id, LocalDateTime timestamp);
}
