package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.services.SpecificationCountService;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.filters.ProcessFilter;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ProcessDefinitionCountService {
    private final ProcessService processes;
    private final SpecificationCountService counts;

    public ProcessDefinitionCountService(@Nonnull ProcessService processes, @Nonnull SpecificationCountService counts) {
        this.processes = processes;
        this.counts = counts;
    }

    @Nonnull
    public Map<String, Long> count(@Nonnull String userId) {
        // Published processes may also have a draft. Reuse list filters instead of partitioning by one status.
        var drafted = ProcessFilter.create().setIsDrafted(true).build();
        var published = ProcessFilter.create().setIsPublished(true).build();
        var scope = drafted.or(published);
        var access = processes.getReadAccessSpecification(userId);
        if (access != null) {
            scope = scope.and(access);
        }
        return counts.count(ProcessEntity.class, scope, Map.of(
                "drafted", drafted,
                "published", published
        ));
    }
}
