package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.models.CommunicationDeliveryView;
import de.aivot.prosuna.backend.communication.permissions.CommunicationProviderPermissionProvider;
import de.aivot.prosuna.backend.communication.repositories.CommunicationDeliveryRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import java.util.Objects;
import java.util.UUID;

@Service
public class CommunicationDeliveryReadService {
    private final CommunicationDeliveryRepository deliveries;
    private final ProcessInstanceTaskRepository tasks;
    private final PermissionService permissions;

    public CommunicationDeliveryReadService(CommunicationDeliveryRepository deliveries,
                                            ProcessInstanceTaskRepository tasks, PermissionService permissions) {
        this.deliveries = deliveries;
        this.tasks = tasks;
        this.permissions = permissions;
    }

    @Nullable
    public CommunicationDeliveryView forTask(@Nullable Jwt jwt, @Nonnull Long taskId) throws ResponseException {
        var userId = UserService.getIdFromJWT(jwt);
        if (userId == null) throw ResponseException.unauthorized();
        var task = tasks.findById(taskId).orElseThrow(ResponseException::notFound);
        permissions.requireProcessInstancePermission(userId, task.getProcessInstanceId(), ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);
        return deliveries.findByTaskId(taskId).map(CommunicationDeliveryView::from).orElse(null);
    }

    @Nonnull
    public CommunicationDeliveryView forTest(@Nullable Jwt jwt, @Nonnull Integer providerId, @Nonnull UUID id) throws ResponseException {
        permissions.requireSystemPermission(jwt, CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ);
        var delivery = deliveries.findById(id).orElseThrow(ResponseException::notFound);
        if (delivery.getTaskId() != null || !Objects.equals(providerId, delivery.getProviderId())) throw ResponseException.notFound();
        return CommunicationDeliveryView.from(delivery);
    }
}
