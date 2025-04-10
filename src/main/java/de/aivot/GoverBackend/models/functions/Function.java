package de.aivot.GoverBackend.models.functions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.form.content.Alert;
import de.aivot.GoverBackend.elements.models.form.content.Headline;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class Function {
    private String requirements;

    protected Function(Map<String, Object> data) {
        requirements = MapUtils.getString(data, "requirements");
    }

    protected Function() {
    }

    public abstract FunctionResult evaluate(
            String idPrefix,
            BaseElement element,
            BaseElementDerivationContext context
    );

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Function function = (Function) o;

        return Objects.equals(requirements, function.requirements);
    }

    @Override
    public int hashCode() {
        return requirements != null ? requirements.hashCode() : 0;
    }

    @JsonIgnore
    public abstract boolean isEmpty();

    @JsonIgnore
    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    @Nonnull
    @JsonIgnore
    public abstract Set<String> getReferencedIds();
}
