package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Complete staff task view returned by a process node definition.
 */
public record ProcessNodeStaffView(
        @Nonnull LayoutElement<?> layout,
        @Nonnull List<TaskViewEvent> events,
        @Nonnull AuthoredElementValues data
) {
    /**
     * Create a staff task view and replace the initial data with a non-empty saved snapshot when present.
     *
     * @param context     The context containing the task runtime data.
     * @param layout      The staff task view layout.
     * @param events      The events offered by the staff task view.
     * @param initialData The data generated from stable sources such as configuration and process data.
     * @return The complete staff task view with effective data.
     */
    @Nonnull
    public static ProcessNodeStaffView of(@Nonnull ProcessNodeExecutionContextUIStaff<?> context,
                                          @Nonnull LayoutElement<?> layout,
                                          @Nonnull List<TaskViewEvent> events,
                                          @Nonnull AuthoredElementValues initialData) {
        var savedData = ProcessNodeDefinition.getAutoSavedTaskViewData(
                context.getThisTask().getRuntimeData(),
                ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY
        );
        return new ProcessNodeStaffView(
                layout,
                events,
                savedData == null || savedData.isEmpty() ? initialData : savedData
        );
    }
}
