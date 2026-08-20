package de.aivot.prosuna.backend.plugins.form.v1.nodes;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.filters.ProcessNodeFilter;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.ProcessVersionService;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;

@RestController
public class FormTriggerListControllerV1 {
    private final UserService userService;
    private final ProcessService processService;
    private final ProcessNodeService processNodeService;
    private final ProcessVersionService processVersionService;
    private final FormTriggerNodeV1 formTriggerNodeV1;

    @Autowired
    public FormTriggerListControllerV1(UserService userService,
                                       ProcessService processService,
                                       ProcessNodeService processNodeService,
                                       ProcessVersionService processVersionService, FormTriggerNodeV1 formTriggerNodeV1) {
        this.userService = userService;
        this.processService = processService;
        this.processNodeService = processNodeService;
        this.processVersionService = processVersionService;
        this.formTriggerNodeV1 = formTriggerNodeV1;
    }

    @GetMapping("/api/forms/v1/")
    public Page<FormListItem> list(
            @Nonnull @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessNodeFilter filter
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

        filter
                .setProcessNodeDefinitionKey(formTriggerNodeV1.getKey())
                .setProcessNodeDefinitionVersion(1)
                .setProcessIds(accessibleProcessIds);

        var nodes = processNodeService
                .list(pageable, filter);

        return buildPage(nodes);
    }


    @GetMapping("/api/public/forms/")
    public Page<FormListItem> listPublic(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessNodeFilter filter
    ) throws ResponseException {
        filter
                .setProcessNodeDefinitionKey(formTriggerNodeV1.getKey())
                .setProcessNodeDefinitionVersion(1)
                .addAdditionalSpecification((root, query, builder) -> {
                    var subquery = query.subquery(ProcessEntity.class);
                    var processRoot = subquery.from(ProcessEntity.class);

                    subquery.select(processRoot).where(
                            builder.equal(processRoot.get("id"), root.get("processId")),
                            builder.isNotNull(processRoot.get("publishedVersion")),
                            builder.equal(processRoot.get("publishedVersion"), root.get("processVersion"))
                    );

                    return builder.exists(subquery);
                })
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

    private Page<FormListItem> buildPage(Page<ProcessNodeEntity> nodes) throws ResponseException {
        var processCache = new HashMap<Integer, ProcessEntity>();
        var processVersionCache = new HashMap<ProcessVersionEntityId, ProcessVersionEntity>();

        var items = new ArrayList<FormListItem>(nodes.getNumberOfElements());

        for (var node : nodes.getContent()) {
            if (!processCache.containsKey(node.getProcessId())) {
                var process = processService
                        .retrieve(node.getProcessId())
                        .orElseThrow(() -> ResponseException.internalServerError("Process with id %d not found".formatted(node.getProcessId())));
                processCache.put(node.getProcessId(), process);
            }
            var process = processCache.get(node.getProcessId());

            var versionId = ProcessVersionEntityId.of(node.getProcessId(), node.getProcessVersion());
            if (!processVersionCache.containsKey(versionId)) {
                var processVersion = processVersionService
                        .retrieve(versionId)
                        .orElseThrow(() -> ResponseException.internalServerError("Process version with id %s not found".formatted(versionId)));
                processVersionCache.put(versionId, processVersion);
            }
            var version = processVersionCache.get(versionId);

            items.add(new FormListItem(process, version, node));
        }

        return new PageImpl<>(items, nodes.getPageable(), nodes.getTotalElements());
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
