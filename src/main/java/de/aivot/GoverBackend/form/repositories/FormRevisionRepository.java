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
    Optional<FormRevisionEntity> getFirstByFormIdAndFormVersionOrderByTimestampAsc(Integer formId, Integer formVersion);

    Collection<FormRevisionEntity> getAllByFormIdAndFormVersionAndTimestampIsAfterOrderByTimestampDesc(Integer id, Integer FormVersion, LocalDateTime timestamp);

    Page<FormRevisionEntity> getAllByFormIdAndFormVersionOrderByTimestampDesc(Integer formId, Integer FormVersion, Pageable pageable);
}
