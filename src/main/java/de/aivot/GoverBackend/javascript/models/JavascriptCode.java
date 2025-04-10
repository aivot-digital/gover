package de.aivot.GoverBackend.javascript.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.utils.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Represents a piece of Javascript code.
 * This model should be used to encapsulate javascript code in e.g. form elements.
 * The methods <code>equals</code> and <code>hashCode</code> should always be implemented to allow this class being stored in the database and not always being marked as dirty by hibernate.
 */
public class JavascriptCode {
    private String code;

    // region Utility Constructors

    public static JavascriptCode from(@Nullable Object object) {
        if (object instanceof Map<?, ?> map) {
            if (map.containsKey("code")) {
                return new JavascriptCode()
                        .setCode((String) map.get("code"));
            }
        }

        return null;
    }

    // endregion

    // region Utility Methods

    /**
     * Check if the code is empty.
     * This might occur if the frontend sends an empty string.
     *
     * @return true if the code is empty, false otherwise
     */
    @JsonIgnore
    public boolean isEmpty() {
        return StringUtils.isNullOrEmpty(code);
    }

    /**
     * Check if the code is not empty.
     *
     * @return true if the code is not empty, false otherwise
     */
    @JsonIgnore
    public boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * Get all ids that are referenced in the code.
     *
     * @return a set of all ids that are referenced in the code
     */
    @Nonnull
    @JsonIgnore
    public Set<String> getReferencedIds() {
        if (code == null || StringUtils.isNullOrEmpty(code)) {
            return new HashSet<>();
        }

        var expliciteReferencePattern = Pattern.compile(">>>([a-zA-Z0-9_-]+)");

        var implicitRegex = String.format(
                "(%s\\.)?(%s|%s|%s|%s|%s)\\.([a-zA-Z0-9_-]+)",
                JavascriptEngine.JS_CONTEXT_OBJECT_NAME,
                BaseElementDerivationContext.INPUT_VALUES_JS_CONTEXT_OBJECT_NAME,
                BaseElementDerivationContext.COMPUTED_VALUES_JS_CONTEXT_OBJECT_NAME,
                BaseElementDerivationContext.VISIBILITIES_JS_CONTEXT_OBJECT_NAME,
                BaseElementDerivationContext.ERRORS_JS_CONTEXT_OBJECT_NAME,
                BaseElementDerivationContext.OVERRIDES_JS_CONTEXT_OBJECT_NAME
        );
        var implicitReferencePattern = Pattern.compile(implicitRegex);

        var ids = new HashSet<String>();

        var matcher = expliciteReferencePattern.matcher(code);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }

        matcher = implicitReferencePattern.matcher(code);
        while (matcher.find()) {
            ids.add(matcher.group(3));
        }

        return ids;
    }

    // endregion

    // region Equals & HashCode

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JavascriptCode that = (JavascriptCode) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getCode() {
        return code;
    }

    @Nonnull
    public JavascriptCode setCode(@Nullable String code) {
        this.code = code;
        return this;
    }

    // endregion
}
