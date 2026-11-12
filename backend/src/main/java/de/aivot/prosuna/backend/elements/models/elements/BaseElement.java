package de.aivot.prosuna.backend.elements.models.elements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.aivot.prosuna.backend.elements.models.elements.form.content.*;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.elements.models.elements.layout.*;
import de.aivot.prosuna.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.IntroductionStepElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.SummaryStepElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FormLayoutElement.class, name = ElementType.ID_FormLayout),
        @JsonSubTypes.Type(value = GenericStepElement.class, name = ElementType.ID_Step),
        @JsonSubTypes.Type(value = AlertContentElement.class, name = ElementType.ID_Alert),
        @JsonSubTypes.Type(value = GroupLayoutElement.class, name = ElementType.ID_Group),
        @JsonSubTypes.Type(value = CheckboxInputElement.class, name = ElementType.ID_Checkbox),
        @JsonSubTypes.Type(value = DateInputElement.class, name = ElementType.ID_Date),
        @JsonSubTypes.Type(value = HeadlineContentElement.class, name = ElementType.ID_Headline),
        @JsonSubTypes.Type(value = MultiCheckboxInputElement.class, name = ElementType.ID_MultiCheckbox),
        @JsonSubTypes.Type(value = NumberInputElement.class, name = ElementType.ID_Number),
        @JsonSubTypes.Type(value = ReplicatingContainerLayoutElement.class, name = ElementType.ID_ReplicatingContainer),
        @JsonSubTypes.Type(value = RichTextContentElement.class, name = ElementType.ID_RichText),
        @JsonSubTypes.Type(value = RadioInputElement.class, name = ElementType.ID_Radio),
        @JsonSubTypes.Type(value = SelectInputElement.class, name = ElementType.ID_Select),
        @JsonSubTypes.Type(value = SpacerContentElement.class, name = ElementType.ID_Spacer),
        @JsonSubTypes.Type(value = TableInputElement.class, name = ElementType.ID_Table),
        @JsonSubTypes.Type(value = TextInputElement.class, name = ElementType.ID_Text),
        @JsonSubTypes.Type(value = TimeInputElement.class, name = ElementType.ID_Time),
        @JsonSubTypes.Type(value = IntroductionStepElement.class, name = ElementType.ID_IntroductionStep),
        @JsonSubTypes.Type(value = SubmitStepElement.class, name = ElementType.ID_SubmitStep),
        @JsonSubTypes.Type(value = SummaryStepElement.class, name = ElementType.ID_SummaryStep),
        @JsonSubTypes.Type(value = ImageContentElement.class, name = ElementType.ID_Image),
        @JsonSubTypes.Type(value = FileUploadInputElement.class, name = ElementType.ID_FileUpload),
        @JsonSubTypes.Type(value = DialogLayoutElement.class, name = ElementType.ID_DialogLayout),
        @JsonSubTypes.Type(value = StepperLayoutElement.class, name = ElementType.ID_StepperLayout),
        @JsonSubTypes.Type(value = ConfigLayoutElement.class, name = ElementType.ID_ConfigLayout),
        @JsonSubTypes.Type(value = FunctionInputElement.class, name = ElementType.ID_FunctionInput),
        @JsonSubTypes.Type(value = CodeInputElement.class, name = ElementType.ID_CodeInput),
        @JsonSubTypes.Type(value = RichTextInputElement.class, name = ElementType.ID_RichTextInput),
        @JsonSubTypes.Type(value = UiDefinitionInputElement.class, name = ElementType.ID_UiDefinitionInput),
        @JsonSubTypes.Type(value = IdentityConfigElement.class, name = ElementType.ID_IdentityInput),
        @JsonSubTypes.Type(value = TabLayoutElement.class, name = ElementType.ID_TabLayout),
        @JsonSubTypes.Type(value = ChipInputElement.class, name = ElementType.ID_ChipInput),
        @JsonSubTypes.Type(value = DateTimeInputElement.class, name = ElementType.ID_DateTime),
        @JsonSubTypes.Type(value = DateRangeInputElement.class, name = ElementType.ID_DateRange),
        @JsonSubTypes.Type(value = TimeRangeInputElement.class, name = ElementType.ID_TimeRange),
        @JsonSubTypes.Type(value = DateTimeRangeInputElement.class, name = ElementType.ID_DateTimeRange),
        @JsonSubTypes.Type(value = MapPointInputElement.class, name = ElementType.ID_MapPoint),
        @JsonSubTypes.Type(value = DomainAndUserSelectInputElement.class, name = ElementType.ID_DomainAndUserSelect),
        @JsonSubTypes.Type(value = AssignmentContextInputElement.class, name = ElementType.ID_AssignmentContext),
        @JsonSubTypes.Type(value = DataModelSelectInputElement.class, name = ElementType.ID_DataModelSelect),
        @JsonSubTypes.Type(value = DataObjectSelectInputElement.class, name = ElementType.ID_DataObjectSelect),
        @JsonSubTypes.Type(value = NoCodeInputElement.class, name = ElementType.ID_NoCodeInput),
        @JsonSubTypes.Type(value = SummaryLayoutElement.class, name = ElementType.ID_SummaryLayout),
        @JsonSubTypes.Type(value = ProcessDataKeyInputElement.class, name = ElementType.ID_ProcessDataKeyInput),
        @JsonSubTypes.Type(value = ProcessAttachmentDisplayContentElement.class, name = ElementType.ID_ProcessAttachmentDisplay),
        @JsonSubTypes.Type(value = ProcessInstanceAttachmentSetSelectElement.class, name = ElementType.ID_ProcessInstanceAttachmentSetSelect),
        @JsonSubTypes.Type(value = ProcessIdentityIdInputElement.class, name = ElementType.ID_ProcessIdentityIdInput),
        @JsonSubTypes.Type(value = SecretSelectInputElement.class, name = ElementType.ID_SecretSelectInput),
        @JsonSubTypes.Type(value = AssetSelectInputElement.class, name = ElementType.ID_AssetSelectInput),
        @JsonSubTypes.Type(value = DepartmentSelectInputElement.class, name = ElementType.ID_DepartmentSelectInput),
        @JsonSubTypes.Type(value = HtmlTemplateInputElement.class, name = ElementType.ID_HtmlTemplateInput),
        @JsonSubTypes.Type(value = StoragePathSelectorInputElement.class, name = ElementType.ID_StoragePathSelector),
        @JsonSubTypes.Type(value = PaymentConfigElement.class, name = ElementType.ID_PaymentConfig),
        @JsonSubTypes.Type(value = LinkButtonContentElement.class, name = ElementType.ID_LinkButton)
})
public abstract class BaseElement implements Serializable {
    @Nonnull
    private ElementType type;
    @Nonnull
    private String id;
    @Nullable
    private String name;

