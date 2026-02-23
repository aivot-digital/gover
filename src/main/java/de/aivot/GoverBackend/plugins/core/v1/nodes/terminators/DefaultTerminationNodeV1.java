package de.aivot.GoverBackend.plugins.core.v1.nodes.terminators;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.NumberInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.models.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DefaultTerminationNodeV1 implements ProcessNodeDefinition {
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
        return Core.PLUGIN_KEY;
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
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var retentionInput = new NumberInputElement();
        retentionInput
                .setWeight(8.0)
                .setId(RETENTION_VALUE_FIELD_KEY);
        retentionInput
                .setLabel("Aufbewahrungsfrist")
                .setHint("Geben Sie die Aufbewahrungsfrist für die Prozessdaten nach Abschluss des Prozesses an (z.B. '30 Tage', '6 Monate', '1 Jahr').")
                .setRequired(true);
        layout.addChild(retentionInput);

        var retentionUnitInput = new SelectInputElement();
        retentionUnitInput
                .setWeight(4.0)
                .setId(RETENTION_UNIT_FIELD_KEY);
        retentionUnitInput
                .setLabel("Einheit der Aufbewahrungsfrist")
                .setRequired(true);
        retentionUnitInput
                .setOptions(List.of(
                        RadioInputElementOption.of(RETENTION_UNIT_DAYS, "Tage"),
                        RadioInputElementOption.of(RETENTION_UNIT_WEEKS, "Wochen"),
                        RadioInputElementOption.of(RETENTION_UNIT_MONTHS, "Monate"),
                        RadioInputElementOption.of(RETENTION_UNIT_YEARS, "Jahre")
                ));
        layout.addChild(retentionUnitInput);

        return layout;
    }

    @Override
    public void validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                      @Nonnull ElementData configuration) throws ResponseException {
        configuration
                .get(RETENTION_VALUE_FIELD_KEY)
                .getOptionalValue(Number.class)
                .orElseThrow(ResponseException::badRequest);

        var retentionUnit = configuration
                .get(RETENTION_UNIT_FIELD_KEY)
                .getOptionalValue(String.class)
                .orElseThrow(ResponseException::badRequest);

        // Check if retention unit is valid
        if (!retentionUnit.equals(RETENTION_UNIT_DAYS) &&
                !retentionUnit.equals(RETENTION_UNIT_WEEKS) &&
                !retentionUnit.equals(RETENTION_UNIT_MONTHS) &&
                !retentionUnit.equals(RETENTION_UNIT_YEARS)) {

            var retentionUnitField = configuration
                    .get(RETENTION_UNIT_FIELD_KEY)
                    .addComputedError("Ungültiger Wert für die Einheit der Aufbewahrungsfrist.");
            configuration.put(RETENTION_UNIT_FIELD_KEY, retentionUnitField);

            throw ResponseException
                    .badRequest(configuration);
        }
    }

    @Nullable
    @Override
    public LayoutElement<?> getTaskStatusLayout(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        return null; // TODO
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var retentionTimeValue = context
                .getThisNode()
                .getConfiguration()
                .get(RETENTION_VALUE_FIELD_KEY)
                .getOptionalValue(Number.class)
                .orElse(DEFAULT_RETENTION_VALUE)
                .longValue();

        var retentionTimeUnit = context
                .getThisNode()
                .getConfiguration()
                .get(RETENTION_UNIT_FIELD_KEY)
                .getOptionalValue(String.class)
                .orElse(DEFAULT_RETENTION_UNIT);

        var retentionTime = LocalDateTime.now();
        switch (retentionTimeUnit) {
            case RETENTION_UNIT_DAYS -> retentionTime = retentionTime.plusDays(retentionTimeValue);
            case RETENTION_UNIT_WEEKS -> retentionTime = retentionTime.plusWeeks(retentionTimeValue);
            case RETENTION_UNIT_MONTHS -> retentionTime = retentionTime.plusMonths(retentionTimeValue);
            case RETENTION_UNIT_YEARS -> retentionTime = retentionTime.plusYears(retentionTimeValue);
        }

        return new ProcessNodeExecutionResultInstanceCompleted()
                .setRetentionDate(retentionTime);
    }
}
