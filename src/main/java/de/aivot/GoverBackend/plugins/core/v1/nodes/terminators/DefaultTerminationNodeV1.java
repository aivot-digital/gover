package de.aivot.GoverBackend.plugins.core.v1.nodes.terminators;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.NumberInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElementOption;
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
                .setHint("Geben Sie die Aufbewahrungsfrist für die Vorgangsdaten nach Abschluss des Vorgangs an (z.B. '30 Tage', '6 Monate', '1 Jahr').")
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
                        SelectInputElementOption.of(RETENTION_UNIT_DAYS, "Tage"),
                        SelectInputElementOption.of(RETENTION_UNIT_WEEKS, "Wochen"),
                        SelectInputElementOption.of(RETENTION_UNIT_MONTHS, "Monate"),
                        SelectInputElementOption.of(RETENTION_UNIT_YEARS, "Jahre")
                ));
        layout.addChild(retentionUnitInput);

        return layout;
    }

    @Override
    public void validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                      @Nonnull AuthoredElementValues configuration, @Nonnull DerivedRuntimeElementData derivedRuntimeElementData) throws ResponseException {
        var effectiveValues = derivedRuntimeElementData.getEffectiveValues();

        var retentionValue = effectiveValues.get(RETENTION_VALUE_FIELD_KEY);
        if (!(retentionValue instanceof Number)) {
            throw ResponseException.badRequest();
        }

        var retentionUnitObj = effectiveValues.get(RETENTION_UNIT_FIELD_KEY);
        if (!(retentionUnitObj instanceof String retentionUnit)) {
            throw ResponseException.badRequest();
        }

        // Check if retention unit is valid
        if (!retentionUnit.equals(RETENTION_UNIT_DAYS) &&
                !retentionUnit.equals(RETENTION_UNIT_WEEKS) &&
                !retentionUnit.equals(RETENTION_UNIT_MONTHS) &&
                !retentionUnit.equals(RETENTION_UNIT_YEARS)) {
            throw ResponseException.badRequest("Ungültiger Wert für die Einheit der Aufbewahrungsfrist.");
        }
    }

    @Nullable
    @Override
    public LayoutElement<?> getTaskStatusLayout(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        return null; // TODO
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var configuration = context.getConfiguration().getEffectiveValues();

        var retentionTimeValue = ((Number) configuration
                .getOrDefault(RETENTION_VALUE_FIELD_KEY, DEFAULT_RETENTION_VALUE))
                .longValue();

        var retentionTimeUnit = String.valueOf(configuration
                .getOrDefault(RETENTION_UNIT_FIELD_KEY, DEFAULT_RETENTION_UNIT));

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
