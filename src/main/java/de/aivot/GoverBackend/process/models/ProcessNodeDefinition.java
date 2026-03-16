package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugin.enums.PluginComponentType;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Optional;

public interface ProcessNodeDefinition extends PluginComponent {
    @Nonnull
    @Override
    default PluginComponentType getComponentType() {
        return PluginComponentType.ProcessNodeDefinition;
    }

    /**
     * Get the type of the process node.
     * This type specifies the behavior of the node in the process execution and if and how other nodes can be connected to it.
     *
     * @return The type of the process node.
     */
    @Nonnull
    ProcessNodeType getType();

    /**
     * Get the ports of the process node.
     * The ports are outgoing connections that can be used to connect this node to other nodes in the process definition.
     *
     * @return The ports of the process node.
     */
    @Nonnull
    List<ProcessNodePort> getPorts();

    /**
     * Get the outputs of the process node.
     * The outputs are data produced by this node that can be mapped in the node configuration.
     *
     * @return The outputs of the process node.
     */
    @Nonnull
    default List<ProcessNodeOutput> getOutputs() {
        return List.of();
    }

    /**
     * Get the configuration layout for nodes of this provider type.
     *
     * @param context The configuration context.
     * @return The configuration layout.
     * @throws ResponseException If an error occurs while generating the layout.
     */
    @Nonnull
    default ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) throws ResponseException {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");
        return layout;
    }

    /**
     * Get the testing layout for nodes of this provider type.
     * This layout is used to display the node during testing of process definitions.
     *
     * @param context The testing context.
     * @return The testing layout, or null if not provided.
     * @throws ResponseException If an error occurs while generating the layout.
     */
    @Nullable
    default GroupLayoutElement getTestingLayout(@Nonnull ProcessNodeDefinitionContextTesting context) throws ResponseException {
        return null;
    }

    /**
     * Cleans the configuration data for export.
     *
     * @param configuration The configuration data to be cleaned.
     * @return The cleaned configuration data.
     */
    @Nonnull
    default AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        return configuration;
    }

    /**
     * Prefills the configuration data on import.
     *
     * @param configuration The configuration data to be prefilled.
     * @return The prefilled configuration data.
     */
    @Nonnull
    default AuthoredElementValues prefillConfigurationOnImport(@Nonnull AuthoredElementValues configuration) {
        return configuration;
    }

    /**
     * Validates the configuration of a process definition node entity.
     *
     * @param processNodeEntity The process definition node entity to be validated.
     * @throws ResponseException If the configuration is invalid.
     */
    default void validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                       @Nonnull AuthoredElementValues configuration) throws ResponseException {
    }

    /**
     * Initialize a task by this node provider during process instance execution.
     *
     * @param context The initialization context.
     * @return The result of the node execution.
     * @throws ProcessNodeExecutionException If an error occurs during execution.
     */
    ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException;

    /**
     * Get the task status layout for nodes of this provider type.
     * This layout is used to display the status of the task in task lists and overviews.
     * It is optional and can be null.
     *
     * @param context The context to build the layout for.
     * @return The task status layout, or null if not provided.
     * @throws ResponseException If an error occurs while generating the layout.
     */
    @Nullable
    default LayoutElement<?> getTaskStatusLayout(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        return null;
    }

    /**
     * Get the staff task view layout for nodes of this provider type.
     *
     * @param context The context to build the layout for.
     * @return The task view layout.
     * @throws ResponseException If an error occurs while generating the layout.
     */
    @Nonnull
    default LayoutElement<?> getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        var layout = new GroupLayoutElement();
        layout.setId(getKey() + "-staff-task-view");
        return layout;
    }

    /**
     * Get the task view events for nodes of this provider type.
     * These events can be used to trigger actions in the task view UI.
     *
     * @param context The context to build the events for.
     * @return The task view events.
     * @throws ResponseException If an error occurs while generating the events.
     */
    @Nonnull
    default List<TaskViewEvent> getStaffTaskViewEvents(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        return List.of();
    }

    /**
     * Get the staff task view data for nodes of this provider type.
     *
     * @param context The context to build the data for.
     * @return The task view data.
     * @throws ResponseException If an error occurs while generating the data.
     */
    default DerivedRuntimeElementData getStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        return new DerivedRuntimeElementData();
    }

    /**
     * Update an existing task by this node provider during process instance execution.
     * If this returns an empty Optional, the task is considered not updated.
     *
     * @param context The context for the update.
     * @param update  The update data passed to this node.
     * @param event   The event that triggered the update.
     * @return An Optional containing the result of the node execution, or empty if the task was not updated.
     * @throws ResponseException             If an error occurs during execution.
     * @throws ProcessNodeExecutionException If an error occurs during execution.
     */
    default Optional<ProcessNodeExecutionResult> onUpdateFromStaff(@Nonnull ProcessNodeExecutionContextUIStaff context,
                                                                   @Nonnull AuthoredElementValues update,
                                                                   @Nonnull String event) throws ResponseException, ProcessNodeExecutionException {
        return Optional.empty();
    }


    /**
     * Get the customer task view layout for nodes of this provider type.
     *
     * @param context The context to build the layout for.
     * @return The task view layout.
     * @throws ResponseException If an error occurs while generating the layout.
     */
    @Nonnull
    default GroupLayoutElement getCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer context) throws ResponseException {
        var layout = new GroupLayoutElement();
        layout.setId(getKey() + "-customer-task-view");
        return layout;
    }

    /**
     * Get the customer task view events for nodes of this provider type.
     *
     * @param context The context to build the events for.
     * @return The task view events.
     * @throws ResponseException If an error occurs while generating the events.
     */
    @Nonnull
    default List<TaskViewEvent> getCustomerTaskViewEvents(@Nonnull ProcessNodeExecutionContextUICustomer context) throws ResponseException {
        return List.of();
    }

    /**
     * Get the customer task view data for nodes of this provider type.
     *
     * @param context The context to build the data for.
     * @return The task view data.
     * @throws ResponseException If an error occurs while generating the data.
     */
    default DerivedRuntimeElementData getCustomerTaskViewData(@Nonnull ProcessNodeExecutionContextUICustomer context) throws ResponseException {
        return new DerivedRuntimeElementData();
    }

    /**
     * Update an existing task by this node provider during process instance execution.
     * If this returns an empty Optional, the task is considered not updated.
     *
     * @param context The context for the update.
     * @param update  The update data passed to this node.
     * @param event   The event that triggered the update.
     * @return An Optional containing the result of the node execution, or empty if the task was not updated.
     * @throws ResponseException             If an error occurs during execution.
     * @throws ProcessNodeExecutionException If an error occurs during execution.
     */
    default Optional<ProcessNodeExecutionResult> onUpdateFromCustomer(@Nonnull ProcessNodeExecutionContextUICustomer context,
                                                                      @Nonnull AuthoredElementValues update,
                                                                      @Nonnull String event) throws ResponseException, ProcessNodeExecutionException {
        return Optional.empty();
    }
}
