package de.aivot.GoverBackend.models.functions;

import de.aivot.GoverBackend.utils.StringUtils;

import java.util.Map;

public class FunctionResult {
    private final Object value;
    private final boolean isNoCodeResult;

    public FunctionResult(Object value, boolean isNoCodeResult) {
        this.value = value;
        this.isNoCodeResult = isNoCodeResult;
    }

    public boolean isNull() {
        return value == null;
    }

    public Object getObjectValue() {
        return value;
    }

    public String getStringValue() {
        if (value == null) {
            return null;
        } else {
            return value instanceof String ? (String) value : null;
        }
    }

    public Boolean getBooleanValue() {
        if (value == null) {
            return false;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return StringUtils.isNotNullOrEmpty((String) value);
        }
        return false;
    }

    public Integer getIntegerValue() {
        if (value == null) {
            return null;
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else if(value instanceof Long) {
            return ((Long) value).intValue();
        } else if(value instanceof Double) {
            return ((Double) value).intValue();
        } else if(value instanceof Float) {
            return ((Float) value).intValue();
        }

        return  null;
    }

    public Double getDoubleValue() {
        if (value == null) {
            return null;
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if(value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if(value instanceof Double) {
            return (Double) value;
        } else if(value instanceof Float) {
            return ((Float) value).doubleValue();
        }

        return  null;
    }

    public Map<String, Object> getJsonValue() {
        if (value == null) {
            return null;
        } else if (value instanceof Map) {
            return ((Map<String, Object>) value);
        }
        return  null;
    }

    public boolean isNoCodeResult() {
        return isNoCodeResult;
    }
}
