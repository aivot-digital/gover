package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Complete customer task view returned by a process node definition.
 *
 * @param requiredExistingIdentityId The process identity whose provider account must be authenticated before the view
 *                                   can be used. {@code null} disables the additional identity check.
 * @param requiredNewIdentitySlot    A new process identity that can be collected in this view and made available to
 *                                   downstream process nodes. {@code null} disables the new identity slot.
 */
public record ProcessNodeCustomerView(
        @Nonnull GroupLayoutElement layout,
        @Nonnull List<TaskViewEvent> events,
        @Nonnull AuthoredElementValues data,
        @Nullable String requiredExistingIdentityId,
        @Nullable IdentityConfigElementSlot requiredNewIdentitySlot
) {
    public ProcessNodeCustomerView(@Nonnull GroupLayoutElement layout,
                                   @Nonnull List<TaskViewEvent> events,
                                   @Nonnull AuthoredElementValues data,
                                   @Nullable String requiredExistingIdentityId) {
        this(layout, events, data, requiredExistingIdentityId, null);
    }

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
     * @param context                    The context containing the task runtime data.
     * @param layout                     The customer task view layout.
     * @param events                     The events offered by the customer task view.
     * @param initialData                The data generated from stable sources such as configuration and process data.
     * @param requiredExistingIdentityId The process identity that must be authenticated before the view can be used.
     * @return The complete customer task view with effective data.
     */
    @Nonnull
    public static ProcessNodeCustomerView of(@Nonnull ProcessNodeExecutionContextUICustomer<?> context,
                                             @Nonnull GroupLayoutElement layout,
                                             @Nonnull List<TaskViewEvent> events,
                                             @Nonnull AuthoredElementValues initialData,
                                             @Nullable String requiredExistingIdentityId) {
        return of(context, layout, events, initialData, requiredExistingIdentityId, null);
    }

    /**
     * Create a customer task view with existing and new identity requirements and merge saved values onto its
     * initial data when present.
     */
    @Nonnull
    public static ProcessNodeCustomerView of(@Nonnull ProcessNodeExecutionContextUICustomer<?> context,
                                             @Nonnull GroupLayoutElement layout,
                                             @Nonnull List<TaskViewEvent> events,
                                             @Nonnull AuthoredElementValues initialData,
                                             @Nullable String requiredExistingIdentityId,
                                             @Nullable IdentityConfigElementSlot requiredNewIdentitySlot) {
        var savedData = ProcessNodeDefinition.getAutoSavedTaskViewData(
                context.getThisTask().getRuntimeData(),
                ProcessNodeDefinition.CUSTOMER_TASK_VIEW_DATA_RUNTIME_KEY
        );
        if (savedData == null || savedData.isEmpty()) {
            return new ProcessNodeCustomerView(
                    layout,
                    events,
                    initialData,
                    requiredExistingIdentityId,
                    requiredNewIdentitySlot
            );
        }

        var mergedData = new AuthoredElementValues();
        mergedData.putAll(initialData);
        mergedData.putAll(savedData);
        return new ProcessNodeCustomerView(
                layout,
                events,
                mergedData,
                requiredExistingIdentityId,
                requiredNewIdentitySlot
        );
    }
}
