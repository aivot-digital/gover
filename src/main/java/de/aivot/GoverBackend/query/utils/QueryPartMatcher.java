package de.aivot.GoverBackend.query.utils;

import java.util.regex.Pattern;

public class QueryPartMatcher {
    private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Pattern VALUE_PATTERN = Pattern.compile("^[a-zA-Z0-9-_,.]*$");

    public static boolean checkColumnName(String columnName) {
        return COLUMN_NAME_PATTERN.matcher(columnName).matches();
    }

    public static boolean checkTableName(String tableName) {
        return TABLE_NAME_PATTERN.matcher(tableName).matches();
    }

    public static boolean checkValue(String value) {
        return VALUE_PATTERN.matcher(value).matches();
    }
}
