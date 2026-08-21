package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugin.enums.PluginComponentType;
import de.aivot.prosuna.backend.plugin.models.PluginComponent;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import de.aivot.prosuna.backend.process.models.processContext.*;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.*;

/**
 * Contract for process node definitions that provide configuration, UI and execution behavior for process tasks.
 */
public interface ProcessNodeDefinition<NodeConfig> extends PluginComponent {
    /**
     * Reserved runtime data key used to persist saved staff task view inputs. Because staff and customer tasks may write to the same runtime data, they need to use different keys
     * to persist their data to avoid conflicts.
     */
    String STAFF_TASK_VIEW_DATA_RUNTIME_KEY = "staffTaskViewData";
    /**
     * Reserved runtime data key used to persist saved customer task view inputs. Because staff and customer tasks may write to the same runtime data, they need to use different
     * keys to persist their data to avoid conflicts.
     */
    String CUSTOMER_TASK_VIEW_DATA_RUNTIME_KEY = "customerTaskViewData";

    @Nonnull
    @Override
    default PluginComponentType getComponentType() {
        return PluginComponentType.ProcessNodeDefinition;
    }

    /**
     * Get the type of the process node. This type specifies the behavior of the node in the process execution and if and how other nodes can be connected to it.
     *
     * @return The type of the process node.
     */
    @Nonnull
    ProcessNodeType getType();

    /**
     * Get the ports of the process node. The ports are outgoing connections that can be used to connect this node to other nodes in the process definition.
     *
     * @return The ports of the process node.
     */
    @Nonnull
    List<ProcessNodePort> getPorts();

    /**
     * Get the outputs of the process node. The outputs are data produced by this node that can be mapped in the node configuration. This list must be equivalent to the data stored
     * in the process node element data.
     *
     * @return The output fields of the process node.
     */
    @Nonnull
    default List<ProcessNodeOutput> getOutputs() {
        return List.of();
    }

