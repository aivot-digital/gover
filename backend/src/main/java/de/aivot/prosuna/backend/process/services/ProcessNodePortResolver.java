package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.ProcessNodePortResolution;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class ProcessNodePortResolver {
    private static final Logger logger = LoggerFactory.getLogger(ProcessNodePortResolver.class);
    private final Validator validator;

    public ProcessNodePortResolver(Validator validator) {
        this.validator = validator;
    }

    @Nonnull
    public <C> ProcessNodePortResolution resolve(@Nonnull ProcessNodeEntity node,
                                                 @Nonnull ProcessNodeDefinition<C> provider,
                                                 @Nonnull C configuration) {
        try {
            var ports = provider.getPorts(configuration);
            var definitions = provider.getDeadlines(configuration);
            if (ports == null || definitions == null) {
                return failed();
            }
            var problems = new ArrayList<String>();
            var portKeys = new HashSet<String>();
            var deadlineKeys = new HashSet<String>();
            for (var port : ports) {
                validatePort(port, portKeys, problems);
            }
            for (var definition : definitions) {
                if (definition == null) {
                    problems.add("Die Fristen enthalten einen leeren Eintrag.");
                    continue;
                }
                validator.validate(definition).stream()
                        .map(violation -> violation.getMessage()).sorted().forEach(problems::add);
                if (!deadlineKeys.add(definition.key())) {
                    problems.add("Der Fristschlüssel „%s“ wird mehrfach verwendet.".formatted(definition.key()));
                }
                // Reserve every offered expiry key, including soft and unconfigured deadlines.
                validatePort(definition.port(), portKeys, problems);
            }
            if (!problems.isEmpty()) {
                return new ProcessNodePortResolution(List.of(), problems);
            }
            validator.validateProperty(node, "deadlines").stream()
                    .map(violation -> violation.getMessage()).sorted().forEach(problems::add);
            if (node.getDeadlines() != null) {
                node.getDeadlines().values().stream().filter(java.util.Objects::nonNull)
                        .flatMap(setting -> validator.validate(setting).stream())
                        .map(violation -> violation.getMessage()).sorted().forEach(problems::add);
            }
            if (!problems.isEmpty()) {
                return new ProcessNodePortResolution(List.of(), problems, definitions);
            }
            var effectivePorts = new ArrayList<>(ports);
            for (var definition : definitions) {
                var setting = node.getDeadlines().get(definition.key());
                if (setting != null && setting.days() != null && setting.strict()) {
                    var port = definition.port();
                    effectivePorts.add(new ProcessNodePort(port.key(), port.label(), port.description(), definition.key()));
                }
            }
            return new ProcessNodePortResolution(effectivePorts, List.of(), definitions);
        } catch (Exception exception) {
            logger.warn("Could not resolve ports for process node {} ({})", node.getId(), provider.getKey(), exception);
            return failed();
        }
    }

    private void validatePort(@Nullable ProcessNodePort port, @Nonnull HashSet<String> keys, @Nonnull List<String> problems) {
        if (port == null) {
            problems.add("Die Ausgangsports enthalten einen leeren Eintrag.");
            return;
        }
        validator.validate(port).stream()
                .map(violation -> violation.getMessage()).sorted().forEach(problems::add);
        if (!keys.add(port.key())) {
            problems.add("Der Ausgangsschlüssel „%s“ wird mehrfach verwendet.".formatted(port.key()));
        }
        if (port.deadlineKey() != null) {
            problems.add("Die Zuordnung eines Ausgangs zu einer Frist wird automatisch ermittelt.");
        }
    }

    @Nonnull
    public static ProcessNodePortResolution failed() {
        return new ProcessNodePortResolution(List.of(), List.of("Die Ausgänge und Fristen konnten nicht ermittelt werden. Prüfen Sie die Konfiguration des Prozesselements."));
    }
}
