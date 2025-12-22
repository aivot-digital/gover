package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.*;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProcessNodeProvider {
    /**
     * Get the unique key of the process node provider.
     * Providers are identified by this key, when they are assigned to process definition nodes.
     *
     * @return The unique key of the process node provider.
     */
    @Nonnull
    String getKey();

    /**
     * Get the type of the process node.
     * This type specifies the behavior of the node in the process execution and if and how other nodes can be connected to it.
     *
     * @return The type of the process node.
     */
    @Nonnull
    ProcessNodeType getType();

    /**
     * Get the name of the process node.
     * This is displayed in the UI when a node of this type is used in a process definition.
     *
     * @return The name of the process node.
     */
    @Nonnull
    String getName();

    /**
     * Get the description of the process node.
     * This is displayed in the UI when a node of this type is used in a process definition.
     *
     * @return The description of the process node.
     */
    @Nonnull
    String getDescription();

    /**
     * Get the ports of the process node.
     * The ports are outgoing connections that can be used to connect this node to other nodes in the process definition.
     *
     * @return The ports of the process node.
     */
    @Nonnull
    List<ProcessNodePort> getPorts();

    /**
     * Get the configuration layout for nodes of this provider type.
     *
     * @param user                     The user requesting the layout.
     * @param processDefinition        The process definition the node belongs to.
     * @param processDefinitionVersion The process definition version the node belongs to.
     * @param thisNode                 The node entity for which the layout is requested.
     * @return The configuration layout.
     * @throws ResponseException If an error occurs while generating the layout.
     */
    @Nonnull
    default ConfigLayoutElement getConfigurationLayout(@Nonnull UserEntity user,
                                                       @Nonnull ProcessDefinitionEntity processDefinition,
                                                       @Nonnull ProcessDefinitionVersionEntity processDefinitionVersion,
                                                       @Nonnull ProcessDefinitionNodeEntity thisNode) throws ResponseException {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");
        return layout;
    }

    /**
     * Get the staff task view layout for nodes of this provider type.
     *
     * @param user            The user requesting the layout.
     * @param thisNode        The node entity for which the layout is requested.
     * @param processInstance The process instance the task belongs to.
     * @param thisTask        The task entity for which the layout is requested.
     * @return The task view layout.
     * @throws ResponseException If an error occurs while generating the layout.
     */
    @Nonnull
    default GroupLayoutElement getStaffTaskView(@Nonnull UserEntity user,
                                                @Nonnull ProcessDefinitionNodeEntity thisNode,
                                                @Nonnull ProcessInstanceEntity processInstance,
                                                @Nonnull ProcessInstanceTaskEntity thisTask) throws ResponseException {
        var layout = new GroupLayoutElement();
        layout.setId(getKey() + "-staff-task-view");
        return layout;
    }

    /**
     * Get the staff task view data for nodes of this provider type.
     *
     * @param user            The user requesting the data. If null, the request is unauthenticated.
     * @param thisNode        The node entity for which the data is requested.
     * @param processInstance The process instance the task belongs to.
     * @param thisTask        The task entity for which the data is requested.
     * @return The task view data.
     * @throws ResponseException If an error occurs while generating the data.
     */
    default ElementData getStaffTaskViewData(@Nonnull UserEntity user,
                                             @Nonnull ProcessDefinitionNodeEntity thisNode,
                                             @Nonnull ProcessInstanceEntity processInstance,
                                             @Nonnull ProcessInstanceTaskEntity thisTask) throws ResponseException {
        return new ElementData();
    }

    /**
     * Get the customer task view layout for nodes of this provider type.
     *
     * @param identityId      The identity ID requesting the layout. If null, the request is unauthenticated.
     * @param thisNode        The node entity for which the layout is requested.
     * @param processInstance The process instance the task belongs to.
     * @param thisTask        The task entity for which the layout is requested.
     * @return The task view layout.
     * @throws ResponseException If an error occurs while generating the layout.
     */
    @Nonnull
    default GroupLayoutElement getCustomerTaskView(@Nullable String identityId,
                                                   @Nonnull ProcessDefinitionNodeEntity thisNode,
                                                   @Nonnull ProcessInstanceEntity processInstance,
                                                   @Nonnull ProcessInstanceTaskEntity thisTask) throws ResponseException {
        var layout = new GroupLayoutElement();
        layout.setId(getKey() + "-customer-task-view");
        return layout;
    }

    /**
     * Get the customer task view data for nodes of this provider type.
     *
     * @param identityId      The identity ID requesting the data. If null, the request is unauthenticated.
     * @param thisNode        The node entity for which the data is requested.
     * @param processInstance The process instance the task belongs to.
     * @param thisTask        The task entity for which the data is requested.
     * @return The task view data.
     * @throws ResponseException If an error occurs while generating the data.
     */
    default ElementData getCustomerTaskViewData(@Nullable String identityId,
                                                @Nonnull ProcessDefinitionNodeEntity thisNode,
                                                @Nonnull ProcessInstanceEntity processInstance,
                                                @Nonnull ProcessInstanceTaskEntity thisTask) throws ResponseException {
        return new ElementData();
    }


    /**
     * Get the task view events for nodes of this provider type.
     * These events can be used to trigger actions in the task view UI.
     *
     * @param user            The user requesting the events.
     * @param thisNode        The node entity for which the events are requested.
     * @param processInstance The process instance the task belongs to.
     * @param thisTask        The task entity for which the events are requested.
     * @return The task view events.
     * @throws ResponseException If an error occurs while generating the events.
     */
    @Nonnull
    default List<TaskViewEvent> getStaffTaskViewEvents(@Nonnull UserEntity user,
                                                       @Nonnull ProcessDefinitionNodeEntity thisNode,
                                                       @Nonnull ProcessInstanceEntity processInstance,
                                                       @Nonnull ProcessInstanceTaskEntity thisTask) throws ResponseException {
        return List.of();
    }

    /**
     * Get the customer task view events for nodes of this provider type.
     *
     * @param identityId      The identity ID requesting the events. If null, the request is unauthenticated.
     * @param thisNode        The node entity for which the events are requested.
     * @param processInstance The process instance the task belongs to.
     * @param thisTask        The task entity for which the events are requested.
     * @return The task view events.
     * @throws ResponseException If an error occurs while generating the events.
     */
    @Nonnull
    default List<TaskViewEvent> getCustomerTaskViewEvents(@Nullable String identityId,
                                                          @Nonnull ProcessDefinitionNodeEntity thisNode,
                                                          @Nonnull ProcessInstanceEntity processInstance,
                                                          @Nonnull ProcessInstanceTaskEntity thisTask) throws ResponseException {
        return List.of();
    }

    /**
     * Cleans the configuration data for export.
     *
     * @param configuration The configuration data to be cleaned.
     * @return The cleaned configuration data.
     */
    @Nonnull
    default ElementData cleanConfigurationForExport(@Nonnull ElementData configuration) {
        return configuration;
    }

    /**
     * Prefills the configuration data on import.
     *
     * @param configuration The configuration data to be prefilled.
     * @return The prefilled configuration data.
     */
    @Nonnull
    default ElementData prefillConfigurationOnImport(@Nonnull ElementData configuration) {
        return configuration;
    }

    /**
     * Validates the configuration of a process definition node entity.
     *
     * @param entity The process definition node entity to be validated.
     * @throws ResponseException If the configuration is invalid.
     */
    default void validateConfiguration(@Nonnull ProcessDefinitionNodeEntity entity) throws ResponseException {
    }

    /**
     * Initialize a task by this node provider during process instance execution.
     *
     * @param processInstance The process instance
     * @param thisNode        The current node
     * @param data            The data passed to this node.
     * @return The result of the node execution.
     * @throws Exception If an error occurs during execution.
     */
    ProcessNodeExecutionResult init(@Nonnull ProcessInstanceEntity processInstance,
                                    @Nonnull ProcessDefinitionNodeEntity thisNode,
                                    @Nonnull Map<String, Object> data) throws Exception;

    /**
     * Update an existing task by this node provider during process instance execution.
     * If this returns an empty Optional, the task is considered not updated.
     *
     * @param processInstance The process instance
     * @param thisNode        The current node
     * @param data            The data passed to this node.
     * @param update          The update data passed to this node.
     * @param event           The event that triggered the update.
     * @return An Optional containing the result of the node execution, or empty if the task was not updated.
     * @throws Exception If an error occurs during execution.
     */
    default Optional<ProcessNodeExecutionResult> updateStaff(@Nonnull UserEntity user,
                                                             @Nonnull ProcessInstanceEntity processInstance,
                                                             @Nonnull ProcessDefinitionNodeEntity thisNode,
                                                             @Nonnull Map<String, Object> data,
                                                             @Nonnull Map<String, Object> update,
                                                             @Nonnull String event) throws Exception {
        return Optional.empty();
    }

    /**
     * Update an existing task by this node provider during process instance execution.
     * If this returns an empty Optional, the task is considered not updated.
     *
     * @param identityId      The identity ID of the customer. If null, the request is unauthenticated.
     * @param processInstance The process instance
     * @param thisNode        The current node
     * @param data            The data passed to this node.
     * @param update          The update data passed to this node.
     * @param event           The event that triggered the update.
     * @return An Optional containing the result of the node execution, or empty if the task was not updated.
     * @throws Exception If an error occurs during execution.
     */
    default Optional<ProcessNodeExecutionResult> updateCustomer(@Nullable String identityId,
                                                                @Nonnull ProcessInstanceEntity processInstance,
                                                                @Nonnull ProcessDefinitionNodeEntity thisNode,
                                                                @Nonnull Map<String, Object> data,
                                                                @Nonnull Map<String, Object> update,
                                                                @Nonnull String event) throws Exception {
        return Optional.empty();
    }
}
