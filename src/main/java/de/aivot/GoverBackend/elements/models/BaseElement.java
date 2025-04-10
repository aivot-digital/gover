package de.aivot.GoverBackend.elements.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.enums.ElementApprovalStatus;
import de.aivot.GoverBackend.elements.utils.ElementReferenceUtils;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.models.functions.Function;
import de.aivot.GoverBackend.models.functions.FunctionCode;
import de.aivot.GoverBackend.models.functions.FunctionNoCode;
import de.aivot.GoverBackend.models.lib.TestProtocolSet;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.utils.MapUtils;
import de.aivot.GoverBackend.utils.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public abstract class BaseElement {
    private ElementType type;
    private String id;
    private String appVersion;
    private String name;

    private TestProtocolSet testProtocolSet;

    /**
     * @deprecated
     */
    private Function isVisible;
    /**
     * @deprecated
     */
    private FunctionCode patchElement;

    private NoCodeExpression visibilityExpression;
    private JavascriptCode visibilityCode;

    private NoCodeExpression overrideExpression;
    private JavascriptCode overrideCode;

    private ElementMetadata metadata;

    public BaseElement(Map<String, Object> values) {
        type = MapUtils.getEnum(values, "type", Integer.class, ElementType.class, ElementType.values(), ElementType.Group);
        id = MapUtils.getString(values, "id", "missing_id");
        appVersion = MapUtils.getString(values, "appVersion", "0.0.0");
        name = MapUtils.getString(values, "name", "");

        testProtocolSet = MapUtils.getApply(values, "testProtocolSet", Map.class, TestProtocolSet::new);

        isVisible = MapUtils.getApply(values, "isVisible", Map.class, d -> {
            boolean mainFunctionExists = MapUtils.getString(d, "code") != null;
            return mainFunctionExists ? new FunctionCode(d) : new FunctionNoCode(d);
        });
        patchElement = MapUtils.getApply(values, "patchElement", Map.class, FunctionCode::new);

        visibilityCode = JavascriptCode.from(values.get("visibilityCode"));
        visibilityExpression = MapUtils.getApply(values, "visibilityExpression", Map.class, NoCodeExpression::new);

        overrideCode = JavascriptCode.from(values.get("overrideCode"));
        overrideExpression = MapUtils.getApply(values, "overrideExpression", Map.class, NoCodeExpression::new);

        metadata = MapUtils.getApply(values, "metadata", Map.class, ElementMetadata::new);

        applyValues(values);
    }

    public Optional<? extends BaseElement> findChild(@Nonnull String childId) {
        return Optional.empty();
    }

    public abstract void applyValues(Map<String, Object> values);

    public List<BasePdfRowDto> toPdfRows(RootElement root, String idPrefix, FormState formState) {
        return new LinkedList<>();
    }

    public String getResolvedId(@Nullable String idPrefix) {
        return idPrefix != null ? idPrefix + "_" + id : id;
    }

    public boolean matches(String id) {
        return this.id.equals(id);
    }

    public boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        return false;
    }

    public Set<String> getVisibilityReferencedIds() {
        return ElementReferenceUtils
                .getReferencedIds(
                        visibilityCode,
                        visibilityExpression,
                        isVisible
                );
    }

    public Set<String> getOverrideReferencedIds() {
        return ElementReferenceUtils
                .getReferencedIds(
                        overrideCode,
                        overrideExpression,
                        patchElement
                );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseElement that = (BaseElement) o;

        if (type != that.type) return false;
        if (!Objects.equals(id, that.id)) return false;
        if (!Objects.equals(appVersion, that.appVersion)) return false;
        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(testProtocolSet, that.testProtocolSet))
            return false;
        if (!Objects.equals(isVisible, that.isVisible)) return false;
        if (!Objects.equals(patchElement, that.patchElement)) return false;
        if (!Objects.equals(visibilityExpression, that.visibilityExpression)) return false;
        if (!Objects.equals(visibilityCode, that.visibilityCode)) return false;
        if (!Objects.equals(overrideExpression, that.overrideExpression)) return false;
        if (!Objects.equals(overrideCode, that.overrideCode)) return false;
        return Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (appVersion != null ? appVersion.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (testProtocolSet != null ? testProtocolSet.hashCode() : 0);
        result = 31 * result + (isVisible != null ? isVisible.hashCode() : 0);
        result = 31 * result + (patchElement != null ? patchElement.hashCode() : 0);
        result = 31 * result + (visibilityExpression != null ? visibilityExpression.hashCode() : 0);
        result = 31 * result + (visibilityCode != null ? visibilityCode.hashCode() : 0);
        result = 31 * result + (overrideExpression != null ? overrideExpression.hashCode() : 0);
        result = 31 * result + (overrideCode != null ? overrideCode.hashCode() : 0);
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        return result;
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
        if (visibilityCode != null && visibilityCode.isNotEmpty()) {
            return true;
        }

        if (visibilityExpression != null && visibilityExpression.isNotEmpty()) {
            return true;
        }

        if (isVisible != null && isVisible instanceof FunctionCode fIsVisible && StringUtils.isNotNullOrEmpty(fIsVisible.getCode())) {
            return true;
        }

        if (isVisible != null && isVisible instanceof FunctionNoCode nIsVisible && nIsVisible.getConditionSet() != null) {
            return true;
        }

        if (overrideCode != null && overrideCode.isNotEmpty()) {
            return true;
        }

        if (overrideExpression != null && overrideExpression.isNotEmpty()) {
            return true;
        }

        if (patchElement != null && StringUtils.isNotNullOrEmpty(patchElement.getCode())) {
            return true;
        }

        return false;
    }

// region Getters & Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ElementType getType() {
        return type;
    }

    public void setType(ElementType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TestProtocolSet getTestProtocolSet() {
        return testProtocolSet;
    }

    public void setTestProtocolSet(TestProtocolSet testProtocolSet) {
        this.testProtocolSet = testProtocolSet;
    }

    public Function getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(Function isVisible) {
        this.isVisible = isVisible;
    }

    public FunctionCode getPatchElement() {
        return patchElement;
    }

    public void setPatchElement(FunctionCode patchElement) {
        this.patchElement = patchElement;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public ElementMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ElementMetadata metadata) {
        this.metadata = metadata;
    }

    public NoCodeExpression getVisibilityExpression() {
        return visibilityExpression;
    }

    public void setVisibilityExpression(NoCodeExpression visibilityExpression) {
        this.visibilityExpression = visibilityExpression;
    }

    public JavascriptCode getVisibilityCode() {
        return visibilityCode;
    }

    public void setVisibilityCode(JavascriptCode visibilityCode) {
        this.visibilityCode = visibilityCode;
    }

    public NoCodeExpression getOverrideExpression() {
        return overrideExpression;
    }

    public void setOverrideExpression(NoCodeExpression overrideExpression) {
        this.overrideExpression = overrideExpression;
    }

    public JavascriptCode getOverrideCode() {
        return overrideCode;
    }

    public void setOverrideCode(JavascriptCode overrideCode) {
        this.overrideCode = overrideCode;
    }

    // endregion
}
