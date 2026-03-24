package de.aivot.GoverBackend;

import de.aivot.GoverBackend.elements.models.*;

import java.util.Map;

public final class TestData {
    private TestData() {
    }

    public static AuthoredElementValues authored(Object... keyValuePairs) {
        var values = new AuthoredElementValues();
        putAll(values, keyValuePairs);
        return values;
    }

    public static EffectiveElementValues effective(Object... keyValuePairs) {
        var values = new EffectiveElementValues();
        putAll(values, keyValuePairs);
        return values;
    }

    public static DerivedRuntimeElementData runtime(Object... keyValuePairs) {
        return new DerivedRuntimeElementData(
                effective(keyValuePairs),
                new ComputedElementStates()
        );
    }

    public static DerivedRuntimeElementData runtime(AuthoredElementValues values) {
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.putAll(values);
        return new DerivedRuntimeElementData(effectiveValues, new ComputedElementStates());
    }

    public static DerivedRuntimeElementData runtime(EffectiveElementValues values) {
        return new DerivedRuntimeElementData(values, new ComputedElementStates());
    }

    public static DerivedRuntimeElementData runtime(EffectiveElementValues values, ComputedElementStates states) {
        return new DerivedRuntimeElementData(values, states);
    }

    public static ComputedElementState state(Boolean visible) {
        return new ComputedElementState().setVisible(visible);
    }

    private static void putAll(Map<String, Object> target, Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Expected an even number of key/value arguments");
        }

        for (var i = 0; i < keyValuePairs.length; i += 2) {
            target.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
    }
}
