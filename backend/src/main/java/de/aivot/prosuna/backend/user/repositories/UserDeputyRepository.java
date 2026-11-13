package de.aivot.prosuna.backend.user.repositories;

import de.aivot.prosuna.backend.user.entities.UserDeputyEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface UserDeputyRepository extends JpaRepository<UserDeputyEntity, Integer>, JpaSpecificationExecutor<UserDeputyEntity> {
    // Runs the same check as the database trigger, so a rejected relation gets a readable error.
    @Query(value = """
            SELECT fun_user_deputy_creates_cycle(
                CAST(:originalUserId AS varchar),
                CAST(:deputyUserId AS varchar),
                CAST(:fromDate AS date),
                CAST(:untilDate AS date),
                CAST(:ignoredId AS integer)
            )
            """, nativeQuery = true)
    boolean createsCycle(@Nonnull @Param("originalUserId") String originalUserId,
                         @Nonnull @Param("deputyUserId") String deputyUserId,
                         @Nonnull @Param("fromDate") LocalDate fromDate,
                         @Nullable @Param("untilDate") LocalDate untilDate,
                         @Nullable @Param("ignoredId") Integer ignoredId);
}
