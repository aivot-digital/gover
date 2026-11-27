package de.aivot.prosuna.backend.process.entities;

import de.aivot.prosuna.backend.staffEvents.models.StaffEvent;
import jakarta.annotation.Nonnull;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Tells the staff browsers that a process instance changed, so the open views of the instance, its tasks and the
 * lists reload. It is registered on the instance and on the rows of the instance that these views show, so every
 * write through JPA is covered, whichever service or worker makes it. Hibernate creates the listener through Spring.
 */
public class ProcessInstanceStaffEventListener {
    public static final String STAFF_EVENT_TYPE = "process-instance";

    private final ApplicationEventPublisher events;

    public ProcessInstanceStaffEventListener(@Nonnull ApplicationEventPublisher events) {
        this.events = events;
    }

    @PostPersist
    @PostUpdate
    public void changed(@Nonnull Object entity) {
        publish(entity);
    }

    @PostRemove
    public void removed(@Nonnull Object entity) {
        publish(entity);
        // The readers of a deleted instance can no longer be determined once it is gone, so only system-wide readers
        // would receive its event. An event without an instance lets the lists of all users reload instead.
        if (entity instanceof ProcessInstanceEntity) {
            events.publishEvent(new StaffEvent(STAFF_EVENT_TYPE, null, null));
        }
    }

    private void publish(@Nonnull Object entity) {
        var instanceId = switch (entity) {
            case ProcessInstanceEntity instance -> instance.getId();
            case ProcessInstanceTaskEntity task -> task.getProcessInstanceId();
            case ProcessInstanceAttachmentEntity attachment -> attachment.getProcessInstanceId();
            case ProcessInstanceAttachmentSetEntity attachmentSet -> attachmentSet.getProcessInstanceId();
            default -> throw new IllegalArgumentException("Unsupported entity " + entity.getClass().getName());
        };
        events.publishEvent(new StaffEvent(STAFF_EVENT_TYPE, instanceId.toString(), null));
    }
}
