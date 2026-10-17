package de.aivot.prosuna.backend.plugins.form.filters;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerListControllerV1.FormOverviewMode;
import de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerConfigV1;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Shared version and search rules for the staff overview, its counters and the public listing. */
public final class FormOverviewSpecifications {
    private FormOverviewSpecifications() {
    }

    @Nonnull
    public static Specification<ProcessNodeEntity> currentProcessVersionSpecification(@Nonnull FormOverviewMode view) {
        var versionField = view == FormOverviewMode.Published ? "publishedVersion" : "draftedVersion";

        // Process nodes are copied per version. Correlating against the process pointers prevents outdated copies
        // from appearing in either overview without loading and filtering the complete result in memory.
        return (root, query, builder) -> {
            var subquery = query.subquery(ProcessEntity.class);
            var processRoot = subquery.from(ProcessEntity.class);

            subquery.select(processRoot).where(
                    builder.equal(processRoot.get("id"), root.get("processId")),
                    builder.isNotNull(processRoot.get(versionField)),
                    builder.equal(processRoot.get(versionField), root.get("processVersion"))
            );

            return builder.exists(subquery);
        };
    }

    @Nonnull
    public static Specification<ProcessNodeEntity> searchSpecification(@Nonnull String search) {
        var pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

        return (root, query, builder) -> {
            var publicTitle = builder.function(
                    "jsonb_extract_path_text",
                    String.class,
                    root.get("configuration"),
                    builder.literal(FormTriggerConfigV1.FORM_LAYOUT),
                    builder.literal(AuthoredElementValues.LITERAL_VALUE_PROPERTY),
                    builder.literal("publicTitle")
            );
            var formSlug = builder.function(
                    "jsonb_extract_path_text",
                    String.class,
                    root.get("configuration"),
                    builder.literal(FormTriggerConfigV1.FORM_SLUG),
                    builder.literal(AuthoredElementValues.LITERAL_VALUE_PROPERTY)
            );

            var processSubquery = query.subquery(ProcessEntity.class);
            var processRoot = processSubquery.from(ProcessEntity.class);
            processSubquery.select(processRoot).where(
                    builder.equal(processRoot.get("id"), root.get("processId")),
                    builder.like(builder.lower(processRoot.get("internalTitle")), pattern)
            );

            return builder.or(
                    builder.like(builder.lower(root.get("name")), pattern),
                    builder.like(builder.lower(publicTitle), pattern),
                    builder.like(builder.lower(formSlug), pattern),
                    builder.exists(processSubquery)
            );
        };
    }
}