    @Nullable
    private ElementVisibilityFunctions visibility;
    @Nullable
    private ElementOverrideFunctions override;

    @Nullable
    private ElementMetadata metadata;

    public BaseElement(@Nonnull ElementType type) {
        this.type = type;
        this.id = UUID.randomUUID().toString();
    }

    public void recalculateReferencedIds() {
        recalculateReferencedIds(Map.of());
    }

    public void recalculateReferencedIds(@Nonnull Map<String, ? extends Collection<String>> destinationKeyIndex) {
        if (visibility != null) {
            visibility.recalculateReferencedIds(destinationKeyIndex);
        }

        if (override != null) {
            override.recalculateReferencedIds(destinationKeyIndex);
        }
    }

    /**
     * Validates the authored configuration of this element without its children. Subclasses extend the result of
     * {@code super}. Checks must stay static: element trees are validated as submitted by clients, so element functions
     * must not be executed and no resources may be loaded here.
     * <p>
     * Deliberately not named like a getter, so Jackson does not serialize the errors into the element.
     *
     * @return Error messages keyed by the name of the erroneous property. Empty if the configuration is valid.
     */
    @Nonnull
    public Map<String, List<String>> validateConfiguration() {
        return new LinkedHashMap<>();
    }

    protected static void addConfigurationError(@Nonnull Map<String, List<String>> errors,
                                                @Nonnull String property,
                                                @Nonnull String message) {
        errors
                .computeIfAbsent(property, ignored -> new LinkedList<>())
                .add(message);
    }

