package de.aivot.prosuna.backend.plugins.form.services;

import de.aivot.prosuna.backend.lib.services.SpecificationCountService;
import de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerListControllerV1.FormOverviewMode;
import de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerNodeV1;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.filters.ProcessNodeFilter;
import de.aivot.prosuna.backend.process.services.ProcessService;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static de.aivot.prosuna.backend.plugins.form.filters.FormOverviewSpecifications.currentProcessVersionSpecification;

@Service
@Transactional(readOnly = true)
public class FormOverviewCountService {
    private final ProcessService processes;
    private final SpecificationCountService counts;
    private final FormTriggerNodeV1 formTrigger;

    public FormOverviewCountService(@Nonnull ProcessService processes, @Nonnull SpecificationCountService counts,
                                     @Nonnull FormTriggerNodeV1 formTrigger) {
        this.processes = processes;
        this.counts = counts;
        this.formTrigger = formTrigger;
    }

    @Nonnull
    public Map<String, Long> count(@Nonnull String userId) {
        var published = currentProcessVersionSpecification(FormOverviewMode.Published);
        var drafted = currentProcessVersionSpecification(FormOverviewMode.Drafted);
        var filter = ProcessNodeFilter.create()
                .setProcessNodeDefinitionKey(formTrigger.getKey())
                .setProcessNodeDefinitionVersion(1)
                .addAdditionalSpecification(published.or(drafted));

        var access = processes.getReadAccessSpecification(userId);
        if (access != null) {
            // Correlate the shared process scope without loading process entities or multiplying form rows by grants.
            filter.addAdditionalSpecification((root, query, builder) -> {
                var subquery = query.subquery(ProcessEntity.class);
                var process = subquery.from(ProcessEntity.class);
                subquery.select(process).where(builder.equal(process.get("id"), root.get("processId")),
                        access.toPredicate(process, query, builder));
                return builder.exists(subquery);
            });
        }
        return counts.count(ProcessNodeEntity.class, filter.build(), Map.of("Published", published, "Drafted", drafted));
    }
}
