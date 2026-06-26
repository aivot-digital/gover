package de.aivot.gover.backend.form.repositories;

import de.aivot.gover.backend.form.entities.FormRevisionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface FormRevisionRepository extends JpaRepository<FormRevisionEntity, BigInteger> {
    Optional<FormRevisionEntity> getFirstByFormIdAndFormVersionOrderByTimestampAsc(Integer formId, Integer formVersion);

    Collection<FormRevisionEntity> getAllByFormIdAndFormVersionAndTimestampIsAfterOrderByTimestampDesc(Integer id, Integer FormVersion, Instant timestamp);

    Page<FormRevisionEntity> getAllByFormIdAndFormVersionOrderByTimestampDesc(Integer formId, Integer FormVersion, Pageable pageable);
}