    /**
     * Reports a minimum that exceeds its maximum at both properties. Like the runtime validation, a maximum of 0 or
     * less means no limit.
     */
    protected static void addMinimumExceedsMaximumError(@Nonnull Map<String, List<String>> errors,
                                                        @Nonnull String minimumProperty,
                                                        @Nullable Integer minimum,
                                                        @Nonnull String maximumProperty,
                                                        @Nullable Integer maximum,
                                                        @Nonnull String message) {
        if (minimum != null && maximum != null && maximum > 0 && minimum > maximum) {
            addConfigurationError(errors, minimumProperty, message);
            addConfigurationError(errors, maximumProperty, message);
        }
    }

    /**
     * Reports manually defined options that the runtime validation would reject for every value, or that the editor
     * rejects as ambiguous. All messages are reported at the single property holding the list.
     */
    protected static <O> void addOptionErrors(@Nonnull Map<String, List<String>> errors,
                                              @Nonnull String property,
                                              @Nullable List<O> options,
                                              @Nonnull Function<O, String> getValue,
                                              @Nonnull Function<O, String> getLabel) {
        if (options == null || options.isEmpty()) {
            addConfigurationError(errors, property, "Fügen Sie mindestens eine Option hinzu.");
            return;
        }

        var values = new HashSet<String>();
        var labels = new HashSet<String>();
        var hasEmptyField = false;
        var hasDuplicateValue = false;
        var hasDuplicateLabel = false;
        for (var option : options) {
            var value = option == null ? null : StringUtils.toNullableTrimmedString(getValue.apply(option));
            var label = option == null ? null : StringUtils.toNullableTrimmedString(getLabel.apply(option));
            hasEmptyField |= value == null || label == null;
            hasDuplicateValue |= value != null && !values.add(value);
            hasDuplicateLabel |= label != null && !labels.add(label);
        }

        if (hasEmptyField) {
            addConfigurationError(errors, property, "Geben Sie für jede Option eine Beschriftung und einen Wert an.");
        }
        if (hasDuplicateValue) {
            addConfigurationError(errors, property, "Die Werte der Optionen müssen eindeutig sein.");
        }
        if (hasDuplicateLabel) {
            addConfigurationError(errors, property, "Die Beschriftungen der Optionen müssen eindeutig sein.");
        }
    }

    /**
     * Overrides can replace any property at runtime, so content that is only missing in the authored configuration
     * must not be reported for such elements.
     */
    protected boolean hasOverride() {
        return override != null && override.getType() != null;
    }

    /**
     * Rich text editors keep empty markup such as {@code <p></p>} when all text was removed.
     */
    protected static boolean isRichTextEmpty(@Nullable String html) {
        return html == null || html.replaceAll("<[^>]*>", "").replace("&nbsp;", "").isBlank();
    }

    public void removeInternalInformation() {
        this.name = "";

        if (this.visibility != null) {
            this.visibility = new ElementVisibilityFunctions()
                    .setType(this.visibility.getType())
                    .setReferencedIds(this.visibility.getReferencedIds());
        }

        if (this.override != null) {
            this.override = new ElementOverrideFunctions()
                    .setType(this.override.getType())
                    .setReferencedIds(this.override.getReferencedIds());
        }
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BaseElement that = (BaseElement) o;
        return type == that.type && id.equals(that.id) && Objects.equals(name, that.name) && Objects.equals(visibility, that.visibility) && Objects.equals(override, that.override) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(visibility);
        result = 31 * result + Objects.hashCode(override);
        result = 31 * result + Objects.hashCode(metadata);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nonnull
    public ElementType getType() {
        return type;
    }

    public BaseElement setType(@Nonnull ElementType type) {
        this.type = type;
        return this;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    public BaseElement setId(@Nonnull String id) {
        this.id = id;
        return this;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public BaseElement setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public ElementVisibilityFunctions getVisibility() {
        return visibility;
    }

    public BaseElement setVisibility(@Nullable ElementVisibilityFunctions visibility) {
        this.visibility = visibility;
        return this;
    }

    @Nullable
    public ElementOverrideFunctions getOverride() {
        return override;
    }

    public BaseElement setOverride(@Nullable ElementOverrideFunctions override) {
        this.override = override;
        return this;
    }

    @Nullable
    public ElementMetadata getMetadata() {
        return metadata;
    }

    public BaseElement setMetadata(@Nullable ElementMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    // endregion
}
