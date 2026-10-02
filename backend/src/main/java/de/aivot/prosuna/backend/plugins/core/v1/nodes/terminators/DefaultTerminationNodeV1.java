package de.aivot.prosuna.backend.plugins.core.v1.nodes.terminators;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.enums.ProcessRetentionTimeUnit;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionBrokenImplementation;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.ProcessRetentionTime;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultInstanceCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class DefaultTerminationNodeV1 implements ProcessNodeDefinition<DefaultTerminationNodeV1.DefaultTerminationNodeV1Config> {
    private static final String NODE_KEY = "default-termination";

    private static final String RETENTION_VALUE_FIELD_KEY = "retention_value";
    private static final String RETENTION_UNIT_FIELD_KEY = "retention_unit";

    private static final String RETENTION_UNIT_DAYS = "days";
    private static final String RETENTION_UNIT_WEEKS = "weeks";
    private static final String RETENTION_UNIT_MONTHS = "months";
    private static final String RETENTION_UNIT_YEARS = "years";

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
    public ProcessNodeExecutionType[] getExecutionTypes() {
        return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
    }

    @Nonnull
    @Override
    public String getName() {
        return "Vorgang beenden";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Beendet einen Vorgang und verwendet die Aufbewahrungsfrist der Prozessversion oder eine eigene Frist.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Schließt einen laufenden Vorgang regulär ab und beendet damit seine weitere Prozessausführung.

                Beim Abschluss gilt die Aufbewahrungsfrist der Prozessversion. Optional können Sie für dieses Element eine andere Frist festlegen. Das Element besitzt keinen weiteren Ausgang, da mit seiner Ausführung der gesamte Vorgang beendet wird.
                """;
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeConfigurationValidationContext<DefaultTerminationNodeV1Config> context) {
        var configuration = context.configuration();
        if (configuration.retentionValue == null && configuration.retentionUnit == null) {
            return null;
        }
        if (configuration.retentionValue == null || positiveWholeNumber(configuration.retentionValue) == null) {
            return Map.of(RETENTION_VALUE_FIELD_KEY, List.of("Geben Sie für die abweichende Aufbewahrungsfrist eine positive ganze Zahl an."));
        }
        var unit = parseRetentionUnit(configuration.retentionUnit);
        if (unit == null) {
            return Map.of(RETENTION_UNIT_FIELD_KEY, List.of("Wählen Sie für die abweichende Aufbewahrungsfrist eine Zeiteinheit aus."));
        }
        try {
            ProcessRetentionTime.calculate(Instant.now(), positiveWholeNumber(configuration.retentionValue), unit);
        } catch (DateTimeException | ArithmeticException e) {
            return Map.of(RETENTION_VALUE_FIELD_KEY, List.of("Die angegebene Aufbewahrungsfrist ist zu groß."));
        }
        return null;
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
        var result = new ProcessNodeExecutionResultInstanceCompleted();
        if (configuration.retentionValue == null && configuration.retentionUnit == null) {
            return result;
        }
        var value = positiveWholeNumber(configuration.retentionValue);
        var unit = parseRetentionUnit(configuration.retentionUnit);
        if (value == null || unit == null) {
            throw new ProcessNodeExecutionExceptionBrokenImplementation("Die abweichende Aufbewahrungsfrist des abschließenden Prozesselements ist ungültig.");
        }
        try {
            return result.setRetentionDate(ProcessRetentionTime.calculate(Instant.now(), value, unit));
        } catch (DateTimeException | ArithmeticException e) {
            throw new ProcessNodeExecutionExceptionBrokenImplementation(e, "Die abweichende Aufbewahrungsfrist des abschließenden Prozesselements ist zu groß.");
        }
    }

    @Nullable
    private static Long positiveWholeNumber(@Nullable Number number) {
        if (number == null) {
            return null;
        }
        try {
            var value = new BigDecimal(number.toString()).longValueExact();
            return value > 0 ? value : null;
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    @Nullable
    private static ProcessRetentionTimeUnit parseRetentionUnit(@Nullable String unit) {
        if (unit == null) {
            return null;
        }
        return switch (unit) {
            case RETENTION_UNIT_DAYS -> ProcessRetentionTimeUnit.Days;
            case RETENTION_UNIT_WEEKS -> ProcessRetentionTimeUnit.Weeks;
            case RETENTION_UNIT_MONTHS -> ProcessRetentionTimeUnit.Months;
            case RETENTION_UNIT_YEARS -> ProcessRetentionTimeUnit.Years;
            default -> null;
        };
    }

    @Nonnull
    @Override
    public Class<DefaultTerminationNodeV1Config> getNodeConfigurationClass() {
        return DefaultTerminationNodeV1Config.class;
    }

    /** Optional retention override for this termination node. */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class DefaultTerminationNodeV1Config {
        /** If absent together with the unit, the process version controls retention. */
        @InputElementPOJOBinding(id = RETENTION_VALUE_FIELD_KEY, type = ElementType.Number, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Abweichende Aufbewahrungsfrist"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Ohne Angabe gilt die Aufbewahrungsfrist der Prozessversion."),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
                @ElementPOJOBindingProperty(key = "required", boolValue = false),
                @ElementPOJOBindingProperty(key = "decimalPlaces", intValue = 0)
        })
        @Nullable
        public Number retentionValue;

        /** Required only when an override value is provided. */
        @InputElementPOJOBinding(id = RETENTION_UNIT_FIELD_KEY, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Einheit der Aufbewahrungsfrist"),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        @Nullable
        public String retentionUnit;
    }
}
