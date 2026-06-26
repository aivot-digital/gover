package de.aivot.GoverBackend.plugin.services;

public class PluginUtils {
    public static String combineComponentKey(String parentPluginKey, String componentKey) {
        return String.format("%s.%s", parentPluginKey, componentKey);
    }
}
