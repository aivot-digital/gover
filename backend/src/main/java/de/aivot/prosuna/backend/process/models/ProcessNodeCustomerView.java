package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Complete customer task view returned by a process node definition.
 *
 * @param requiredIdentityId The process identity whose provider account must be authenticated before the view
 *                           can be used. {@code null} disables the additional identity check.
 */
public record ProcessNodeCustomerView(
        @Nonnull GroupLayoutElement layout,
        @Nonnull List<TaskViewEvent> events,
        @Nonnull AuthoredElementValues data,
        @Nullable String requiredIdentityId,
        @Nullable IdentityConfigElementSlot requiredNewIdentitySlot
) {
    /**
     * Create a customer task view and merge saved values onto its initial data when present.
     *
     * @param context     The context containing the task runtime data.
     * @param layout      The customer task view layout.
     * @param events      The events offered by the customer task view.
     * @param initialData The data generated from stable sources such as configuration and process data.
     * @return The complete customer task view with effective data.
     */
    @Nonnull
    public static ProcessNodeCustomerView of(@Nonnull ProcessNodeExecutionContextUICustomer<?> context,
                                             @Nonnull GroupLayoutElement layout,
                                             @Nonnull List<TaskViewEvent> events,
                                             @Nonnull AuthoredElementValues initialData) {
        return of(context, layout, events, initialData, null);
    }

    /**
     * Create a customer task view with an optional identity requirement and merge saved values onto its initial
     * data when present.
     *
     * @param context            The context containing the task runtime data.
     * @param layout             The customer task view layout.
     * @param events             The events offered by the customer task view.
     * @param initialData        The data generated from stable sources such as configuration and process data.
     * @param requiredIdentityId The process identity that must be authenticated before the view can be used.
     * @return The complete customer task view with effective data.
     */
    @Nonnull
    public static ProcessNodeCustomerView of(@Nonnull ProcessNodeExecutionContextUICustomer<?> context,
                                             @Nonnull GroupLayoutElement layout,
                                             @Nonnull List<TaskViewEvent> events,
                                             @Nonnull AuthoredElementValues initialData,
                                             @Nullable String requiredIdentityId) {
        var savedData = ProcessNodeDefinition.getAutoSavedTaskViewData(
                context.getThisTask().getRuntimeData(),
                ProcessNodeDefinition.CUSTOMER_TASK_VIEW_DATA_RUNTIME_KEY
        );
        if (savedData == null || savedData.isEmpty()) {
            return new ProcessNodeCustomerView(layout, events, initialData, requiredIdentityId, null);
        }

        var mergedData = new AuthoredElementValues();
        mergedData.putAll(initialData);
        mergedData.putAll(savedData);
        return new ProcessNodeCustomerView(layout, events, mergedData, requiredIdentityId, null);
    }
}
