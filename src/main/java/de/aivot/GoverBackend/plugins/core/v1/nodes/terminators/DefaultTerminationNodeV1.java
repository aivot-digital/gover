package de.aivot.GoverBackend.plugins.core.v1.nodes.terminators;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugins.core.CorePlugin;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultInstanceCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.utils.ApplicationTimeZone;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Component
public class DefaultTerminationNodeV1 implements ProcessNodeDefinition<DefaultTerminationNodeV1.DefaultTerminationNodeV1Config> {
    private static final String NODE_KEY = "default-termination";

    private static final String RETENTION_VALUE_FIELD_KEY = "retention_value";
    private static final String RETENTION_UNIT_FIELD_KEY = "retention_unit";

    private static final String RETENTION_UNIT_DAYS = "days";
    private static final String RETENTION_UNIT_WEEKS = "weeks";
    private static final String RETENTION_UNIT_MONTHS = "months";
    private static final String RETENTION_UNIT_YEARS = "years";

    private static final Number DEFAULT_RETENTION_VALUE = 30;
    private static final String DEFAULT_RETENTION_UNIT = RETENTION_UNIT_DAYS;

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return NODE_KEY;
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Termination;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Vorgang beenden";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Beendet die Ausführung eines Vorgangs und berechnet/startet die Aufbewahrungsfrist.";
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of();
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(DefaultTerminationNodeV1Config.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts für den DefaultTerminationNodeV1: %s", e.getMessage());
        }

        layout
                .findChild(RETENTION_UNIT_FIELD_KEY, SelectInputElement.class)
                .ifPresent(retentionUnitInput -> {
                    retentionUnitInput
                            .setOptions(List.of(
                                    SelectInputElementOption.of(RETENTION_UNIT_DAYS, "Tage"),
                                    SelectInputElementOption.of(RETENTION_UNIT_WEEKS, "Wochen"),
                                    SelectInputElementOption.of(RETENTION_UNIT_MONTHS, "Monate"),
                                    SelectInputElementOption.of(RETENTION_UNIT_YEARS, "Jahre")
                            ));
                });

        return layout;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<DefaultTerminationNodeV1Config> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();

        var retentionTimeValue = configuration.retentionValue.longValue();
        var retentionTimeUnit = configuration.retentionUnit;

        // Apply retention periods in local business time before storing the resulting absolute instant.
        var retentionTime = ZonedDateTime.now(ApplicationTimeZone.getZoneId());
        switch (retentionTimeUnit) {
            case RETENTION_UNIT_DAYS -> retentionTime = retentionTime.plusDays(retentionTimeValue);
            case RETENTION_UNIT_WEEKS -> retentionTime = retentionTime.plusWeeks(retentionTimeValue);
            case RETENTION_UNIT_MONTHS -> retentionTime = retentionTime.plusMonths(retentionTimeValue);
            case RETENTION_UNIT_YEARS -> retentionTime = retentionTime.plusYears(retentionTimeValue);
        }

        return new ProcessNodeExecutionResultInstanceCompleted()
                .setRetentionDate(retentionTime.toInstant());
    }

    @Nonnull
    @Override
    public Class<DefaultTerminationNodeV1Config> getNodeConfigurationClass() {
        return DefaultTerminationNodeV1Config.class;
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class DefaultTerminationNodeV1Config {
        @InputElementPOJOBinding(id = RETENTION_VALUE_FIELD_KEY, type = ElementType.Number, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Aufbewahrungsfrist"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie die Aufbewahrungsfrist für die Vorgangsdaten nach Abschluss des Vorgangs an (z.B. '30 Tage', '6 Monate', '1 Jahr')."),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 8.0),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "decimalPlaces", intValue = 0)
        })
        public Number retentionValue;

        @InputElementPOJOBinding(id = RETENTION_UNIT_FIELD_KEY, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Einheit der Aufbewahrungsfrist"),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 4.0),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String retentionUnit;
    }
}
