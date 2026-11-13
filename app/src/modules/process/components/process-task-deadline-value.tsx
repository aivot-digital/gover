import {useDeadlineNow} from '../hooks/use-deadline-now';
import {getProcessTaskDeadlineState, hasActiveTaskDeadline, type ProcessTaskDeadlineSource} from '../utils/process-task-deadline';
import {ProcessDeadlineValue} from './process-deadline-value';

export function ProcessTaskDeadlineValue({task}: {task: ProcessTaskDeadlineSource}) {
    const active = hasActiveTaskDeadline(task);
    const now = useDeadlineNow(active);
    const state = getProcessTaskDeadlineState(task, now);
    return (
        <ProcessDeadlineValue
            deadline={task.deadline}
            now={active ? now : undefined}
            color={state === 'exceeded' ? 'error' : undefined}
            label={state === 'exceeded' ? 'Frist überschritten' : undefined}
            emptyLabel={state === 'exceeded' ? 'Fristzeitpunkt nicht verfügbar' : 'Keine aktive Frist'}
        />
    );
}
