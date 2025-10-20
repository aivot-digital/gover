package de.aivot.GoverBackend.elements.models.elements;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.aivot.GoverBackend.elements.enums.ElementApprovalStatus;
import de.aivot.GoverBackend.elements.models.elements.form.content.*;
import de.aivot.GoverBackend.elements.models.elements.form.input.*;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.elements.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.elements.steps.IntroductionStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.StepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.SummaryStepElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.models.lib.TestProtocolSet;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RootElement.class, name = ElementType.ID_Root),
        @JsonSubTypes.Type(value = StepElement.class, name = ElementType.ID_Step),
        @JsonSubTypes.Type(value = Alert.class, name = ElementType.ID_Alert),
        @JsonSubTypes.Type(value = GroupLayout.class, name = ElementType.ID_Group),
        @JsonSubTypes.Type(value = CheckboxField.class, name = ElementType.ID_Checkbox),
        @JsonSubTypes.Type(value = DateField.class, name = ElementType.ID_Date),
        @JsonSubTypes.Type(value = Headline.class, name = ElementType.ID_Headline),
        @JsonSubTypes.Type(value = MultiCheckboxField.class, name = ElementType.ID_MultiCheckbox),
        @JsonSubTypes.Type(value = NumberField.class, name = ElementType.ID_Number),
        @JsonSubTypes.Type(value = ReplicatingContainerLayout.class, name = ElementType.ID_ReplicatingContainer),
        @JsonSubTypes.Type(value = RichText.class, name = ElementType.ID_Richtext),
        @JsonSubTypes.Type(value = RadioField.class, name = ElementType.ID_Radio),
        @JsonSubTypes.Type(value = SelectField.class, name = ElementType.ID_Select),
        @JsonSubTypes.Type(value = Spacer.class, name = ElementType.ID_Spacer),
        @JsonSubTypes.Type(value = TableField.class, name = ElementType.ID_Table),
        @JsonSubTypes.Type(value = TextField.class, name = ElementType.ID_Text),
        @JsonSubTypes.Type(value = TimeField.class, name = ElementType.ID_Time),
        @JsonSubTypes.Type(value = IntroductionStepElement.class, name = ElementType.ID_IntroductionStep),
        @JsonSubTypes.Type(value = SubmitStepElement.class, name = ElementType.ID_SubmitStep),
        @JsonSubTypes.Type(value = SummaryStepElement.class, name = ElementType.ID_SummaryStep),
        @JsonSubTypes.Type(value = Image.class, name = ElementType.ID_Image),
        @JsonSubTypes.Type(value = FileUploadField.class, name = ElementType.ID_FileUpload),
})
public abstract class BaseElement implements Serializable {
    @Nonnull
    private ElementType type;
    @Nonnull
    private String id;
    @Nullable
    private String name;

    @Nullable
    private TestProtocolSet testProtocolSet;

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
        if (visibility != null) {
            visibility.recalculateReferencedIds();
        }

        if (override != null) {
            override.recalculateReferencedIds();
        }
    }

    @JsonIgnore
    public ElementApprovalStatus getApproval() {
        if (testProtocolSet == null) {
            return ElementApprovalStatus.MissingBothApprovals;
        }

        var hasGeneralTest = testProtocolSet.getProfessionalTest() != null && StringUtils.isNotNullOrEmpty(testProtocolSet.getProfessionalTest().getUserId());

        if (testIfTechnicalApprovalNeeded()) {
            var hasTechnicalTest = testProtocolSet.getTechnicalTest() != null && StringUtils.isNotNullOrEmpty(testProtocolSet.getTechnicalTest().getUserId());

            if (hasGeneralTest && hasTechnicalTest) {
                return ElementApprovalStatus.Approved;
            }

            if (!hasGeneralTest && !hasTechnicalTest) {
                return ElementApprovalStatus.MissingBothApprovals;
            }

            if (!hasGeneralTest) {
                return ElementApprovalStatus.MissingGeneralApproval;
            }

            return ElementApprovalStatus.MissingTechnicalApproval;
        } else {
            if (hasGeneralTest) {
                return ElementApprovalStatus.Approved;
            }

            return ElementApprovalStatus.MissingGeneralApproval;
        }
    }

    protected boolean testIfTechnicalApprovalNeeded() {
        if (visibility != null) {
            if (visibility.getJavascriptCode() != null && visibility.getJavascriptCode().isNotEmpty()) {
                return true;
            }

            if (visibility.getNoCode() != null) {
                return true;
            }

            if (visibility.getConditionSet() != null) {
                return true;
            }
        }

        if (override != null) {
            if (override.getJavascriptCode() != null && override.getJavascriptCode().isNotEmpty()) {
                return true;
            }

            if (override.getFieldNoCodeMap() != null) {
                return true;
            }
        }

        return false;
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BaseElement that = (BaseElement) o;
        return type == that.type && id.equals(that.id) && Objects.equals(name, that.name) && Objects.equals(testProtocolSet, that.testProtocolSet) && Objects.equals(visibility, that.visibility) && Objects.equals(override, that.override) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(testProtocolSet);
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
    public TestProtocolSet getTestProtocolSet() {
        return testProtocolSet;
    }

    public BaseElement setTestProtocolSet(@Nullable TestProtocolSet testProtocolSet) {
        this.testProtocolSet = testProtocolSet;
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
