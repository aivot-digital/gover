package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.elements.enums.OptionsSourceType;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.BaseFormElement;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.elements.models.elements.layout.*;
import de.aivot.prosuna.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.prosuna.backend.elements.utils.ElementStreamUtils;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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
        List<ForwardedIdentity> forwardedIdentities,
        @Nonnull
        List<InputVariableSuggestion> inputVariables
) {
    private static final String COMPLETE_FORM_LABEL = "Gesamtes Formular";
    private static final String FALLBACK_UI_DEFINITION_LABEL = "UI-Definition";
    private static final int MAX_ATTACHMENT_SET_DATA_KEY_LENGTH = 255;
    private static final Pattern ELEMENT_ID_PATTERN = Pattern.compile("^[a-z][a-zA-Z0-9_]*$");

    public ProcessNodeDefinitionMetadata(
            @Nonnull List<ReusableUiDefinition> reusableUiDefinitions,
            @Nonnull List<ForwardedAttachmentSet> forwardedAttachmentSets,
            @Nonnull List<ForwardedProcessDataKey> forwardedProcessDataKeys,
            @Nonnull List<ForwardedIdentity> forwardedIdentities
    ) {
        this(reusableUiDefinitions, forwardedAttachmentSets, forwardedProcessDataKeys, forwardedIdentities, new LinkedList<>());
    }

    public static ProcessNodeDefinitionMetadata empty() {
        return new ProcessNodeDefinitionMetadata(
                new LinkedList<>(),
                new LinkedList<>(),
                new LinkedList<>(),
                new LinkedList<>(),
                new LinkedList<>()
        );
    }

    public static ProcessNodeDefinitionMetadata reuse(ProcessNodeDefinitionMetadata previous) {
        return new ProcessNodeDefinitionMetadata(
                copyList(previous.reusableUiDefinitions()),
                copyList(previous.forwardedAttachmentSets()),
                copyList(previous.forwardedProcessDataKeys()),
                copyList(previous.forwardedIdentities()),
                copyList(previous.inputVariables())
        );
    }

    // Plugin providers may leave lists unset or immutable. Copies are always complete and mutable.
    @Nonnull
    private static <T> LinkedList<T> copyList(@Nullable List<T> source) {
        return source == null ? new LinkedList<>() : new LinkedList<>(source);
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

    public ProcessNodeDefinitionMetadata addReusableUiDefinition(@Nonnull
                                                                 String label,
                                                                 @Nullable
                                                                 String subLabel,
                                                                 @Nonnull
                                                                 BaseElement uiDefinition,
                                                                 @Nonnull
                                                                 ProcessNodeEntity origin,
                                                                 @Nonnull
                                                                 ReusableUiDefinitionKind kind) {
        return addReusableUiDefinition(new ReusableUiDefinition(label, subLabel, uiDefinition, origin, kind));
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
                                                                   boolean isMultifile,
                                                                   @Nonnull
                                                                   ProcessNodeEntity origin) {
        return addForwardedAttachmentSet(new ForwardedAttachmentSet(dataKey, label, subLabel, isMultifile, origin));
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

    public ProcessNodeDefinitionMetadata addForwardedProcessDataKey(@Nonnull
                                                                    String processDataKey,
                                                                    @Nonnull
                                                                    String label,
                                                                    @Nullable
                                                                    String subLabel,
                                                                    @Nonnull
                                                                    ProcessNodeEntity origin,
                                                                    @Nullable
                                                                    List<ForwardedProcessDataKey.ValueOption> valueOptions) {
        return addForwardedProcessDataKey(new ForwardedProcessDataKey(processDataKey, label, subLabel, origin, valueOptions, null, null));
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
                                                              List<UUID> identityProviderKeys,
                                                              @Nonnull
                                                              ProcessNodeEntity origin) {
        return addForwardedIdentity(new ForwardedIdentity(identityId, label, subLabel, identityProviderKeys, origin));
    }


    public ProcessNodeDefinitionMetadata addForwardedIdentity(ForwardedIdentity forwardedIdentity) {
        forwardedIdentities.add(forwardedIdentity);
        return this;
    }

    public ProcessNodeDefinitionMetadata addInputVariable(@Nonnull InputVariableSuggestion suggestion) {
        var alreadyPresent = inputVariables.stream().anyMatch(existing ->
                existing.source() == suggestion.source() &&
                        java.util.Objects.equals(existing.path(), suggestion.path()) &&
                        java.util.Objects.equals(existing.nodeDataKey(), suggestion.nodeDataKey())
        );
        if (!alreadyPresent) {
            inputVariables.add(suggestion);
        }
        return this;
    }

    public ProcessNodeDefinitionMetadata withLayout(@Nullable LayoutElement<?> layout, @Nonnull ProcessNodeEntity origin) {
        if (layout == null) {
            return this;
        }

        if (layout instanceof FormLayoutElement formLayout) {
            addCompleteFormReusableUiDefinition(formLayout, origin);
        } else if (layout instanceof GroupLayoutElement ||
                layout instanceof StepperLayoutElement ||
                layout instanceof TabLayoutElement) {
            addCompleteUiDefinitionReusableUiDefinition((BaseElement) layout, origin);
        }

        ElementStreamUtils.applyActionWithParents((BaseElement) layout, (parents, e) -> {
            if (e instanceof BaseInputElement<?> i && StringUtils.isNotNullOrEmpty(i.getDestinationKey())) {
                var parentDestinationKey = parents
                        .stream()
                        .filter(p -> p instanceof ReplicatingContainerLayoutElement)
                        .map(p -> (ReplicatingContainerLayoutElement) p)
                        .map(ReplicatingContainerLayoutElement::getDestinationKey)
                        .filter(StringUtils::isNotNullOrEmpty)
                        .collect(Collectors.joining("[*]."));
                if (StringUtils.isNotNullOrEmpty(parentDestinationKey)) {
                    parentDestinationKey += "[*].";
                }

                var valueOptions = getValueOptions(i);
                var codeListKey = getCodeListKey(i);

                this.addForwardedProcessDataKey(new ForwardedProcessDataKey(
                        parentDestinationKey + i.getDestinationKey(),
                        StringUtils.isNotNullOrEmpty(i.getLabel()) ? i.getLabel() : i.getId(),
                        i.getHint(),
                        origin,
                        valueOptions,
                        codeListKey,
                        getCodeListValueMode(i, codeListKey)
                ));
            }

            if (e instanceof FileUploadInputElement f) {
                var attachmentSetDataKey = resolveAttachmentSetDataKey(f);
                if (attachmentSetDataKey != null) {
                    var isMultifile = Boolean.TRUE.equals(f.getIsMultifile()) ||
                            parents.stream().anyMatch(ReplicatingContainerLayoutElement.class::isInstance);

                    this.addForwardedAttachmentSet(
                            attachmentSetDataKey,
                            StringUtils.isNotNullOrEmpty(f.getLabel()) ? f.getLabel() : attachmentSetDataKey,
                            f.getHint(),
                            isMultifile,
                            origin
                    );
                }
            }

            if (e instanceof GenericStepElement s) {
                var kind = resolveReusableUiDefinitionKind(parents);
                if (kind != null) {
                    this.addReusableUiDefinition(
                            resolveStepTitle(s),
                            null,
                            createReusableGroupForStep(s),
                            origin,
                            kind
                    );
                }
            }
        });

        return this;
    }

    private @Nullable List<ForwardedProcessDataKey.ValueOption> getValueOptions(BaseInputElement<?> i) {
        List<ForwardedProcessDataKey.ValueOption> valueOptions = switch (i) {
            case RadioInputElement r -> {
                OptionsSourceType os = r.getOptionsSource();
                if (os == null) {
                    os = OptionsSourceType.Manual;
                }

                yield switch (os) {
                    case Manual -> r
                            .getOptions() == null ? null : r
                            .getOptions()
                            .stream()
                            .filter(o -> o.getValue() != null && o.getLabel() != null)
                            .map(o -> new ForwardedProcessDataKey.ValueOption(o.getValue(), o.getLabel()))
                            .toList();
                    case CodeList -> null;
                };
            }
            case SelectInputElement s -> {
                var os = s.getOptionsSource();
                if (os == null) {
                    os = OptionsSourceType.Manual;
                }

                yield switch (os) {
                    case Manual -> s
                            .getOptions() == null ? null : s
                            .getOptions()
                            .stream()
                            .filter(o -> o.getValue() != null && o.getLabel() != null)
                            .map(o -> new ForwardedProcessDataKey.ValueOption(o.getValue(), o.getLabel()))
                            .toList();
                    case CodeList -> null;
                };
            }
            case MultiCheckboxInputElement m -> {
                var os = m.getOptionsSource();
                if (os == null) {
                    os = OptionsSourceType.Manual;
                }

                yield switch (os) {
                    case Manual -> m.getOptions() == null ? null : m
                            .getOptions()
                            .stream()
                            .filter(o -> o.getValue() != null && o.getLabel() != null)
                            .map(o -> new ForwardedProcessDataKey.ValueOption(o.getValue(), o.getLabel()))
                            .toList();
                    case CodeList -> null;
                };
            }
            case ChipInputElement c -> {
                var os = c.getOptionsSource();
                if (os == null) {
                    os = OptionsSourceType.Manual;
                }

                yield switch (os) {
                    case Manual -> c.getSuggestions() == null ? null : c
                            .getSuggestions()
                            .stream()
                            .filter(Objects::nonNull)
                            .map(s -> new ForwardedProcessDataKey.ValueOption(s, s))
                            .toList();
                    case CodeList -> null;
                };
            }
            default -> null;
        };

        return valueOptions;
    }

    private static @Nullable String getCodeListKey(@Nonnull BaseInputElement<?> i) {
        var codeListKey = switch (i) {
            case RadioInputElement r when r.getOptionsSource() == OptionsSourceType.CodeList -> r.getCodeListKey();
            case SelectInputElement s when s.getOptionsSource() == OptionsSourceType.CodeList -> s.getCodeListKey();
            case MultiCheckboxInputElement m when m.getOptionsSource() == OptionsSourceType.CodeList -> m.getCodeListKey();
            case ChipInputElement c when c.getOptionsSource() == OptionsSourceType.CodeList -> c.getCodeListKey();
            default -> null;
        };

        return StringUtils.toNullableTrimmedString(codeListKey);
    }

    private static @Nullable ForwardedProcessDataKey.CodeListValueMode getCodeListValueMode(@Nonnull BaseInputElement<?> i,
                                                                                             @Nullable String codeListKey) {
        if (codeListKey == null) {
            return null;
        }

        // Chip inputs store plain strings, so they keep the label of the selected code list item instead of its value.
        return i instanceof ChipInputElement
                ? ForwardedProcessDataKey.CodeListValueMode.LabelOnly
                : ForwardedProcessDataKey.CodeListValueMode.ValueAndLabel;
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
                origin,
                ReusableUiDefinitionKind.CompleteForm
        );
    }

    private void addCompleteUiDefinitionReusableUiDefinition(@Nonnull BaseElement uiDefinition,
                                                             @Nonnull ProcessNodeEntity origin) {
        if (!(uiDefinition instanceof LayoutElement<?> layout) || layout.getChildren().isEmpty()) {
            return;
        }

        this.addReusableUiDefinition(
                StringUtils.isNotNullOrEmpty(uiDefinition.getName()) ? uiDefinition.getName() : FALLBACK_UI_DEFINITION_LABEL,
                null,
                uiDefinition,
                origin,
                ReusableUiDefinitionKind.UiDefinition
        );
    }

    @Nullable
    private static ReusableUiDefinitionKind resolveReusableUiDefinitionKind(@Nonnull List<BaseElement> parents) {
        if (parents.isEmpty()) {
            return null;
        }

        return switch (parents.getLast()) {
            case FormLayoutElement ignored -> ReusableUiDefinitionKind.FormSection;
            case StepperLayoutElement ignored -> ReusableUiDefinitionKind.StepperSection;
            case TabLayoutElement ignored -> ReusableUiDefinitionKind.Tab;
            default -> null;
        };
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

        if (sourceKey == null) {
            return null;
        }

        var dataKey = sourceKey.replace('.', '_');
        return dataKey.length() > MAX_ATTACHMENT_SET_DATA_KEY_LENGTH ? null : dataKey;
    }

    public record ReusableUiDefinition(
            @Nonnull
            String label,
            @Nullable
            String subLabel,
            @Nonnull
            BaseElement uiDefinition,
            @Nonnull
            ProcessNodeEntity origin,
            @Nonnull
            ReusableUiDefinitionKind kind
    ) {
        public ReusableUiDefinition(@Nonnull String label,
                                    @Nullable String subLabel,
                                    @Nonnull BaseElement uiDefinition,
                                    @Nonnull ProcessNodeEntity origin) {
            this(label, subLabel, uiDefinition, origin, ReusableUiDefinitionKind.UiDefinition);
        }
    }

    public enum ReusableUiDefinitionKind {
        CompleteForm,
        FormSection,
        UiDefinition,
        StepperSection,
        Tab
    }

    public record ForwardedAttachmentSet(
            @Nonnull
            String dataKey,
            @Nonnull
            String label,
            @Nullable
            String subLabel,
            boolean isMultifile,
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
            ProcessNodeEntity origin,
            @Nullable
            List<ValueOption> valueOptions,
            // Set instead of valueOptions when the options come from a code list. Clients load the options themselves
            // and use codeListValueMode to determine which item field the process data contains.
            @Nullable
            String codeListKey,
            @Nullable
            CodeListValueMode codeListValueMode
    ) {
        public ForwardedProcessDataKey(@Nonnull String processDataKey,
                                       @Nonnull String label,
                                       @Nullable String subLabel,
                                       @Nonnull ProcessNodeEntity origin) {
            this(processDataKey, label, subLabel, origin, null, null, null);
        }

        public record ValueOption(
                @Nonnull
                String value,
                @Nonnull
                String label
        ) {
        }

        public enum CodeListValueMode {
            // The process data contains the item value; the item label is used for display.
            ValueAndLabel,
            // The process data contains the item label itself.
            LabelOnly
        }
    }

    public record ForwardedIdentity(
            @Nonnull
            String identityId,
            @Nonnull
            String label,
            @Nullable
            String subLabel,
            @Nonnull
            List<UUID> identityProviderKeys,
            @Nonnull
            ProcessNodeEntity origin
    ) {
    }
}
