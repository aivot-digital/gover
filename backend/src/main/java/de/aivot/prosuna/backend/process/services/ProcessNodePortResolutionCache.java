package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodePortResolution;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.security.MessageDigest;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Reuses authoring port resolutions, so loading the nodes of a process does not derive every node configuration again.
 *
 * <p>Providers may only base their ports and deadline phases on literal configuration values. A resolution therefore
 * stays valid while the provider version, the process version, the authored configuration and the deadline settings
 * are unchanged. Provider code only changes with a deployment, which also clears this in-memory cache.
 */
@Component
public class ProcessNodePortResolutionCache {
    private static final int DEFAULT_MAX_ENTRIES = 2048;
    private final JsonMapper jsonMapper;
    private final Map<String, ProcessNodePortResolution> entries;

    public ProcessNodePortResolutionCache(@Nonnull JsonMapper jsonMapper) {
        this(jsonMapper, DEFAULT_MAX_ENTRIES);
    }

    ProcessNodePortResolutionCache(@Nonnull JsonMapper jsonMapper, int maxEntries) {
        this.jsonMapper = jsonMapper;
        this.entries = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ProcessNodePortResolution> eldest) {
                return size() > maxEntries;
            }
        });
    }

    @Nonnull
    public ProcessNodePortResolution resolve(@Nonnull ProcessNodeEntity node,
                                             @Nonnull Supplier<ProcessNodePortResolution> resolver) {
        var key = keyOf(node);
        if (key == null) {
            return resolver.get();
        }
        var cached = entries.get(key);
        if (cached != null) {
            return cached;
        }
        var resolution = resolver.get();
        // Problems can be transient, e.g. when a configuration layout could not be built, so only successes are kept.
        if (resolution.problems().isEmpty()) {
            entries.put(key, resolution);
        }
        return resolution;
    }

    @Nullable
    private String keyOf(@Nonnull ProcessNodeEntity node) {
        try {
            var inputs = List.of(
                    String.valueOf(node.getProcessNodeDefinitionKey()),
                    String.valueOf(node.getProcessNodeDefinitionVersion()),
                    String.valueOf(node.getProcessId()),
                    String.valueOf(node.getProcessVersion()),
                    jsonMapper.writeValueAsString(node.getConfiguration()),
                    jsonMapper.writeValueAsString(node.getDeadlines())
            );
            var digest = MessageDigest.getInstance("SHA-256").digest(jsonMapper.writeValueAsBytes(inputs));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            // Inputs that cannot be fingerprinted are resolved without the cache.
            return null;
        }
    }
}
