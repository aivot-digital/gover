import {describe, expect, it} from 'vitest';
import {
    createEmptyProcessTaskDetailsPageItem,
    getProcessTaskStatusColor,
    getProcessTaskStatusLabel,
} from './process-task-view-page';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {ProcessInstanceStatus} from '../../enums/process-instance-status';
import {ProcessTaskStatus} from '../../enums/process-task-status';

describe('Task header status', () => {
    it.each([
        [null, 'Wartet auf Antragstellende'],
        [' Unterlagen angefordert ', 'Unterlagen angefordert'],
    ])('uses the stored identity title and custom status in the header (%s)', (statusOverride, expected) => {
        const item = createEmptyProcessTaskDetailsPageItem();
        item.task.status = ProcessTaskStatus.AwaitingCustomer;
        item.task.statusOverride = statusOverride;
        item.task.assignedCustomerIdentityId = 'applicant';
        item.instance = new ProcessInstanceApiService().initialize();
        item.instance.identities = {
            applicant: {
                identityId: 'applicant', title: 'Antragstellende', sessionId: 'session', type: 'Email',
                emailAddress: 'person@example.org', attributes: {}, providerKey: null, metadataIdentifier: null,
                uniqueIdFromIdentityProvider: null, communicationProviderBindingId: null, communicationProviderData: {},
            },
        };
        expect(getProcessTaskStatusLabel(item)).toBe(expected);
    });

    it('falls back to the identity ID before instance details are available', () => {
        const item = createEmptyProcessTaskDetailsPageItem();
        item.task.status = ProcessTaskStatus.AwaitingCustomer;
        item.task.assignedCustomerIdentityId = 'applicant';
        expect(getProcessTaskStatusLabel(item)).toBe('Wartet auf applicant');
    });

    it('distinguishes started tasks from staff tasks with saved work', () => {
        const item = createEmptyProcessTaskDetailsPageItem();
        item.task.status = ProcessTaskStatus.Running;
        expect(getProcessTaskStatusLabel(item)).toBe('Gestartet');
        expect(getProcessTaskStatusColor(item)).toBe('info');

        item.task.status = ProcessTaskStatus.AwaitingStaff;
        expect(getProcessTaskStatusLabel(item)).toBe('Wartet auf Bearbeitung');
        expect(getProcessTaskStatusColor(item)).toBe('warning');

        item.task.status = ProcessTaskStatus.InProgress;
        expect(getProcessTaskStatusLabel(item)).toBe('In Bearbeitung');
        expect(getProcessTaskStatusColor(item)).toBe('primary');
    });

    it('uses the task status even when its instance has a different status or custom label', () => {
        const item = createEmptyProcessTaskDetailsPageItem();
        item.instance = {
            ...new ProcessInstanceApiService().initialize(),
            status: ProcessInstanceStatus.Running,
            statusOverride: 'Gesamtprüfung',
        };
        item.task.status = ProcessTaskStatus.Failed;
        expect(getProcessTaskStatusLabel(item)).toBe('Fehlgeschlagen');
        expect(getProcessTaskStatusColor(item)).toBe('error');
        item.task.statusOverride = '  Fachliche Klärung  ';
        expect(getProcessTaskStatusLabel(item)).toBe('Fachliche Klärung');
        expect(getProcessTaskStatusColor(item)).toBe('error');
    });

    it.each([null, '', '   '])('falls back to the system status for an empty custom label (%s)', (statusOverride) => {
        const item = createEmptyProcessTaskDetailsPageItem();
        item.task.status = ProcessTaskStatus.AwaitingPayment;
        item.task.statusOverride = statusOverride;
        expect(getProcessTaskStatusLabel(item)).toBe('Wartet auf Zahlungsbestätigung');
        expect(getProcessTaskStatusColor(item)).toBe('warning');
    });
});
