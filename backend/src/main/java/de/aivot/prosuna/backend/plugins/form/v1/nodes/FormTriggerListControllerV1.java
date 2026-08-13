package de.aivot.prosuna.backend.plugins.form.v1.nodes;

import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.filters.ProcessNodeFilter;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.ProcessVersionService;
import de.aivot.prosuna.backend.process.services.PublicUrlService;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
public class FormTriggerListControllerV1 {
    private final UserService userService;
    private final ProcessService processService;
    private final ProcessNodeService processNodeService;
    private final ProcessVersionService processVersionService;
    private final FormTriggerNodeV1 formTriggerNodeV1;
    private final PublicUrlService publicUrlService;

    @Autowired
    public FormTriggerListControllerV1(UserService userService,
                                       ProcessService processService,
                                       ProcessNodeService processNodeService,
                                       ProcessVersionService processVersionService,
                                       FormTriggerNodeV1 formTriggerNodeV1,
                                       PublicUrlService publicUrlService) {
        this.userService = userService;
        this.processService = processService;
        this.processNodeService = processNodeService;
        this.processVersionService = processVersionService;
        this.formTriggerNodeV1 = formTriggerNodeV1;
        this.publicUrlService = publicUrlService;
    }

    @GetMapping("/api/forms/v1/")
    public Page<FormOverviewItem> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @RequestParam(defaultValue = "Published") FormOverviewMode view,
            @Nullable @RequestParam(required = false) String search
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var accessibleProcessIds = processService
                .listAllByAccessibleForUser(Pageable.unpaged(), execUser.getId(), null)
                .stream()
                .map(ProcessEntity::getId)
                .toList();

        if (accessibleProcessIds.isEmpty()) {
            return Page.empty(pageable);
        }

        var filter = ProcessNodeFilter
                .create()
                .setProcessNodeDefinitionKey(formTriggerNodeV1.getKey())
                .setProcessNodeDefinitionVersion(1)
                .setProcessIds(accessibleProcessIds)
                .addAdditionalSpecification(currentProcessVersionSpecification(view));

        var normalizedSearch = StringUtils.toNullableTrimmedString(search);
        if (normalizedSearch != null) {
            filter.addAdditionalSpecification(searchSpecification(normalizedSearch));
        }

        var nodes = processNodeService
                .list(pageable, filter);

