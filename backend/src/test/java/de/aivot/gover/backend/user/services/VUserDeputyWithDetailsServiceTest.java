package de.aivot.gover.backend.user.services;

import de.aivot.gover.backend.core.services.BusinessTime;
import de.aivot.gover.backend.user.entities.VUserDeputyWithDetailsEntity;
import de.aivot.gover.backend.user.repositories.VUserDeputyWithDetailsRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VUserDeputyWithDetailsServiceTest {
    @Test
    void shouldCalculateActiveStateUsingCurrentBusinessDate() throws Exception {
        var repository = mock(VUserDeputyWithDetailsRepository.class);
        var businessTime = new BusinessTime(
                ZoneId.of("Europe/Berlin"),
                Clock.fixed(
                        Instant.parse("2026-07-29T22:30:00Z"),
                        ZoneId.of("UTC")
                )
        );
        var service = new VUserDeputyWithDetailsService(repository, businessTime);
        var active = deputy(LocalDate.of(2026, 7, 30), LocalDate.of(2026, 7, 30));
        var future = deputy(LocalDate.of(2026, 7, 31), null);

        when(repository.findAll(
                ArgumentMatchers.<Specification<VUserDeputyWithDetailsEntity>>isNull(),
                any(Pageable.class)
        ))
                .thenReturn(new PageImpl<>(List.of(active, future)));

        var result = service.performList(
                PageRequest.of(0, 10),
                null,
                null
        );

        assertTrue(result.getContent().get(0).getActive());
        assertFalse(result.getContent().get(1).getActive());
    }

    private VUserDeputyWithDetailsEntity deputy(LocalDate fromDate, LocalDate untilDate) {
        return new VUserDeputyWithDetailsEntity()
                .setFromDate(fromDate)
                .setUntilDate(untilDate);
    }

}
