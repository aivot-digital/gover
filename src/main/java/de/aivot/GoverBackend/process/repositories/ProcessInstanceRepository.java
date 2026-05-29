package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ProcessInstanceRepository extends JpaRepository<ProcessInstanceEntity, Long>, JpaSpecificationExecutor<ProcessInstanceEntity> {
    List<ProcessInstanceEntity> findAllByStatus(ProcessInstanceStatus status);

    List<ProcessInstanceEntity> findAllByStatusAndKeepUntilLessThanEqual(ProcessInstanceStatus status,
                                                                         Instant keepUntil,
                                                                         Pageable pageable);

    long countByStatusAndKeepUntilLessThanEqual(ProcessInstanceStatus status,
                                                Instant keepUntil);

    List<ProcessInstanceEntity> findAllByCreatedForTestClaimId(Integer createdForTestClaimId);

    long countAllByStatusIs(ProcessInstanceStatus status);

    boolean existsByCaseNumber(String caseNumber);

    /**
     * Reads the highest increment used for the already rendered static parts of a case number template.
     *
     * <p>The query deliberately compares prefix/suffix and the overall character length instead of using a shared ID
     * generator helper. That keeps the process module independent and lets the increment reset automatically whenever
     * one of the temporal placeholders changes.</p>
     */
    @Query(value = """
            SELECT MAX(CAST(SUBSTRING(case_number FROM :incrementStart FOR :padding) AS INTEGER))
            FROM process_instances
            WHERE CHAR_LENGTH(case_number) = :expectedLength
              AND LEFT(case_number, :prefixLength) = :prefix
              AND RIGHT(case_number, :suffixLength) = :suffix
              AND SUBSTRING(case_number FROM :incrementStart FOR :padding) ~ '^[0-9]+$'
            """, nativeQuery = true)
    Integer getMaxCaseNumberIncrement(@Param("prefix") String prefix,
                                      @Param("suffix") String suffix,
                                      @Param("prefixLength") int prefixLength,
                                      @Param("suffixLength") int suffixLength,
                                      @Param("incrementStart") int incrementStart,
                                      @Param("padding") int padding,
                                      @Param("expectedLength") int expectedLength);
}
