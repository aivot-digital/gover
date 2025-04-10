package de.aivot.GoverBackend.config.services;

import de.aivot.GoverBackend.config.models.UserConfigDefinition;
import de.aivot.GoverBackend.config.repositories.UserConfigRepository;
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
public class UserConfigStartupService implements ApplicationListener<ApplicationReadyEvent> {
    private final Logger logger = LoggerFactory.getLogger(UserConfigStartupService.class);

    private final UserConfigRepository userConfigRepository;
    private final List<UserConfigDefinition> userConfigDefinitions;

    @Autowired
    public UserConfigStartupService(
            UserConfigRepository userConfigRepository,
            List<UserConfigDefinition> userConfigDefinitions
    ) {
        this.userConfigRepository = userConfigRepository;
        this.userConfigDefinitions = userConfigDefinitions;
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        var availableDefinitions = new HashMap<String, UserConfigDefinition>();

        // Iterate over all existing definitions and collect them in a map
        // Check if there are any conflicting keys
        for (var definition : userConfigDefinitions) {
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

            var configEntities = userConfigRepository
                    .findAllByKey(key);

            for (var config : configEntities) {
                if (config.getPublicConfig() != definition.isPublicConfig()) {
                    logger
                            .atWarn()
                            .setMessage("Updating user config because the public config flag has changed")
                            .addKeyValue("userId", config.getUserId())
                            .addKeyValue("key", config.getKey())
                            .addKeyValue("previous", config.getPublicConfig())
                            .addKeyValue("new", definition.isPublicConfig())
                            .log();

                    config.setPublicConfig(definition.isPublicConfig());
                    userConfigRepository.save(config);
                }
            }
        }

        // Delete all user config entities which are in the database but not in the definitions
        userConfigRepository
                .findAll()
                .stream()
                .filter(def -> !availableDefinitions.containsKey(def.getKey()))
                .forEach(config -> {
                    logger
                            .atWarn()
                            .setMessage("Deleting user config " + config.getKey() + " for user " + config.getUserId() + " because it is not present in the user config definitions")
                            .addKeyValue("key", config.getKey())
                            .addKeyValue("value", config.getValue())
                            .log();

                    userConfigRepository.delete(config);
                });
    }
}
