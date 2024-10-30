package de.aivot.GoverBackend.utils;

import java.math.BigDecimal;

public class MathUtils {
    public static double doubleValueBetween(double min, double max, double value) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }

    public static BigDecimal bigDecimalValueBetween(BigDecimal min, BigDecimal max, BigDecimal value) {
        if (value.compareTo(min) < 0) {
            return min;
        } else if (value.compareTo(max) > 0) {
            return max;
        } else {
            return value;
        }
    }

    public static long longValueBetween(long min, long max, long value) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }
}
