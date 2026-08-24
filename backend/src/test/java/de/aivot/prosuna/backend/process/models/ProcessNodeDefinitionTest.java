package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskUpdated;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessNodeDefinitionTest {
    @Test
    void getStaffTaskViewData_DefaultReturnsSavedSnapshotWhenPresent() throws Exception {
        ProcessNodeDefinition<AuthoredElementValues> definition = new ProcessNodeDefinition<>() {
            @Override
            public String getParentPluginKey() {
                return "test.plugin";
            }

            @Override
            public String getComponentKey() {
                return "test-node";
            }

            @Override
            public String getComponentVersion() {
                return "1.0.0";
            }

            @Override
            public String getName() {
                return "Test Node";
            }

            @Override
            public String getAbstract() {
                return "Test node abstract";
            }

            @Override
            public String getDescription() {
                return "Test node description";
            }

            @Nonnull
            @Override
            public ProcessNodeType getType() {
                return ProcessNodeType.Action;
            }

            @Nonnull
            @Override
            public ProcessNodeExecutionType[] getExecutionTypes() {
                return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
            }

            @Nonnull
            @Override
            public List<ProcessNodePort> getPorts() {
                return List.of();
            }

            @Override
            public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) {
                return new ProcessNodeExecutionResultTaskUpdated();
            }

            @Nonnull
            @Override
            public AuthoredElementValues createDefaultStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff context) {
                var initialData = new AuthoredElementValues();
                initialData.put("defaultField", "initial");
                initialData.put("sharedField", "initial");
                return initialData;
            }

            @Nonnull
            @Override
            public Class<AuthoredElementValues> getNodeConfigurationClass() {
                return AuthoredElementValues.class;
            }
        };

        var context = staffContext(
                Map.of(
                        ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY,
                        Map.of(
                                "defaultField", "initial",
                                "sharedField", "saved",
                                "savedField", "saved"
                        )
                ),
                Map.of(),
                Map.of()
        );

        var data = definition.getStaffTaskViewData(context);

        assertEquals("initial", data.get("defaultField"));
        assertEquals("saved", data.get("sharedField"));
        assertEquals("saved", data.get("savedField"));
    }

    @Test
    void getStaffTaskViewData_DefaultPreservesSavedNullValues() throws Exception {
        ProcessNodeDefinition<AuthoredElementValues> definition = new ProcessNodeDefinition<>() {
            @Override
            public String getParentPluginKey() {
                return "test.plugin";
            }

            @Override
            public String getComponentKey() {
                return "test-node";
            }

            @Override
            public String getComponentVersion() {
                return "1.0.0";
            }

            @Override
            public String getName() {
                return "Test Node";
            }

            @Override
            public String getAbstract() {
                return "Test node abstract";
            }

            @Override
            public String getDescription() {
                return "Test node description";
            }

            @Nonnull
            @Override
            public ProcessNodeType getType() {
                return ProcessNodeType.Action;
            }

            @Nonnull
            @Override
            public ProcessNodeExecutionType[] getExecutionTypes() {
                return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
            }

            @Nonnull
            @Override
            public List<ProcessNodePort> getPorts() {
                return List.of();
            }

            @Override
            public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) {
                return new ProcessNodeExecutionResultTaskUpdated();
            }

            @Nonnull
            @Override
            public AuthoredElementValues createDefaultStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff context) {
                var initialData = new AuthoredElementValues();
                initialData.put("defaultField", "initial");
                return initialData;
            }

            @Nonnull
            @Override
            public Class<AuthoredElementValues> getNodeConfigurationClass() {
                return AuthoredElementValues.class;
            }
        };

        var runtimeData = new HashMap<String, Object>();
        var savedData = new HashMap<String, Object>();
        savedData.put("defaultField", null);
        runtimeData.put(ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY, savedData);

        var context = staffContext(
                runtimeData,
                Map.of(),
                Map.of()
        );

        var data = definition.getStaffTaskViewData(context);

        assertTrue(data.containsKey("defaultField"));
        assertNull(data.get("defaultField"));
    }

    @Test
    void onAutoSaveFromStaffTaskView_DefaultPersistsSavedSnapshotAndPreservesTaskData() throws Exception {
        ProcessNodeDefinition<AuthoredElementValues> definition = new ProcessNodeDefinition<>() {
            @Override
            public String getParentPluginKey() {
                return "test.plugin";
            }

            @Override
            public String getComponentKey() {
                return "test-node";
            }

            @Override
            public String getComponentVersion() {
                return "1.0.0";
            }

            @Override
            public String getName() {
                return "Test Node";
            }

            @Override
            public String getAbstract() {
                return "Test node abstract";
            }

            @Override
            public String getDescription() {
                return "Test node description";
            }

            @Nonnull
            @Override
            public ProcessNodeType getType() {
                return ProcessNodeType.Action;
            }

            @Nonnull
            @Override
            public ProcessNodeExecutionType[] getExecutionTypes() {
                return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
            }

            @Nonnull
            @Override
            public List<ProcessNodePort> getPorts() {
                return List.of();
            }

            @Override
            public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) {
                return new ProcessNodeExecutionResultTaskUpdated();
            }

            @Nonnull
            @Override
            public Class<AuthoredElementValues> getNodeConfigurationClass() {
                return AuthoredElementValues.class;
            }
        };

        var update = new AuthoredElementValues();
        update.put("field", "value");

        var context = staffContext(
                Map.of("keep", "value"),
                Map.of("existing", "node-data"),
                Map.of("applicant", Map.of("name", "Ada"))
        );

        var result = definition.onAutoSaveFromStaffTaskView(context, update);

        assertTrue(result.isPresent());
        var updated = assertInstanceOf(ProcessNodeExecutionResultTaskUpdated.class, result.get());
        assertEquals("value", updated.getRuntimeData().get("keep"));
        assertEquals(Map.of("existing", "node-data"), updated.getNodeData());
        assertEquals(Map.of("applicant", Map.of("name", "Ada")), updated.getProcessData());

        var savedData = updated.getRuntimeData().get(ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY);
        assertEquals("value", ((Map<?, ?>) savedData).get("field"));
    }

    @Test
    void getCustomerTaskViewData_DefaultMergesSavedDataOntoInitialData() throws Exception {
        ProcessNodeDefinition<AuthoredElementValues> definition = new ProcessNodeDefinition<>() {
            @Override
            public String getParentPluginKey() {
                return "test.plugin";
            }

            @Override
            public String getComponentKey() {
                return "test-node";
            }

            @Override
            public String getComponentVersion() {
                return "1.0.0";
            }

            @Override
            public String getName() {
                return "Test Node";
            }

            @Override
            public String getAbstract() {
                return "Test node abstract";
            }

            @Override
            public String getDescription() {
                return "Test node description";
            }

            @Nonnull
            @Override
            public ProcessNodeType getType() {
                return ProcessNodeType.Action;
            }

            @Nonnull
            @Override
            public ProcessNodeExecutionType[] getExecutionTypes() {
                return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
            }

            @Nonnull
            @Override
            public List<ProcessNodePort> getPorts() {
                return List.of();
            }

            @Override
            public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) {
                return new ProcessNodeExecutionResultTaskUpdated();
            }

            @Nonnull
            @Override
            public AuthoredElementValues createDefaultCustomerTaskViewData(@Nonnull ProcessNodeExecutionContextUICustomer context) {
                var initialData = new AuthoredElementValues();
                initialData.put("defaultField", "initial");
                initialData.put("sharedField", "initial");
                return initialData;
            }

            @Nonnull
            @Override
            public Class<AuthoredElementValues> getNodeConfigurationClass() {
                return AuthoredElementValues.class;
            }
        };

        var context = customerContext(
                Map.of(
                        ProcessNodeDefinition.CUSTOMER_TASK_VIEW_DATA_RUNTIME_KEY,
                        Map.of(
                                "sharedField", "saved",
                                "savedField", "saved"
                        )
                ),
                Map.of(),
                Map.of()
        );

        var data = definition.getCustomerTaskViewData(context);

        assertEquals("initial", data.get("defaultField"));
        assertEquals("saved", data.get("sharedField"));
        assertEquals("saved", data.get("savedField"));
    }

    @Test
    void getCustomerTaskViewData_DefaultTreatsSavedNullAsExplicitDeletion() throws Exception {
        ProcessNodeDefinition<AuthoredElementValues> definition = new ProcessNodeDefinition<>() {
            @Override
            public String getParentPluginKey() {
                return "test.plugin";
            }

            @Override
            public String getComponentKey() {
                return "test-node";
            }

            @Override
            public String getComponentVersion() {
                return "1.0.0";
            }

            @Override
            public String getName() {
                return "Test Node";
            }

            @Override
            public String getAbstract() {
                return "Test node abstract";
            }

            @Override
            public String getDescription() {
                return "Test node description";
            }

            @Nonnull
            @Override
            public ProcessNodeType getType() {
                return ProcessNodeType.Action;
            }

            @Nonnull
            @Override
            public ProcessNodeExecutionType[] getExecutionTypes() {
                return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
            }

            @Nonnull
            @Override
            public List<ProcessNodePort> getPorts() {
                return List.of();
            }

            @Override
            public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) {
                return new ProcessNodeExecutionResultTaskUpdated();
            }

            @Nonnull
            @Override
            public AuthoredElementValues createDefaultCustomerTaskViewData(@Nonnull ProcessNodeExecutionContextUICustomer context) {
                var initialData = new AuthoredElementValues();
                initialData.put("defaultField", "initial");
                return initialData;
            }

            @Nonnull
            @Override
            public Class<AuthoredElementValues> getNodeConfigurationClass() {
                return AuthoredElementValues.class;
            }
        };

        var runtimeData = new HashMap<String, Object>();
        var savedData = new HashMap<String, Object>();
        savedData.put("defaultField", null);
        runtimeData.put(ProcessNodeDefinition.CUSTOMER_TASK_VIEW_DATA_RUNTIME_KEY, savedData);

        var context = customerContext(
                runtimeData,
                Map.of(),
                Map.of()
        );

        var data = definition.getCustomerTaskViewData(context);

        assertTrue(data.containsKey("defaultField"));
        assertNull(data.get("defaultField"));
    }

    @Test
    void onAutoSaveFromCustomerTaskView_DefaultPersistsSavedSnapshotAndPreservesTaskData() throws Exception {
        ProcessNodeDefinition<AuthoredElementValues> definition = new ProcessNodeDefinition<>() {
            @Override
            public String getParentPluginKey() {
                return "test.plugin";
            }

            @Override
            public String getComponentKey() {
                return "test-node";
            }

            @Override
            public String getComponentVersion() {
                return "1.0.0";
            }

            @Override
            public String getName() {
                return "Test Node";
            }

            @Override
            public String getAbstract() {
                return "Test node abstract";
            }

            @Override
            public String getDescription() {
                return "Test node description";
            }

            @Nonnull
            @Override
            public ProcessNodeType getType() {
                return ProcessNodeType.Action;
            }

            @Nonnull
            @Override
            public ProcessNodeExecutionType[] getExecutionTypes() {
                return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
            }

            @Nonnull
            @Override
            public List<ProcessNodePort> getPorts() {
                return List.of();
            }

            @Override
            public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) {
                return new ProcessNodeExecutionResultTaskUpdated();
            }

            @Nonnull
            @Override
            public Class<AuthoredElementValues> getNodeConfigurationClass() {
                return AuthoredElementValues.class;
            }
        };

        var update = new AuthoredElementValues();
        update.put("field", "value");

        var context = customerContext(
                Map.of("keep", "value"),
                Map.of("existing", "node-data"),
                Map.of("applicant", Map.of("name", "Ada"))
        );

        var result = definition.onAutoSaveFromCustomerTaskView(context, update, new DerivedRuntimeElementData());

        assertTrue(result.isPresent());
        var updated = assertInstanceOf(ProcessNodeExecutionResultTaskUpdated.class, result.get());
        assertEquals("value", updated.getRuntimeData().get("keep"));
        assertEquals(Map.of("existing", "node-data"), updated.getNodeData());
        assertEquals(Map.of("applicant", Map.of("name", "Ada")), updated.getProcessData());

        var savedData = updated.getRuntimeData().get(ProcessNodeDefinition.CUSTOMER_TASK_VIEW_DATA_RUNTIME_KEY);
        assertEquals("value", ((Map<?, ?>) savedData).get("field"));
    }

    private static ProcessNodeExecutionContextUIStaff staffContext(Map<String, Object> runtimeData,
                                                                   Map<String, Object> nodeData,
                                                                   Map<String, Object> processData) {
        var task = new ProcessInstanceTaskEntity()
                .setRuntimeData(new HashMap<>(runtimeData))
                .setNodeData(new HashMap<>(nodeData))
                .setProcessData(new HashMap<>(processData));

        return new ProcessNodeExecutionContextUIStaff(
                logger(),
                new ProcessNodeEntity(),
                new ProcessInstanceEntity(),
                task,
                null,
                new UserEntity().setId("user-1"),
                new DerivedRuntimeElementData(),
                new ProcessExecutionData()
        );
    }

    private static ProcessNodeExecutionContextUICustomer customerContext(Map<String, Object> runtimeData,
                                                                         Map<String, Object> nodeData,
                                                                         Map<String, Object> processData) {
        var task = new ProcessInstanceTaskEntity()
                .setRuntimeData(new HashMap<>(runtimeData))
                .setNodeData(new HashMap<>(nodeData))
                .setProcessData(new HashMap<>(processData));

        return new ProcessNodeExecutionContextUICustomer(
                logger(),
                new ProcessNodeEntity(),
                new ProcessInstanceEntity(),
                task,
                null,
                null
        );
    }

    private static ProcessNodeExecutionLogger logger() {
        return new ProcessNodeExecutionLogger(
                99L,
                456L,
                null,
                null,
                proxy(ProcessInstanceHistoryEventRepository.class, (methodName, args) -> switch (methodName) {
                    case "save" -> args[0];
                    default -> unsupported(methodName);
                })
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class[]{type},
                (proxy, method, args) -> invocation.apply(method.getName(), args == null ? new Object[0] : args)
        );
    }

    private static Object unsupported(String methodName) {
        throw new UnsupportedOperationException("Method not supported in test: " + methodName);
    }

    @FunctionalInterface
    private interface Invocation {
        Object apply(String methodName, Object[] args);
    }
}
