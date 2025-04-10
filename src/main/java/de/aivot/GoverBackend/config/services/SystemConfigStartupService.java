package de.aivot.GoverBackend.config.services;

import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.config.repositories.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;

@Component
public class SystemConfigStartupService implements ApplicationListener<ApplicationReadyEvent> {
    private final Logger logger = LoggerFactory.getLogger(SystemConfigStartupService.class);

    private final SystemConfigRepository systemConfigRepository;
    private final List<SystemConfigDefinition> systemConfigDefinitions;

    @Autowired
    public SystemConfigStartupService(
            SystemConfigRepository systemConfigRepository,
            List<SystemConfigDefinition> systemConfigDefinitions
    ) {
        this.systemConfigRepository = systemConfigRepository;
        this.systemConfigDefinitions = systemConfigDefinitions;
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        var availableDefinitions = new HashMap<String, SystemConfigDefinition>();

        // Iterate over all existing definitions and collect them in a map
        // Check if there are any conflicting keys
        for (var definition : systemConfigDefinitions) {
            var key = definition.getKey();

            if (availableDefinitions.containsKey(key)) {
                var collidingDefinition = availableDefinitions.get(key);

                var error = String.format(
                        "Conflicting config keys found. The offending key is %s. The definition %s and %s are both using this key",
                        key,
                        definition.getClass().getName(),
                        collidingDefinition.getClass().getName()
                );

                throw new IllegalStateException(error);
            }

            availableDefinitions.put(key, definition);
        }

        // Iterate over all available definitions and update the database if the public config flag has changed
        for (var entry : availableDefinitions.entrySet()) {
            var key = entry.getKey();
            var definition = entry.getValue();

            var configEntity = systemConfigRepository.findById(key);
            if (configEntity.isPresent()) {
                var config = configEntity.get();
                if (config.getPublicConfig() != definition.isPublicConfig()) {
                    logger
                            .atWarn()
                            .setMessage("Updating system config because the public config flag has changed")
                            .addKeyValue("key", config.getKey())
                            .addKeyValue("previous", config.getPublicConfig())
                            .addKeyValue("new", definition.isPublicConfig())
                            .log();

                    config.setPublicConfig(definition.isPublicConfig());
                    systemConfigRepository.save(config);
                }
            }
        }

        // Delete all system config entities which are in the database but not in the definitions
        systemConfigRepository
                .findAll()
                .stream()
                .filter(def -> !availableDefinitions.containsKey(def.getKey()))
                .forEach(config -> {
                    logger
                            .atWarn()
                            .setMessage("Deleting system config " + config.getKey() + " because it is not present in the system config definitions")
                            .addKeyValue("key", config.getKey())
                            .addKeyValue("value", config.getValue())
                            .log();

                    systemConfigRepository.delete(config);
                });
    }
}
