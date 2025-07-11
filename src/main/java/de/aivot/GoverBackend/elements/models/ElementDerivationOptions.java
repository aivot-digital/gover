package de.aivot.GoverBackend.elements.models;

import jakarta.annotation.Nonnull;

import java.util.LinkedList;
import java.util.List;

public class ElementDerivationOptions {
    public static final String ALL_ELEMENTS = "ALL";

    private List<String> skipErrorsForElementIds = new LinkedList<>();
    private List<String> skipVisibilitiesForElementIds = new LinkedList<>();
    private List<String> skipOverridesForElementIds = new LinkedList<>();
    private List<String> skipValuesForElementIds = new LinkedList<>();

    // region copy

    public ElementDerivationOptions copyForUseInChild(String parentId) {
        ElementDerivationOptions copy = new ElementDerivationOptions();

        if (containsSkipErrors(parentId)) {
            copy.skipErrorsForElementIds.add(ALL_ELEMENTS);
        } else {
            copy.skipErrorsForElementIds.addAll(skipErrorsForElementIds);
        }

        if (containsSkipVisibilities(parentId)) {
            copy.skipVisibilitiesForElementIds.add(ALL_ELEMENTS);
        } else {
            copy.skipVisibilitiesForElementIds.addAll(skipVisibilitiesForElementIds);
        }

        if (containsSkipOverrides(parentId)) {
            copy.skipOverridesForElementIds.add(ALL_ELEMENTS);
        } else {
            copy.skipOverridesForElementIds.addAll(skipOverridesForElementIds);
        }

        if (containsSkipValues(parentId)) {
            copy.skipValuesForElementIds.add(ALL_ELEMENTS);
        } else {
            copy.skipValuesForElementIds.addAll(skipValuesForElementIds);
        }

        return copy;
    }

    // endregion

    // region Helpers

    public boolean contains(@Nonnull String id) {
        return containsSkipErrors(id) ||
               containsSkipVisibilities(id) ||
               containsSkipOverrides(id) ||
               containsSkipValues(id);
    }

    public boolean containsSkipErrors(@Nonnull String id) {
        return skipErrorsForElementIds.contains(id) ||
               skipErrorsForElementIds.contains(ALL_ELEMENTS);
    }

    public boolean notContainsSkipErrors(@Nonnull String id) {
        return !containsSkipErrors(id);
    }

    public boolean containsSkipVisibilities(@Nonnull String id) {
        return skipVisibilitiesForElementIds.contains(id) ||
               skipVisibilitiesForElementIds.contains(ALL_ELEMENTS);
    }

    public boolean notContainsSkipVisibilities(@Nonnull String id) {
        return !containsSkipVisibilities(id);
    }

    public boolean containsSkipOverrides(@Nonnull String id) {
        return skipOverridesForElementIds.contains(id) ||
               skipOverridesForElementIds.contains(ALL_ELEMENTS);
    }

    public boolean notContainsSkipOverrides(@Nonnull String id) {
        return !containsSkipOverrides(id);
    }

    public boolean containsSkipValues(@Nonnull String id) {
        return skipValuesForElementIds.contains(id) ||
               skipValuesForElementIds.contains(ALL_ELEMENTS);
    }

    public boolean notContainsSkipValues(@Nonnull String id) {
        return !containsSkipValues(id);
    }

    // endregion

    public List<String> getSkipErrorsForElementIds() {
        return skipErrorsForElementIds;
    }

    public ElementDerivationOptions setSkipErrorsForElementIds(List<String> skipErrorsForElementIds) {
        this.skipErrorsForElementIds = skipErrorsForElementIds;
        return this;
    }

    public List<String> getSkipVisibilitiesForElementIds() {
        return skipVisibilitiesForElementIds;
    }

    public ElementDerivationOptions setSkipVisibilitiesForElementIds(List<String> skipVisibilitiesForElementIds) {
        this.skipVisibilitiesForElementIds = skipVisibilitiesForElementIds;
        return this;
    }

    public List<String> getSkipOverridesForElementIds() {
        return skipOverridesForElementIds;
    }

    public ElementDerivationOptions setSkipOverridesForElementIds(List<String> skipOverridesForElementIds) {
        this.skipOverridesForElementIds = skipOverridesForElementIds;
        return this;
    }

    public List<String> getSkipValuesForElementIds() {
        return skipValuesForElementIds;
    }

    public ElementDerivationOptions setSkipValuesForElementIds(List<String> skipValuesForElementIds) {
        this.skipValuesForElementIds = skipValuesForElementIds;
        return this;
    }
}
