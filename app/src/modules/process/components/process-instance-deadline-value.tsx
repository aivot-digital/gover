import {type ReactNode} from 'react';
import {type ProcessInstanceEntity} from '../entities/process-instance-entity';
import {
    getProcessInstanceDeadlineState,
    ProcessInstanceDeadlineStateColors,
    ProcessInstanceDeadlineStateLabels,
} from '../utils/process-instance-deadline';
import {ProcessDeadlineValue} from './process-deadline-value';
import {useDeadlineNow} from '../hooks/use-deadline-now';

interface ProcessInstanceDeadlineValueProps {
    instance: Pick<ProcessInstanceEntity, 'status' | 'started' | 'deadline'>;
}

export function ProcessInstanceDeadlineValue({instance}: ProcessInstanceDeadlineValueProps): ReactNode {
    const active = getProcessInstanceDeadlineState(instance) != null;
    const now = useDeadlineNow(active);
    const state = getProcessInstanceDeadlineState(instance, now);
    const color = state == null ? undefined : ProcessInstanceDeadlineStateColors[state];

    return (
        <ProcessDeadlineValue
            deadline={instance.deadline}
            now={active ? now : undefined}
            color={color}
            label={state == null ? undefined : ProcessInstanceDeadlineStateLabels[state]}
            emptyLabel="Keine Bearbeitungsfrist festgelegt"
        />
    );
}
