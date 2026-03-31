package de.aivot.GoverBackend.process.workers;

import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.services.ProcessTaskMailService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextInit;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.repositories.ProcessEdgeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProcessNodeExecutionResultHandlerTest {
    @Test
    void handleResult_SendsAssignmentMail_ForNewAssignmentToOtherUser() throws ProcessNodeExecutionException {
        var triggeringUser = user("user-1", "Trigger User");
        var assignedUser = user("user-2", "Assigned User");
        var mailService = new RecordingProcessTaskMailService();
        var savedTasks = new ArrayList<ProcessInstanceTaskEntity>();
        var handler = createHandler(savedTasks, Map.of(
                triggeringUser.getId(), triggeringUser,
                assignedUser.getId(), assignedUser
        ), mailService);

        var logger = new RecordingProcessNodeExecutionLogger();
        var processInstance = processInstance();
        var processInstanceTask = processInstanceTask(null);
        var currentNode = processNode("Pruefung");
        var provider = new TestProcessNodeDefinition("Fallback task");

        handler.handleResult(
                logger,
                triggeringUser,
                provider,
                currentNode,
                processInstance,
                processInstanceTask,
                null,
                ProcessNodeExecutionResultTaskAssigned.of(assignedUser.getId())
        );

        assertEquals(1, savedTasks.size());
        assertEquals(assignedUser.getId(), processInstanceTask.getAssignedUserId());
        assertEquals(1, mailService.sendCount);
        assertEquals(assignedUser.getId(), mailService.lastAssignedUserId);
        assertFalse(mailService.lastReassignment);
        assertEquals(0, logger.exceptionCount);
    }

    @Test
    void handleResult_DoesNotSendAssignmentMail_ForSelfAssignment() throws ProcessNodeExecutionException {
        var triggeringUser = user("user-1", "Trigger User");
        var mailService = new RecordingProcessTaskMailService();
        var handler = createHandler(new ArrayList<>(), Map.of(triggeringUser.getId(), triggeringUser), mailService);

        handler.handleResult(
                new RecordingProcessNodeExecutionLogger(),
                triggeringUser,
                new TestProcessNodeDefinition("Fallback task"),
                processNode("Pruefung"),
                processInstance(),
                processInstanceTask(null),
                null,
                ProcessNodeExecutionResultTaskAssigned.of(triggeringUser.getId())
        );

        assertEquals(0, mailService.sendCount);
    }

    @Test
    void handleResult_DoesNotSendAssignmentMail_ForUnchangedAssignee() throws ProcessNodeExecutionException {
        var triggeringUser = user("user-1", "Trigger User");
        var assignedUser = user("user-2", "Assigned User");
        var mailService = new RecordingProcessTaskMailService();
        var handler = createHandler(new ArrayList<>(), Map.of(
                triggeringUser.getId(), triggeringUser,
                assignedUser.getId(), assignedUser
        ), mailService);

        handler.handleResult(
                new RecordingProcessNodeExecutionLogger(),
                triggeringUser,
                new TestProcessNodeDefinition("Fallback task"),
                processNode("Pruefung"),
                processInstance(),
                processInstanceTask(assignedUser.getId()),
                null,
                ProcessNodeExecutionResultTaskAssigned.of(assignedUser.getId())
        );

        assertEquals(0, mailService.sendCount);
    }

    @Test
    void handleResult_DoesNotFail_WhenAssignmentMailFails() {
        var triggeringUser = user("user-1", "Trigger User");
        var assignedUser = user("user-2", "Assigned User");
        var mailService = new RecordingProcessTaskMailService();
        mailService.throwOnSend = true;

        var savedTasks = new ArrayList<ProcessInstanceTaskEntity>();
        var handler = createHandler(savedTasks, Map.of(
                triggeringUser.getId(), triggeringUser,
                assignedUser.getId(), assignedUser
        ), mailService);
        var logger = new RecordingProcessNodeExecutionLogger();
        var task = processInstanceTask(null);

        assertDoesNotThrow(() -> handler.handleResult(
                logger,
                triggeringUser,
                new TestProcessNodeDefinition("Fallback task"),
                processNode("Pruefung"),
                processInstance(),
                task,
                null,
                ProcessNodeExecutionResultTaskAssigned.of(assignedUser.getId())
        ));

        assertEquals(1, savedTasks.size());
        assertEquals(assignedUser.getId(), task.getAssignedUserId());
        assertEquals(1, mailService.sendCount);
        assertEquals(1, logger.exceptionCount);
    }

    private static ProcessNodeExecutionResultHandler createHandler(List<ProcessInstanceTaskEntity> savedTasks,
                                                                   Map<String, UserEntity> users,
                                                                   RecordingProcessTaskMailService mailService) {
        return new ProcessNodeExecutionResultHandler(
                null,
                proxy(ProcessInstanceRepository.class),
                createTaskRepository(savedTasks),
                proxy(ProcessEdgeRepository.class),
                new TestUserService(users),
                mailService
        );
    }

    private static ProcessInstanceTaskRepository createTaskRepository(List<ProcessInstanceTaskEntity> savedTasks) {
        return ProcessNodeExecutionResultHandlerTest.<ProcessInstanceTaskRepository>proxy(ProcessInstanceTaskRepository.class, (methodName, args) -> switch (methodName) {
            case "save" -> {
                var entity = (ProcessInstanceTaskEntity) args[0];
                if (entity.getId() == null) {
                    entity.setId(100L + savedTasks.size());
                }
                savedTasks.add(entity);
                yield entity;
            }
            default -> null;
        });
    }

    private static ProcessInstanceEntity processInstance() {
        return new ProcessInstanceEntity(
                42L,
                UUID.randomUUID(),
                7,
                1,
                ProcessInstanceStatus.Running,
                null,
                null,
                List.of("AZ-123"),
                Map.of(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                Map.of(),
                11,
                null,
                null
        );
    }

    private static ProcessInstanceTaskEntity processInstanceTask(String assignedUserId) {
        return new ProcessInstanceTaskEntity(
                null,
                UUID.randomUUID(),
                42L,
                7,
                1,
                11,
                null,
                ProcessTaskStatus.Running,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                Map.of("existing", true),
                Map.of("meta", true),
                Map.of("data", true),
                assignedUserId,
                null,
                null,
                null,
                null
        );
    }

    private static ProcessNodeEntity processNode(String name) {
        return new ProcessNodeEntity()
                .setId(11)
                .setProcessId(7)
                .setProcessVersion(1)
                .setName(name)
                .setDataKey("taskNode")
                .setProcessNodeDefinitionKey("test/task")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());
    }

    private static UserEntity user(String id, String fullName) {
        return new UserEntity()
                .setId(id)
                .setEmail(id + "@example.com")
                .setFirstName(fullName)
                .setLastName(fullName)
                .setFullName(fullName)
                .setEnabled(true)
                .setVerified(true)
                .setDeletedInIdp(false);
    }

    private interface ProxyHandler {
        Object invoke(String methodName, Object[] args);
    }

    private static <T> T proxy(Class<T> type) {
        return proxy(type, (methodName, args) -> null);
    }

    private static <T> T proxy(Class<T> type, ProxyHandler handler) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + "Proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }

                    return handler.invoke(method.getName(), args);
                }
        ));
    }

    private static final class TestUserService extends UserService {
        private final Map<String, UserEntity> users;

        private TestUserService(Map<String, UserEntity> users) {
            super(null, null);
            this.users = users;
        }

        @Override
        public Optional<UserEntity> retrieve(String id) throws ResponseException {
            return Optional.ofNullable(users.get(id));
        }
    }

    private static final class RecordingProcessTaskMailService extends ProcessTaskMailService {
        private int sendCount;
        private boolean lastReassignment;
        private String lastAssignedUserId;
        private boolean throwOnSend;

        private RecordingProcessTaskMailService() {
            super(null, null, null);
        }

        @Override
        public void sendAssigned(UserEntity triggeringUser,
                                 UserEntity assignedUser,
                                 ProcessInstanceEntity processInstance,
                                 ProcessInstanceTaskEntity processInstanceTask,
                                 ProcessNodeEntity currentNode,
                                 ProcessNodeDefinition provider,
                                 boolean isReassignment) {
            sendCount++;
            lastAssignedUserId = assignedUser.getId();
            lastReassignment = isReassignment;

            if (throwOnSend) {
                throw new RuntimeException("mail send failed");
            }
        }
    }

    private static final class RecordingProcessNodeExecutionLogger extends ProcessNodeExecutionLogger {
        private int exceptionCount;

        private RecordingProcessNodeExecutionLogger() {
            super(42L, 9L, null, null, null);
        }

        @Override
        public void logf(ProcessNodeExecutionLogLevel level,
                         Boolean isTechnical,
                         Boolean isAuditable,
                         String format,
                         Object... args) {
        }

        @Override
        public void logException(ProcessNodeExecutionException exception) {
            exceptionCount++;
        }

        @Override
        public void logException(Exception exception) {
            exceptionCount++;
        }
    }

    private static final class TestProcessNodeDefinition implements ProcessNodeDefinition {
        private final String name;

        private TestProcessNodeDefinition(String name) {
            this.name = name;
        }

        @Override
        public String getParentPluginKey() {
            return "test";
        }

        @Override
        public String getComponentKey() {
            return "task";
        }

        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return "Test provider";
        }

        @Override
        public ProcessNodeType getType() {
            return ProcessNodeType.Action;
        }

        @Override
        public List<ProcessNodePort> getPorts() {
            return List.of();
        }

        @Override
        public ProcessNodeExecutionResult init(ProcessNodeExecutionContextInit context) {
            throw new UnsupportedOperationException("Not used in this test");
        }
    }
}
