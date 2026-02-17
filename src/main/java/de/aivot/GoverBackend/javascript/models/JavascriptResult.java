package de.aivot.GoverBackend.javascript.models;

import org.graalvm.polyglot.Value;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of a Javascript execution.
 * This model should be used to encapsulate the result of a javascript execution and provide a clean interface to process the result.
 */
public class JavascriptResult {
    private final Value value;
    private final String stdOutput;
    private final String errOutput;

    public JavascriptResult(@Nullable Value value, @Nonnull String stdOutput, @Nonnull String errOutput) {
        this.value = value;
        this.stdOutput = stdOutput;
        this.errOutput = errOutput;
    }

    public boolean isNull() {
        return value == null || value.isNull();
    }

    @Nullable
    public Number asNumber() {
        if (value == null || isNull()) {
            return null;
        }
        try {
            return value.asInt();
        } catch (Exception ignored) {
        }
        try {
            return value.asLong();
        } catch (Exception ignored) {
        }
        try {
            return value.asDouble();
        } catch (Exception ignored) {
        }
        try {
            return value.asFloat();
        } catch (Exception ignored) {
        }
        try {
            return value.asShort();
        } catch (Exception ignored) {
        }
        try {
            return value.asByte();
        } catch (Exception ignored) {
        }
        return null;
    }

    @Nullable
    public String asString() {
        if (value == null || isNull()) {
            return null;
        }
        return value.asString();
    }

    @Nullable
    public Boolean asBoolean() {
        if (value == null || isNull()) {
            return null;
        }
        return value.asBoolean();
    }

    @Nullable
    public Object asObject() {
        if (value == null || isNull()) {
            return null;
        }

        if (value.hasArrayElements()) {
            return convertValueToList(value);
        } else if (value.isNumber()) {
            if (Double.isNaN(value.asDouble()) || Double.isInfinite(value.asDouble())) {
                return (double) 0;
            }
        } else if (value.hasMembers()) {
            return convertValueToMap(value);
        }

        return value.as(Object.class);
    }

    @Nullable
    public Map<String, Object> asMap() {
        if (value == null || isNull()) {
            return null;
        }

        return convertValueToMap(value);
    }

    @Nullable
    private static Map<String, Object> convertValueToMap(@Nullable Value val) {
        if (val == null || val.isNull()) {
            return null;
        }

        var result = new HashMap<String, Object>();

        var keys = val.getMemberKeys();
        for (var key : keys) {
            var memberValue = val.getMember(key);

            if (memberValue.isHostObject()) {
                result.put(key, memberValue.asHostObject());
            } else if (memberValue.hasArrayElements()) {
                result.put(key, convertValueToList(memberValue));
            } else if (memberValue.hasMembers()) {
                result.put(key, convertValueToMap(memberValue));
            } else {
                result.put(key, memberValue.as(Object.class));
            }
        }

        return result;
    }

    @Nullable
    private static List<Object> convertValueToList(@Nullable Value val) {
        if (val == null || val.isNull()) {
            return null;
        }

        var result = new LinkedList<>();

        for (var i = 0; i < val.getArraySize(); i++) {
            var itemValue = val.getArrayElement(i);

            if (itemValue.isHostObject()) {
                result.add(itemValue.asHostObject());
            } else if (itemValue.hasArrayElements()) {
                result.add(convertValueToList(itemValue));
            } else if (itemValue.hasMembers()) {
                result.add(convertValueToMap(itemValue));
            } else {
                result.add(itemValue.as(Object.class));
            }
        }

        return result;
    }

    public String getStdOutput() {
        return stdOutput;
    }

    public String getErrOutput() {
        return errOutput;
    }

    public String toString() {
        if (isNull() || value == null) {
            return "null";
        }
        return value.toString();
    }
}
