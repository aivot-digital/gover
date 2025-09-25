package de.aivot.GoverBackend.xdf.v2.models;

public class XdfPraezisierung {
    private Integer minLength;
    private Integer maxLength;
    private String pattern;
    private Integer minValue;
    private Integer maxValue;

    public Integer getMinLength() {
        return minLength;
    }
    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }
    public Integer getMaxLength() {
        return maxLength;
    }
    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }
    public String getPattern() {
        return pattern;
    }
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    public Integer getMinValue() {
        return minValue;
    }
    public void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }
    public Integer getMaxValue() {
        return maxValue;
    }
    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }
}
