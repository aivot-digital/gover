package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.ProcessNodePortResolution;
import jakarta.annotation.Nonnull;
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

    public static final String DEADLINE_PORT_KEY = "__deadline_exceeded";
    public static final ProcessNodePort DEADLINE_PORT = new ProcessNodePort(
            DEADLINE_PORT_KEY,
            "Fristüberschreitung",
            "Ausgang für die Fortsetzung nach Überschreitung der festgelegten Frist. Die automatische Weiterleitung ist noch nicht verfügbar."
    );

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
            var problems = new ArrayList<String>();
            var keys = new HashSet<String>();
            if (ports == null) {
                return failed(node);
            }
            for (var port : ports) {
                if (port == null) {
                    problems.add("Die Ausgangsports enthalten einen leeren Eintrag.");
                    continue;
                }
                validator.validate(port).stream()
                        .map(violation -> violation.getMessage()).sorted().forEach(problems::add);
                if (!keys.add(port.key())) {
                    problems.add("Der Ausgangsschlüssel „%s“ wird mehrfach verwendet.".formatted(port.key()));
                }
                if (DEADLINE_PORT_KEY.equals(port.key())) {
                    problems.add("Der Ausgangsschlüssel „%s“ ist für Fristüberschreitungen reserviert.".formatted(port.key()));
                }
            }
            return withDeadline(node, problems.isEmpty() ? ports : List.of(), problems);
        } catch (Exception exception) {
            logger.warn("Could not resolve ports for process node {} ({})", node.getId(), provider.getKey(), exception);
            return failed(node);
        }
    }

    @Nonnull
    public static ProcessNodePortResolution failed(@Nonnull ProcessNodeEntity node) {
        return withDeadline(node, List.of(), List.of("Die Ausgangsports konnten nicht ermittelt werden. Prüfen Sie die Konfiguration des Prozesselements."));
    }

    @Nonnull
    private static ProcessNodePortResolution withDeadline(@Nonnull ProcessNodeEntity node,
                                                          @Nonnull List<ProcessNodePort> ports,
                                                          @Nonnull List<String> problems) {
        var effectivePorts = new ArrayList<>(ports);
        if (node.getTimeLimitDays() != null) {
            effectivePorts.add(DEADLINE_PORT);
        }
        return new ProcessNodePortResolution(effectivePorts, problems);
    }
}
