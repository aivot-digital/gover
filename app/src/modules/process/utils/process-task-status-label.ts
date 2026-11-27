import {type IdentityDataMap} from '../../identity/models/identity-data';
import {ProcessTaskStatus, ProcessTaskStatusLabels} from '../enums/process-task-status';

export interface ProcessTaskStatusLabelSource {
    status: ProcessTaskStatus;
    statusOverride?: string | null;
    assignedCustomerIdentityId?: string | null;
    assignedCustomerIdentityLabel?: string | null;
}

export interface ProcessTaskStatusDisplay {
    label: string;
    systemLabel: string;
    identityLabel: string | null;
    tooltip?: string;
}

export function getProcessTaskStatusDisplay(
    task: ProcessTaskStatusLabelSource,
    identities?: IdentityDataMap | null,
): ProcessTaskStatusDisplay {
    let identityLabel: string | null = null;
    if (task.status === ProcessTaskStatus.AwaitingCustomer) {
        const identityId = task.assignedCustomerIdentityId;
        identityLabel = task.assignedCustomerIdentityLabel?.trim() || null;
        if (!identityLabel && identityId?.trim()) {
            const identity = Object.values(identities ?? {}).find(value => value.identityId === identityId);
            identityLabel = identity?.title?.trim() || identityId.trim();
        }
    }

    const systemLabel = identityLabel ? `Wartet auf ${identityLabel}` : ProcessTaskStatusLabels[task.status];
    const label = task.statusOverride?.trim() || systemLabel;
    const tooltip = label !== systemLabel
        ? `${label} (Systemstatus: ${systemLabel})`
        : identityLabel ? systemLabel : undefined;

    return {label, systemLabel, identityLabel, tooltip};
}