    /**
     * Get the configuration layout for nodes of this provider type. This is rendered in the process node editor. <br/> Make sure to display all configuration fields defined for
     * this node in the layout returned by this method, otherwise they won't be saved when editing a node of this type. If no layout is returned, only the default fields of a node
     * can be configured.
     *
     * @param context The configuration context.
     * @return The configuration layout.
     * @throws ResponseException If an error occurs while generating the layout.
     */
    @Nonnull
    default ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");
        return layout;
    }

    /**
     * Loads a configuration layout from a JSON resource.
     *
     * <p>This helper is intended for process nodes that keep their editor layout in a static resource instead of
     * building it programmatically. The resource content must be a JSON representation of a {@link ConfigLayoutElement}.
     * I/O and JSON conversion failures are wrapped in a {@link ResponseException} with an internal-server-error
     * response.</p>
     *
     * @param configResource The resource containing the serialized configuration layout.
     * @return The deserialized configuration layout.
     * @throws ResponseException If the resource cannot be read or converted to a configuration layout.
     */
    default ConfigLayoutElement loadConfigLayoutFromResource(@Nonnull Resource configResource) throws ResponseException {
        try {
            return JsonMapperFactory
                    .getInstance()
                    .readValue(configResource.getInputStream(), ConfigLayoutElement.class);
        } catch (IOException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Fehler bei der Erstellung des Konfigurationslayouts: %s",
                    e.getMessage()
            );
        }
    }

                                                             /**
     * Get the testing layout for nodes of this provider type. This layout is used to be displayed in the testing tab of the node during an active test claim. <br/> Use this to
     * display additional information for the user which is usefull for the testing.
     *
     * @param context The testing context.
     * @return The testing layout, or null if not provided.
     * @throws ResponseException If an error occurs while generating the layout.
     */
                                                             @Nullable
    default GroupLayoutElement getTestingLayout(@Nonnull ProcessNodeDefinitionTestingLayoutContext<NodeConfig> context) throws ResponseException {
        return null;
    }

    /**
     * Cleans the configuration data for export. Make sure to remove all system specific data from the configuration in this method, as the exported configuration can be imported
     * in a different system. This includes for example references to users, departments or teams.
     *
     * @param configuration The configuration data to be cleaned.
     * @return The cleaned configuration data.
     */
    @Nonnull
    default AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        return configuration;
    }

    /**
     * Prefills the configuration data on import. Use this to set default data, whenever a new node is created by import. This can be used to set default values for configuration
     * fields, which makes sure that the node is configured correctly after import.
     *
     * @param configuration The configuration data to be prefilled.
     * @return The prefilled configuration data.
     */
    @Nonnull
    default AuthoredElementValues prefillConfigurationOnImport(@Nonnull AuthoredElementValues configuration) {
        return configuration;
    }

    /**
     * Validates the configuration of a process definition node entity. This used to check if the configuration of a node is valid before saving it or publishing the parent
     * process. When errors are returned, the flag {@link ProcessNodeEntity#setSavedWithErrors(Boolean)} is set. Nodes can be saved with errors but a process cannot be published
     * when at least one node does not validate correctly.
     *
     * @param processNodeEntity The process definition node entity to be validated.
     * @param configuration     The configuration to be validated.
     * @return A map of configuration field keys to error messages for that field. If the configuration is valid, null is returned.
     * @throws ResponseException If the configuration is invalid.
     */
    @Nullable
    default Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                            @Nonnull NodeConfig configuration) throws ResponseException {
        return null;
    }

    /**
     * Calculate the list of all process node metadata this process node produces or passes on.
     *
     * @param processNodeEntity The current node.
     * @param configuration     The configuration of the current node.
     * @param previousMetadata  The set of all process node metadata this node produces or passes on.
     * @return The list of all process node keys this node passes onto the next node.
     */
    @Nonnull
    default ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                      @Nonnull NodeConfig configuration,
                                                      @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
        return previousMetadata;
    }

    /**
     * Initialize a task by this node provider during process instance execution. The initialization should return a result to determine how to proceed from here.
     * <ul>
     *     <li>If a staff task should be assigned, return a {@link ProcessNodeExecutionResultTaskAssigned} result.</li>
     *     <li>If this task is completed and should move on, return a {@link ProcessNodeExecutionResultTaskCompleted} result.</li>
     *     <li>If this is a completion node, use the {@link ProcessNodeExecutionResultInstanceCompleted} to complete the whole process.</li>
     * </ul>
     *
     * @param context The initialization context.
     * @return The result of the node execution.
     * @throws ProcessNodeExecutionException If an error occurs during execution.
     */
    ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<NodeConfig> context) throws ProcessNodeExecutionException;

    /**
     * Get the task status layout for nodes of this provider type. This layout is used to display the status of the task in task lists and overviews. It is optional and can be
     * null.
     *
     * @param context The context to build the layout for.
     * @return The task status layout, or null if not provided.
     * @throws ResponseException If an error occurs while generating the layout.
     */
    @Nullable
    default LayoutElement<?> getTaskStatusLayout(@Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context) throws ResponseException {
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
    default LayoutElement<?> getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context) throws ResponseException {
        var layout = new GroupLayoutElement();
        layout.setId(getKey() + "-staff-task-view");
        return layout;
    }

    /**
     * Get the staff task view events for nodes of this provider type. These events can be used to trigger actions in the task view UI.
     *
     * @param context The context to build the events for.
     * @return The task view events.
     * @throws ResponseException If an error occurs while generating the events.
     */
    @Nonnull
    default List<TaskViewEvent> getStaffTaskViewEvents(@Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context) throws ResponseException {
        return List.of();
    }

    /**
     * Build the initial staff task view data from stable sources such as process data, configuration or templates. Saved task view data from the task runtime data is merged on top
     * by {@link #getStaffTaskViewData(ProcessNodeExecutionContextUIStaff)}. Saved keys with a {@code null} value are treated as explicit deletions and therefore override
     * regenerated defaults.
     *
     * @param context The context to build the data for.
     * @return The initial task view data.
     * @throws ResponseException If an error occurs while generating the data.
     */
    @Nonnull
    default AuthoredElementValues createDefaultStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context) throws ResponseException {
        return new AuthoredElementValues();
    }

    /**
     * Read saved staff task view data from the task runtime data.
     *
     * @param context The context to read the data for.
     * @return The saved task view data, or null if none exists.
     */
    @Nullable
    default AuthoredElementValues getAutoSavedStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context) {
        var rawSavedData = context
                .getThisTask()
                .getRuntimeData()
                .get(STAFF_TASK_VIEW_DATA_RUNTIME_KEY);
        if (rawSavedData == null) {
            return null;
        }

        return JsonMapperFactory
                .getNullPreservingInstance()
                .convertValue(rawSavedData, AuthoredElementValues.class);
    }

    /**
     * Get the staff task view data for nodes of this provider type.
     *
     * @param context The context to build the data for.
     * @return The task view data.
     * @throws ResponseException If an error occurs while generating the data.
     */
    @Nonnull
    default AuthoredElementValues getStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context) throws ResponseException {
        var initialData = createDefaultStaffTaskViewData(context);
        var savedData = getAutoSavedStaffTaskViewData(context);
        if (savedData == null || savedData.isEmpty()) {
            return initialData;
        }

        return savedData;
    }

    /**
     * Handle an event that was triggered from the staff task view. If this returns an empty Optional, the task is considered not updated.
     *
     * @param context The context for the update.
     * @param update  The current task view data.
     * @param event   The event that triggered the update.
     * @return An Optional containing the result of the node execution, or empty if the task was not updated.
     * @throws ResponseException             If an error occurs during execution.
     * @throws ProcessNodeExecutionException If an error occurs during execution.
     */
    @Nonnull
    default Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context,
                                                                          @Nonnull AuthoredElementValues update,
                                                                          @Nonnull String event) throws ResponseException, ProcessNodeExecutionException {
        return Optional.empty();
    }

    /**
     * Handle automatically saved data from the staff task view. If this returns an empty Optional, the task is considered not updated.
     *
     * @param context The context for the update.
     * @param update  The automatically saved task view data.
     * @return An Optional containing the result of the node execution, or empty if the task was not updated.
     * @throws ResponseException             If an error occurs during execution.
     * @throws ProcessNodeExecutionException If an error occurs during execution.
     */
    @Nonnull
    default Optional<ProcessNodeExecutionResult> onAutoSaveFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context,
                                                                             @Nonnull AuthoredElementValues update) throws ResponseException, ProcessNodeExecutionException {
        var rtd = new HashMap<>(context.getThisTask().getRuntimeData());
        rtd.put(STAFF_TASK_VIEW_DATA_RUNTIME_KEY, update);

        return new ProcessNodeExecutionResultTaskUpdated()
                .setRuntimeData(rtd)
                .setNodeData(new LinkedHashMap<>(context.getThisTask().getNodeData()))
                .setProcessData(context.getThisTask().getProcessData())
                .asOptional();
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
     * Build the initial customer task view data from stable sources such as process data, configuration or templates. Saved task view data from the task runtime data is merged on
     * top by {@link #getCustomerTaskViewData(ProcessNodeExecutionContextUICustomer)}. Saved keys with a {@code null} value are treated as explicit deletions and therefore override
     * regenerated defaults.
     *
     * @param context The context to build the data for.
     * @return The initial task view data.
     * @throws ResponseException If an error occurs while generating the data.
     */
    @Nonnull
    default AuthoredElementValues createDefaultCustomerTaskViewData(@Nonnull ProcessNodeExecutionContextUICustomer context) throws ResponseException {
        return new AuthoredElementValues();
    }

    /**
     * Read saved customer task view data from the task runtime data.
     *
     * @param context The context to read the data for.
     * @return The saved task view data, or null if none exists.
     */
    @Nullable
    default AuthoredElementValues getAutoSavedCustomerTaskViewData(@Nonnull ProcessNodeExecutionContextUICustomer context) {
        var rawSavedData = context
                .getThisTask()
                .getRuntimeData()
                .get(CUSTOMER_TASK_VIEW_DATA_RUNTIME_KEY);
        if (rawSavedData == null) {
            return null;
        }

        return JsonMapperFactory
                .getNullPreservingInstance()
                .convertValue(rawSavedData, AuthoredElementValues.class);
    }

    /**
     * Get the customer task view data for nodes of this provider type.
     *
     * @param context The context to build the data for.
     * @return The task view data.
     * @throws ResponseException If an error occurs while generating the data.
     */
    @Nonnull
    default AuthoredElementValues getCustomerTaskViewData(@Nonnull ProcessNodeExecutionContextUICustomer context) throws ResponseException {
        var initialData = createDefaultCustomerTaskViewData(context);
        var savedData = getAutoSavedCustomerTaskViewData(context);
        if (savedData == null || savedData.isEmpty()) {
            return initialData;
        }

        var mergedData = new AuthoredElementValues();
        mergedData.putAll(initialData);
        mergedData.putAll(savedData);
        return mergedData;
    }

    /**
     * Handle an event that was triggered from the customer task view. If this returns an empty Optional, the task is considered not updated.
     *
     * @param context The context for the update.
     * @param update  The current task view data.
     * @param derived The derived runtime element data for the current task view data.
     * @param event   The event that triggered the update.
     * @return An Optional containing the result of the node execution, or empty if the task was not updated.
     * @throws ResponseException             If an error occurs during execution.
     * @throws ProcessNodeExecutionException If an error occurs during execution.
     */
    @Nonnull
    default Optional<ProcessNodeExecutionResult> onEventFromCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer context,
                                                                             @Nonnull AuthoredElementValues update,
                                                                             @Nonnull DerivedRuntimeElementData derived,
                                                                             @Nonnull String event) throws ResponseException, ProcessNodeExecutionException {
        return Optional.empty();
    }

    /**
     * Handle automatically saved data from the customer task view. If this returns an empty Optional, the task is considered not updated.
     *
     * @param context The context for the update.
     * @param update  The automatically saved task view data.
     * @param derived The derived runtime element data for the current task view data.
     * @return An Optional containing the result of the node execution, or empty if the task was not updated.
     * @throws ResponseException             If an error occurs during execution.
     * @throws ProcessNodeExecutionException If an error occurs during execution.
     */
    @Nonnull
    default Optional<ProcessNodeExecutionResult> onAutoSaveFromCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer context,
                                                                                @Nonnull AuthoredElementValues update,
                                                                                @Nonnull DerivedRuntimeElementData derived) throws ResponseException, ProcessNodeExecutionException {
        var rtd = new HashMap<>(context.getThisTask().getRuntimeData());
        rtd.put(CUSTOMER_TASK_VIEW_DATA_RUNTIME_KEY, update);

        return new ProcessNodeExecutionResultTaskUpdated()
                .setRuntimeData(rtd)
                .setNodeData(new LinkedHashMap<>(context.getThisTask().getNodeData()))
                .setProcessData(context.getThisTask().getProcessData())
                .asOptional();
    }

    /**
     * Return the class of the configuration pojo. This will automatically be converted and injected into the corresponding methods.
     *
     * @return The class of the configuration pojo.
     */
    @Nonnull
    Class<NodeConfig> getNodeConfigurationClass();

    /**
     * Resolve the name of a process node instance for this node definition. By default, this returns the name of the node entity if it is set, otherwise it returns the name of the
     * node definition. You can override this to provide dynamic names for process node instances based on their configuration or other factors.
     *
     * @param nodeEntity The process node entity to resolve the name for.
     * @return The name of the node.
     */
    default String resolveNodeName(ProcessNodeEntity nodeEntity) {
        if (StringUtils.isNotNullOrEmpty(nodeEntity.getName())) {
            return nodeEntity.getName();
        }
        return getName();
    }
}
