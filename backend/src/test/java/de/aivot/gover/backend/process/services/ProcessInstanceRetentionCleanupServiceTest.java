package de.aivot.gover.backend.process.services;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.repositories.ProcessInstanceAttachmentSetRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.gover.backend.process.services.CaseNumberGeneratorService;
import de.aivot.gover.backend.process.services.ProcessInstanceRetentionCleanupService;
import de.aivot.gover.backend.process.services.ProcessInstanceService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ProcessInstanceRetentionCleanupServiceTest {
    @Test
    void cleanDueProcessInstancesNightly_DeletesAtMostConfiguredBatchSize_AndLogsRemainingBacklog() {
        var pageableReference = new AtomicReference<Pageable>();
        var processInstanceRepository = createProxy(ProcessInstanceRepository.class, (methodName, args) -> switch (methodName) {
            case "countByStatusAndKeepUntilLessThanEqual" -> 7L;
            case "findAllByStatusAndKeepUntilLessThanEqual" -> {
                pageableReference.set((Pageable) args[2]);
                yield List.of(
                        createProcessInstance(1L),
                        createProcessInstance(2L),
                        createProcessInstance(3L)
                );
            }
            default -> null;
        });
        var processInstanceService = new TestProcessInstanceService();
        var service = new ProcessInstanceRetentionCleanupService(
                processInstanceRepository,
                processInstanceService,
                null,
                3
        );

        var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ProcessInstanceRetentionCleanupService.class);
        var listAppender = new ListAppender<ILoggingEvent>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            service.cleanDueProcessInstancesNightly();
        } finally {
            logger.detachAppender(listAppender);
        }

        assertEquals(3, processInstanceService.deletedProcessInstances.size());
        assertEquals(3, pageableReference.get().getPageSize());

        var loggedMessages = listAppender.list
                .stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
        assertTrue(loggedMessages.contains("deleted 3 due instance(s), 4 due instance(s) remain"));
        assertTrue(loggedMessages.contains("batchSize=3"));
    }

    @Test
    void cleanDueProcessInstancesNightly_SkipsQueryAndDeletion_WhenNoDueInstances() {
        var findCalled = new AtomicBoolean(false);
        var processInstanceRepository = createProxy(ProcessInstanceRepository.class, (methodName, args) -> switch (methodName) {
            case "countByStatusAndKeepUntilLessThanEqual" -> 0L;
            case "findAllByStatusAndKeepUntilLessThanEqual" -> {
                findCalled.set(true);
                yield List.of();
            }
            default -> null;
        });
        var processInstanceService = new TestProcessInstanceService();
        var service = new ProcessInstanceRetentionCleanupService(
                processInstanceRepository,
                processInstanceService,
                null,
                5
        );

        service.cleanDueProcessInstancesNightly();

        assertFalse(findCalled.get());
        assertTrue(processInstanceService.deletedProcessInstances.isEmpty());
    }

    private interface ProxyHandler {
        Object invoke(String methodName, Object[] args);
    }

    private static <T> T createProxy(Class<T> type, ProxyHandler handler) {
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

    private static ProcessInstanceEntity createProcessInstance(Long id) {
        return new ProcessInstanceEntity(
                id,
                null,
                UUID.randomUUID().toString(),
                1,
                1,
                ProcessInstanceStatus.Completed,
                null,
                null,
                List.of(),
                new IdentityDataMap(),
                Instant.now().minus(Duration.ofDays(3)),
                Instant.now().minus(Duration.ofDays(1)),
                Instant.now().minus(Duration.ofDays(1)),
                Duration.ofHours(1),
                Map.of(),
                1,
                Instant.now().minus(Duration.ofDays(2)),
                null
        );
    }

    private static final class TestProcessInstanceService extends ProcessInstanceService {
        private final List<ProcessInstanceEntity> deletedProcessInstances = new ArrayList<>();

        private TestProcessInstanceService() {
            super(null, null, mock(ProcessInstanceAttachmentSetRepository.class), null, null, mock(CaseNumberGeneratorService.class));
        }

        @Override
        public ProcessInstanceEntity deleteEntity(ProcessInstanceEntity entity) throws ResponseException {
            deletedProcessInstances.add(entity);
            return entity;
        }
    }
}