        return buildOverviewPage(nodes);
    }


    @GetMapping("/api/public/forms/")
    public Page<FormListItem> listPublic(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessNodeFilter filter
    ) throws ResponseException {
        filter
                .setProcessNodeDefinitionKey(formTriggerNodeV1.getKey())
                .setProcessNodeDefinitionVersion(1)
                .addAdditionalSpecification(currentProcessVersionSpecification(FormOverviewMode.Published))
                .addAdditionalSpecification((root, query, builder) -> {
                    var showOnFormIndexPage = builder.function(
                            "jsonb_extract_path_text",
                            String.class,
                            root.get("configuration"),
                            builder.literal(FormTriggerConfigV1.FORM_LAYOUT),
                            builder.literal("showOnFormIndexPage")
                    );

                    return builder.or(
                            builder.isNull(showOnFormIndexPage),
                            builder.equal(showOnFormIndexPage, "true")
                    );
                });

        var nodes = processNodeService
                .list(pageable, filter);

        return buildPage(nodes);
    }

    @Nonnull
    private Specification<ProcessNodeEntity> currentProcessVersionSpecification(@Nonnull FormOverviewMode view) {
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
    private Specification<ProcessNodeEntity> searchSpecification(@Nonnull String search) {
        var pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

        return (root, query, builder) -> {
            var publicTitle = builder.function(
                    "jsonb_extract_path_text",
                    String.class,
                    root.get("configuration"),
                    builder.literal(FormTriggerConfigV1.FORM_LAYOUT),
                    builder.literal("publicTitle")
            );
            var formSlug = builder.function(
                    "jsonb_extract_path_text",
                    String.class,
                    root.get("configuration"),
                    builder.literal(FormTriggerConfigV1.FORM_SLUG)
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

    private Page<FormOverviewItem> buildOverviewPage(Page<ProcessNodeEntity> nodes) throws ResponseException {
        var processCache = new HashMap<Integer, ProcessEntity>();
        var processVersionCache = new HashMap<ProcessVersionEntityId, ProcessVersionEntity>();
        var items = new ArrayList<FormOverviewItem>(nodes.getNumberOfElements());

        for (var node : nodes.getContent()) {
            var process = retrieveProcess(node, processCache);
            var version = retrieveProcessVersion(node, processVersionCache);
            var formSlug = StringUtils.toNullableTrimmedString(
                    node.getConfiguration().get(FormTriggerConfigV1.FORM_SLUG)
            );
            var formLayout = resolveFormLayout(node);
            var formTitle = formLayout != null && StringUtils.isNotNullOrEmpty(formLayout.getPublicTitle())
                    ? formLayout.getPublicTitle().trim()
                    : version.getPublicTitle();
            var showOnFormIndexPage = formLayout == null || !Boolean.FALSE.equals(formLayout.getShowOnFormIndexPage());
            // Draft form slugs may match a published version of the same process. Exposing that URL here would
            // misleadingly suggest that the draft itself can be opened publicly.
            var publicUrl = formSlug == null || version.getStatus() != ProcessVersionStatus.Published
                    ? null
                    : publicUrlService.createPublicFormUrl(process, formSlug);

            items.add(new FormOverviewItem(
                    node.getId(),
                    node.resolveName(formTriggerNodeV1),
                    formTitle,
                    process.getId(),
                    process.getInternalTitle(),
                    version.getProcessVersion(),
                    version.getStatus(),
                    publicUrl,
                    showOnFormIndexPage,
                    node.getUpdated(),
                    version.getPublished()
            ));
        }

        return new PageImpl<>(items, nodes.getPageable(), nodes.getTotalElements());
    }

    @Nullable
    private FormLayoutElement resolveFormLayout(@Nonnull ProcessNodeEntity node) {
        var rawLayout = node.getConfiguration().get(FormTriggerConfigV1.FORM_LAYOUT);
        if (rawLayout == null) {
            return null;
        }

        return ObjectMapperFactory
                .getInstance()
                .convertValue(rawLayout, FormLayoutElement.class);
    }

    private Page<FormListItem> buildPage(Page<ProcessNodeEntity> nodes) throws ResponseException {
        var processCache = new HashMap<Integer, ProcessEntity>();
        var processVersionCache = new HashMap<ProcessVersionEntityId, ProcessVersionEntity>();

        var items = new ArrayList<FormListItem>(nodes.getNumberOfElements());

        for (var node : nodes.getContent()) {
            var process = retrieveProcess(node, processCache);
            var version = retrieveProcessVersion(node, processVersionCache);

            items.add(new FormListItem(process, version, node));
        }

        return new PageImpl<>(items, nodes.getPageable(), nodes.getTotalElements());
    }

    @Nonnull
    private ProcessEntity retrieveProcess(@Nonnull ProcessNodeEntity node,
                                          @Nonnull Map<Integer, ProcessEntity> cache) throws ResponseException {
        if (!cache.containsKey(node.getProcessId())) {
            var process = processService
                    .retrieve(node.getProcessId())
                    .orElseThrow(() -> ResponseException.internalServerError(
                            "Process with id %d not found".formatted(node.getProcessId())
                    ));
            cache.put(node.getProcessId(), process);
        }

        return cache.get(node.getProcessId());
    }

    @Nonnull
    private ProcessVersionEntity retrieveProcessVersion(@Nonnull ProcessNodeEntity node,
                                                        @Nonnull Map<ProcessVersionEntityId, ProcessVersionEntity> cache) throws ResponseException {
        var versionId = ProcessVersionEntityId.of(node.getProcessId(), node.getProcessVersion());
        if (!cache.containsKey(versionId)) {
            var version = processVersionService
                    .retrieve(versionId)
                    .orElseThrow(() -> ResponseException.internalServerError(
                            "Process version with id %s not found".formatted(versionId)
                    ));
            cache.put(versionId, version);
        }

        return cache.get(versionId);
    }

    public enum FormOverviewMode {
        Published,
        Drafted,
    }

    public record FormOverviewItem(
            int id,
            @Nonnull String nodeName,
            @Nonnull String formTitle,
            int processId,
            @Nonnull String processTitle,
            int processVersion,
            @Nonnull ProcessVersionStatus status,
            @Nullable String publicUrl,
            boolean showOnFormIndexPage,
            @Nonnull Instant updated,
            @Nullable Instant published
    ) {
    }

    public record FormListItem(
            @Nonnull
            ProcessEntity process,
            @Nonnull
            ProcessVersionEntity version,
            @Nonnull
            ProcessNodeEntity node
    ) {

    }
}
