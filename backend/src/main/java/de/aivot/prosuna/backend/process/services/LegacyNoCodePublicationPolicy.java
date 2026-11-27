package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.system.properties.BuildProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Publication-only boundary. Never use this policy for loading, importing, saving or executing existing versions.
 */
@Service
public class LegacyNoCodePublicationPolicy {
    private static final Pattern RELEASE = Pattern.compile("^v?(\\d+)\\.(\\d+)(?:\\.\\d+)?(?:[-+].*)?$");
    private final BuildProperties buildProperties;
    private final JsonMapper jsonMapper;

    public LegacyNoCodePublicationPolicy(@Nonnull BuildProperties buildProperties, @Nonnull JsonMapper jsonMapper) {
        this.buildProperties = buildProperties;
        this.jsonMapper = jsonMapper;
    }

    public boolean isEnforced() {
        var version = buildProperties.getBuildVersion();
        // Unversioned local builds retain the 5.0 transition behavior; release images inject build.buildVersion.
        if (version == null) return false;
        var match = RELEASE.matcher(version);
        if (!match.matches()) return false;
        var major = new BigInteger(match.group(1));
        var minor = new BigInteger(match.group(2));
        return major.compareTo(BigInteger.valueOf(5)) > 0 ||
                major.equals(BigInteger.valueOf(5)) && minor.compareTo(BigInteger.ONE) >= 0;
    }

    @Nonnull
    public List<String> findProblems(@Nonnull ProcessNodeEntity node) {
        var locations = new ArrayList<String>();
        find(jsonMapper.convertValue(node.getConfiguration(), Object.class), "Konfiguration", locations);
        return locations.stream().map(location -> "Prozesselement „" + node.getName() + "“ (ID " + node.getId() + "): " + location +
                " verwendet veraltete No-Code-Regeln. Stellen Sie diese vor der Veröffentlichung auf den neuen No-Code um.").toList();
    }

    public void requirePublishable(@Nonnull ProcessVersionEntity version, @Nonnull List<ProcessNodeEntity> nodes) throws ResponseException {
        if (!isEnforced()) return;
        var problems = nodes.stream().flatMap(node -> findProblems(node).stream()).toList();
        if (!problems.isEmpty()) throw ResponseException.notAcceptable(
                "Prozess %s, Version %s kann ab Prosuna 5.1 mit veralteten No-Code-Regeln nicht veröffentlicht werden. %s",
                version.getProcessId(), version.getProcessVersion(), String.join(" ", problems));
    }

    private static void find(@Nullable Object value, @Nonnull String path, @Nonnull List<String> result) {
        if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) find(list.get(i), path + "[" + i + "]", result);
        } else if (value instanceof Map<?, ?> map) {
            boolean definition = map.get("id") instanceof String && map.get("type") instanceof Number;
            if (definition) {
                var label = "Element „" + (map.get("label") instanceof String title && !title.isBlank() ? title : map.get("id")) + "“ (ID " + map.get("id") + ")";
                for (var section : List.of("visibility", "validation")) {
                    if (map.get(section) instanceof Map<?, ?> function && function.get("conditionSet") instanceof Map<?, ?>) {
                        result.add(label + " · " + (section.equals("visibility") ? "Sichtbarkeit" : "Validierung") + " · " + path + "." + section + ".conditionSet");
                    }
                }
            }
            // Function-input values have their own tagged contract; ordinary customer objects with a similarly named
            // property are not rules. Implicit visibility/validation types are detected via their containing element.
            if ("ConditionSet".equals(map.get("type")) && map.get("conditionSet") instanceof Map<?, ?> &&
                    !path.endsWith(".visibility") && !path.endsWith(".validation")) result.add(path + ".conditionSet");
            for (var entry : map.entrySet()) find(entry.getValue(), path + "." + entry.getKey(), result);
        }
    }
}
