package de.aivot.prosuna.backend.process.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProcessRuntimeControllerEndpointTest {
    @Test
    void processInstancesExposeOnlyExplicitWriteCommands() {
        assertFalse(hasMethodWithAnnotation(ProcessInstanceController.class, PostMapping.class));
        assertEquals(
                Set.of("{id}/reassign/", "{id}/restart-failed/"),
                putMappings(ProcessInstanceController.class)
        );
    }

    @Test
    void processInstanceTasksExposeOnlyTheFailedTaskRestartCommand() {
        assertFalse(hasMethodWithAnnotation(ProcessInstanceTaskController.class, PostMapping.class));
        assertFalse(hasMethodWithAnnotation(ProcessInstanceTaskController.class, DeleteMapping.class));
        assertEquals(Set.of("{id}/rerun-failed/"), putMappings(ProcessInstanceTaskController.class));
    }

    @Test
    void processInstanceEventsAreReadOnly() {
        assertFalse(hasMethodWithAnnotation(ProcessInstanceEventController.class, PostMapping.class));
        assertFalse(hasMethodWithAnnotation(ProcessInstanceEventController.class, PutMapping.class));
        assertFalse(hasMethodWithAnnotation(ProcessInstanceEventController.class, DeleteMapping.class));
    }

    @Test
    void processInstanceAttachmentsAreReadOnly() {
        assertFalse(hasMethodWithAnnotation(ProcessInstanceAttachmentController.class, PostMapping.class));
        assertFalse(hasMethodWithAnnotation(ProcessInstanceAttachmentController.class, PutMapping.class));
        assertFalse(hasMethodWithAnnotation(ProcessInstanceAttachmentController.class, DeleteMapping.class));
    }

    private static boolean hasMethodWithAnnotation(Class<?> controller,
                                                   Class<? extends java.lang.annotation.Annotation> annotation) {
        return Arrays.stream(controller.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(annotation));
    }

    private static Set<String> putMappings(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .map(method -> method.getAnnotation(PutMapping.class))
                .filter(java.util.Objects::nonNull)
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .collect(Collectors.toSet());
    }
}
