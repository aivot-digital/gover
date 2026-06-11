package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;
import de.aivot.GoverBackend.elements.utils.ElementStreamUtils;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public record ProcessNodeDefinitionMetadata(
        @Nonnull
        List<ReusableUiDefinition> reusableUiDefinitions,
        @Nonnull
        List<ForwardedAttachment> forwardedAttachments,
        @Nonnull
        List<ForwardedProcessDataKey> forwardedProcessDataKeys,
        @Nonnull
        List<ForwardedIdentity> forwardedIdentities
) {
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
                new LinkedList<>(previous.forwardedAttachments()),
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

    public ProcessNodeDefinitionMetadata addForwardedAttachment(@Nonnull
                                                                String fileName,
                                                                @Nonnull
                                                                String label,
                                                                @Nullable
                                                                String subLabel,
                                                                @Nonnull
                                                                ProcessNodeEntity origin) {
        return addForwardedAttachment(new ForwardedAttachment(fileName, label, subLabel, origin));
    }

    public ProcessNodeDefinitionMetadata addForwardedAttachment(ForwardedAttachment forwardedAttachment) {
        forwardedAttachments.add(forwardedAttachment);
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

            if (e instanceof FileUploadInputElement f && StringUtils.isNotNullOrEmpty(f.getSubmittedFileName())) {
                // TODO: Try to guess the extensions
                this.addForwardedAttachment(
                        f.getSubmittedFileName(),
                        f.getSubmittedFileName(),
                        StringUtils.isNotNullOrEmpty(f.getLabel()) ? f.getLabel() : f.getId(),
                        origin
                );
            }

            if (e instanceof GenericStepElement s) {
                var group = new GroupLayoutElement();
                group.setId(s.getId());
                var childCopy = new LinkedList<BaseFormElement>();
                childCopy.add(
                        new HeadlineContentElement()
                                .setContent(s.getTitle())
                );
                childCopy.addAll(s.getChildren());
                group.setChildren(childCopy);
                this.addReusableUiDefinition(
                        StringUtils.isNotNullOrEmpty(s.getTitle()) ? s.getTitle() : s.getId(),
                        null,
                        group,
                        origin
                );
            }
        });

        return this;
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

    public record ForwardedAttachment(
            @Nonnull
            String fileName,
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
