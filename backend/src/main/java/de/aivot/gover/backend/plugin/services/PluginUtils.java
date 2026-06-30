package de.aivot.gover.backend.plugin.services;

public class PluginUtils {
    public static String combineComponentKey(String parentPluginKey, String componentKey) {
        return String.format("%s.%s", parentPluginKey, componentKey);
    }
}
