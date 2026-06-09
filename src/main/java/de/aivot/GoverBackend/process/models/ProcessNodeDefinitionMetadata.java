package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;

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
