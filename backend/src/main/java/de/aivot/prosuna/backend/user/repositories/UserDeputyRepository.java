package de.aivot.prosuna.backend.user.repositories;

import de.aivot.prosuna.backend.user.entities.UserDeputyEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

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

    // The view contains only relations valid today and resolves chains to the represented user.
    @Query(value = """
            SELECT DISTINCT original_user_id
            FROM v_user_is_recursively_deputy_for
            WHERE deputy_user_id = :deputyUserId
            """, nativeQuery = true)
    List<String> findActivelyRepresentedUserIds(@Nonnull @Param("deputyUserId") String deputyUserId);

    @Query(value = """
            SELECT DISTINCT original_user_id AS "originalUserId", deputy_user_id AS "deputyUserId"
            FROM v_user_is_recursively_deputy_for
            WHERE original_user_id IN (:originalUserIds)
            """, nativeQuery = true)
    List<ActiveDeputyProjection> findActiveDeputies(@Nonnull @Param("originalUserIds") Collection<String> originalUserIds);

    interface ActiveDeputyProjection {
        String getOriginalUserId();

        String getDeputyUserId();
    }
}
