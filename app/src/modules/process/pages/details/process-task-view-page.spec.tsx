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
