import {type ReactNode} from 'react';
import {Box} from '@mui/material';
import {Chip} from '../../../components/chip/chip';
import {type ProcessInstanceEntity} from '../entities/process-instance-entity';
import {
    getProcessInstanceDeadlineState,
    ProcessInstanceDeadlineStateColors,
    ProcessInstanceDeadlineStateLabels,
} from '../utils/process-instance-deadline';
import {formatDateTimeWithRelative, ProcessEmptyValue} from './process-detail-values';

interface ProcessInstanceDeadlineValueProps {
    instance: Pick<ProcessInstanceEntity, 'status' | 'started' | 'deadline'>;
}

export function ProcessInstanceDeadlineValue({instance}: ProcessInstanceDeadlineValueProps): ReactNode {
    if (instance.deadline == null) {
        return <ProcessEmptyValue>Keine Bearbeitungsfrist festgelegt</ProcessEmptyValue>;
    }

    const state = getProcessInstanceDeadlineState(instance);
    const color = state == null ? undefined : ProcessInstanceDeadlineStateColors[state];

    return (
        <Box
            component="span"
            sx={{display: 'inline-flex', alignItems: 'center', flexWrap: 'wrap', columnGap: 1, rowGap: 0.5}}
        >
            {formatDateTimeWithRelative(instance.deadline)}
            {state != null && color != null && (
                <Chip
                    label={ProcessInstanceDeadlineStateLabels[state]}
                    size="small"
                    mode="soft"
                    color={color}
                />
            )}
        </Box>
    );
}
