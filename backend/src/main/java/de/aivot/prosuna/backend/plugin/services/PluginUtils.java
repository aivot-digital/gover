package de.aivot.prosuna.backend.plugin.services;

public class PluginUtils {
    public static String combineComponentKey(String parentPluginKey, String componentKey) {
        return String.format("%s.%s", parentPluginKey, componentKey);
    }
}
