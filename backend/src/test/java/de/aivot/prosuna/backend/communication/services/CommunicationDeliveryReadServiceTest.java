package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.entities.CommunicationDeliveryEntity;
import de.aivot.prosuna.backend.communication.models.CommunicationDeliveryStatus;
import de.aivot.prosuna.backend.communication.permissions.CommunicationProviderPermissionProvider;
import de.aivot.prosuna.backend.communication.repositories.CommunicationDeliveryRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommunicationDeliveryReadServiceTest {
    private final CommunicationDeliveryRepository deliveries = mock(CommunicationDeliveryRepository.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final VUserSystemPermissionRepository systemPermissions = mock(VUserSystemPermissionRepository.class);
    private final PermissionService permissions = new PermissionService(null, null, systemPermissions, null, null, null, instances);
    private final CommunicationDeliveryReadService service = new CommunicationDeliveryReadService(deliveries, tasks, permissions);
    private final Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("user").build();
    private CommunicationDeliveryEntity delivery;

    @BeforeEach
    void setup() {
        delivery = new CommunicationDeliveryEntity().setId(UUID.randomUUID()).setProviderId(7).setTaskId(2L)
                .setStatus(CommunicationDeliveryStatus.Submitted).setReceipt(Map.of("submissionId", "id")).setUpdated(Instant.now());
        when(tasks.findById(2L)).thenReturn(Optional.of(new ProcessInstanceTaskEntity().setId(2L).setProcessInstanceId(17L)));
        when(tasks.findById(3L)).thenReturn(Optional.of(new ProcessInstanceTaskEntity().setId(3L).setProcessInstanceId(18L)));
        when(deliveries.findByTaskId(2L)).thenReturn(Optional.of(delivery));
        when(deliveries.findById(delivery.getId())).thenReturn(Optional.of(delivery));
    }

    @Test
    void allowsCorrectResourceAndDeniesOtherResource() throws Exception {
        when(instances.hasPermission("user", 17L, ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ)).thenReturn(true);
        assertEquals(delivery.getId(), service.forTask(jwt, 2L).id());
        assertThrows(ResponseException.class, () -> service.forTask(jwt, 3L));
        verify(deliveries, never()).findByTaskId(3L);
    }

    @Test
    void systemGrantOverridesResourceRestriction() throws Exception {
        when(systemPermissions.hasPermission("user", ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ)).thenReturn(true);
        assertNotNull(service.forTask(jwt, 2L));
    }

    @Test
    void missingGrantAndAnonymousAccessAreDenied() {
        assertThrows(ResponseException.class, () -> service.forTask(jwt, 2L));
        assertThrows(ResponseException.class, () -> service.forTask(null, 2L));
        verify(deliveries, never()).findByTaskId(2L);
    }

    @Test
    void providerReaderCannotUseTestEndpointToReadProcessDeliveriesOrAnotherProvider() throws Exception {
        when(systemPermissions.hasPermission("user", CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ)).thenReturn(true);
        assertThrows(ResponseException.class, () -> service.forTest(jwt, 7, delivery.getId()));
        delivery.setTaskId(null);
        assertThrows(ResponseException.class, () -> service.forTest(jwt, 8, delivery.getId()));
        assertEquals(delivery.getId(), service.forTest(jwt, 7, delivery.getId()).id());
    }
}
