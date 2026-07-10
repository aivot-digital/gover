package de.aivot.gover.backend.process.models;

import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.BaseFormElement;
import de.aivot.gover.backend.elements.models.elements.BaseInputElement;
import de.aivot.gover.backend.elements.models.elements.LayoutElement;
import de.aivot.gover.backend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.gover.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.gover.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.gover.backend.elements.utils.ElementStreamUtils;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record ProcessNodeDefinitionMetadata(
        @Nonnull
        List<ReusableUiDefinition> reusableUiDefinitions,
        @Nonnull
        List<ForwardedAttachmentSet> forwardedAttachmentSets,
        @Nonnull
        List<ForwardedProcessDataKey> forwardedProcessDataKeys,
        @Nonnull
        List<ForwardedIdentity> forwardedIdentities
) {
    private static final String COMPLETE_FORM_LABEL = "Gesamtes Formular";
    private static final String FALLBACK_UI_DEFINITION_LABEL = "UI-Definition";
    private static final Pattern ELEMENT_ID_PATTERN = Pattern.compile("^[a-z][a-zA-Z0-9_]*$");

    public static ProcessNodeDefinitionMetadata empty() {
        return new ProcessNodeDefinitionMetadata(
                new LinkedList<>(),
                new LinkedList<>(),
                new LinkedList<>(),
                new LinkedList<>()
        );
    }

    public static ProcessNodeDefinitionMetadata reuse(ProcessNodeDefinitionMetadata previous) {
        return new ProcessNodeDefinitionMetadata(
                new LinkedList<>(previous.reusableUiDefinitions()),
                new LinkedList<>(previous.forwardedAttachmentSets()),
                new LinkedList<>(previous.forwardedProcessDataKeys()),
                new LinkedList<>(previous.forwardedIdentities())
        );
    }


    public ProcessNodeDefinitionMetadata addReusableUiDefinition(@Nonnull
                                                                 String label,
                                                                 @Nullable
                                                                 String subLabel,
                                                                 @Nonnull
                                                                 BaseElement uiDefinition,
                                                                 @Nonnull
                                                                 ProcessNodeEntity origin) {
        return addReusableUiDefinition(new ReusableUiDefinition(label, subLabel, uiDefinition, origin));
    }

    public ProcessNodeDefinitionMetadata addReusableUiDefinition(ReusableUiDefinition reusableUiDefinition) {
        reusableUiDefinitions.add(reusableUiDefinition);
        return this;
    }

    public ProcessNodeDefinitionMetadata addForwardedAttachmentSet(@Nonnull
                                                                   String dataKey,
                                                                   @Nonnull
                                                                   String label,
                                                                   @Nullable
                                                                   String subLabel,
                                                                   @Nonnull
                                                                   ProcessNodeEntity origin) {
        return addForwardedAttachmentSet(new ForwardedAttachmentSet(dataKey, label, subLabel, origin));
    }

    public ProcessNodeDefinitionMetadata addForwardedAttachmentSet(ForwardedAttachmentSet forwardedAttachmentSet) {
        forwardedAttachmentSets.add(forwardedAttachmentSet);
        return this;
    }

    public ProcessNodeDefinitionMetadata addForwardedProcessDataKey(@Nonnull
                                                                    String processDataKey,
                                                                    @Nonnull
                                                                    String label,
                                                                    @Nullable
                                                                    String subLabel,
                                                                    @Nonnull
                                                                    ProcessNodeEntity origin) {
        return addForwardedProcessDataKey(new ForwardedProcessDataKey(processDataKey, label, subLabel, origin));
    }

    public ProcessNodeDefinitionMetadata addForwardedProcessDataKey(ForwardedProcessDataKey forwardedProcessDataKey) {
        forwardedProcessDataKeys.add(forwardedProcessDataKey);
        return this;
    }

    public ProcessNodeDefinitionMetadata addForwardedIdentity(@Nonnull
                                                              String identityId,
                                                              @Nonnull
                                                              String label,
                                                              @Nullable
                                                              String subLabel,
                                                              @Nonnull
                                                              ProcessNodeEntity origin) {
        return addForwardedIdentity(new ForwardedIdentity(identityId, label, subLabel, origin));
    }


    public ProcessNodeDefinitionMetadata addForwardedIdentity(ForwardedIdentity forwardedIdentity) {
        forwardedIdentities.add(forwardedIdentity);
        return this;
    }

    public ProcessNodeDefinitionMetadata withLayout(@Nullable LayoutElement<?> layout, @Nonnull ProcessNodeEntity origin) {
        if (layout == null) {
            return this;
        }

        if (layout instanceof FormLayoutElement formLayout) {
            addCompleteFormReusableUiDefinition(formLayout, origin);
        } else if (layout instanceof GroupLayoutElement groupLayout) {
            addRootGroupReusableUiDefinition(groupLayout, origin);
        }

        ElementStreamUtils.applyActionWithParents((BaseElement) layout, (parents, e) -> {
            if (e instanceof BaseInputElement<?> i && StringUtils.isNotNullOrEmpty(i.getDestinationKey())) {
                var parentDestinationKey = parents
                        .stream()
                        .filter(p -> p instanceof ReplicatingContainerLayoutElement)
                        .map(p -> (ReplicatingContainerLayoutElement) p)
                        .map(ReplicatingContainerLayoutElement::getDestinationKey)
                        .filter(StringUtils::isNotNullOrEmpty)
                        .collect(Collectors.joining(".*."));
                if (StringUtils.isNotNullOrEmpty(parentDestinationKey)) {
                    parentDestinationKey += ".*.";
                }

                this.addForwardedProcessDataKey(
                        parentDestinationKey + i.getDestinationKey(),
                        StringUtils.isNotNullOrEmpty(i.getLabel()) ? i.getLabel() : i.getId(),
                        i.getHint(),
                        origin
                );
            }

            if (e instanceof FileUploadInputElement f) {
                var attachmentSetDataKey = resolveAttachmentSetDataKey(f);
                if (attachmentSetDataKey != null) {
                    this.addForwardedAttachmentSet(
                            attachmentSetDataKey,
                            StringUtils.isNotNullOrEmpty(f.getLabel()) ? f.getLabel() : attachmentSetDataKey,
                            f.getHint(),
                            origin
                    );
                }
            }

            if (e instanceof GenericStepElement s) {
                this.addReusableUiDefinition(
                        resolveStepTitle(s),
                        null,
                        createReusableGroupForStep(s),
                        origin
                );
            }
        });

        return this;
    }

    private void addCompleteFormReusableUiDefinition(@Nonnull FormLayoutElement formLayout,
                                                     @Nonnull ProcessNodeEntity origin) {
        var stepGroups = formLayout
                .getChildren()
                .stream()
                .filter(GenericStepElement.class::isInstance)
                .map(GenericStepElement.class::cast)
                .map(ProcessNodeDefinitionMetadata::createReusableGroupForStep)
                .map(BaseFormElement.class::cast)
                .toList();

        if (stepGroups.isEmpty()) {
            return;
        }

        var group = new GroupLayoutElement();
        group.setId(resolveElementId(formLayout.getId(), "gp"));
        group.setName(COMPLETE_FORM_LABEL);
        group.setChildren(new LinkedList<>(stepGroups));

        this.addReusableUiDefinition(
                COMPLETE_FORM_LABEL,
                null,
                group,
                origin
        );
    }

    private void addRootGroupReusableUiDefinition(@Nonnull GroupLayoutElement groupLayout,
                                                  @Nonnull ProcessNodeEntity origin) {
        if (groupLayout.getChildren().isEmpty()) {
            return;
        }

        this.addReusableUiDefinition(
                StringUtils.isNotNullOrEmpty(groupLayout.getName()) ? groupLayout.getName() : FALLBACK_UI_DEFINITION_LABEL,
                null,
                groupLayout,
                origin
        );
    }

    @Nonnull
    private static GroupLayoutElement createReusableGroupForStep(@Nonnull GenericStepElement step) {
        var title = resolveStepTitle(step);

        var group = new GroupLayoutElement();
        group.setId(resolveElementId(step.getId(), "gp"));
        group.setName(title);

        var headline = new HeadlineContentElement();
        headline.setId(createElementId("hd"));
        headline.setContent(title);

        var children = new LinkedList<BaseFormElement>();
        children.add(headline);
        children.addAll(step.getChildren());
        group.setChildren(children);

        return group;
    }

    @Nonnull
    private static String resolveStepTitle(@Nonnull GenericStepElement step) {
        return step.getResolvedTitle();
    }

    @Nonnull
    private static String resolveElementId(@Nullable String candidate, @Nonnull String prefix) {
        if (candidate != null && ELEMENT_ID_PATTERN.matcher(candidate).matches()) {
            return candidate;
        }

        return createElementId(prefix);
    }

    @Nonnull
    private static String createElementId(@Nonnull String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "_");
    }

    @Nullable
    private static String resolveAttachmentSetDataKey(@Nonnull FileUploadInputElement element) {
        var sourceKey = StringUtils.toNullableTrimmedString(element.getDestinationKey());
        if (sourceKey == null) {
            sourceKey = StringUtils.toNullableTrimmedString(element.getId());
        }

        return sourceKey == null ? null : sourceKey.replace('.', '_');
    }

    public record ReusableUiDefinition(
            @Nonnull
            String label,
            @Nullable
            String subLabel,
            @Nonnull
            BaseElement uiDefinition,
            @Nonnull
            ProcessNodeEntity origin
    ) {
    }

    public record ForwardedAttachmentSet(
            @Nonnull
            String dataKey,
            @Nonnull
            String label,
            @Nullable
            String subLabel,
            @Nonnull
            ProcessNodeEntity origin
    ) {
    }

    public record ForwardedProcessDataKey(
            @Nonnull
            String processDataKey,
            @Nonnull
            String label,
            @Nullable
            String subLabel,
            @Nonnull
            ProcessNodeEntity origin
    ) {
    }

    public record ForwardedIdentity(
            @Nonnull
            String identityId,
            @Nonnull
            String label,
            @Nullable
            String subLabel,
            @Nonnull
            ProcessNodeEntity origin
    ) {
    }
}
