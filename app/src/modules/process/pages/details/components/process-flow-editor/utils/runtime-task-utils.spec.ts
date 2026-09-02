import {describe, expect, it} from 'vitest';
import {ProcessTaskStatus} from '../../../../../enums/process-task-status';
import {type ProcessInstanceTaskEntity} from '../../../../../entities/process-instance-task-entity';
import {getTransferredProcessDataComparisonForEdge} from './runtime-task-utils';

function createTask(
    id: number,
    processNodeId: number,
    processData: Record<string, any>,
    overrides: Partial<ProcessInstanceTaskEntity> = {},
): ProcessInstanceTaskEntity {
    return {
        id,
        accessKey: `task-${id}`,
        processInstanceId: 1,
        processId: 1,
        processVersion: 1,
        processNodeId,
        previousProcessInstanceTaskId: null,
        previousProcessNodeId: null,
        previousProcessNodePortKey: null,
        status: ProcessTaskStatus.Completed,
        statusOverride: null,
        started: `2025-01-01T00:0${id}:00Z`,
        updated: `2025-01-01T00:0${id}:30Z`,
        finished: `2025-01-01T00:0${id}:45Z`,
        runtime: 1000,
        runtimeData: {},
        nodeData: {},
        processData,
        processDataDiff: {},
        assignedUserId: null,
        deadline: null,
        ...overrides,
    };
}

describe('getTransferredProcessDataComparisonForEdge', () => {
    it('returns complete snapshots from the source task and its predecessor', () => {
        const previousTask = createTask(1, 5, {status: 'old'});
        const sourceTask = createTask(2, 10, {status: 'new'}, {
            previousProcessInstanceTaskId: previousTask.id,
        });
        const targetTask = createTask(3, 20, {status: 'new'}, {
            previousProcessNodeId: 10,
            previousProcessNodePortKey: 'success',
        });

        expect(getTransferredProcessDataComparisonForEdge(
            [previousTask, sourceTask, targetTask],
            10,
            20,
            'success',
        )).toEqual({
            currentValue: {status: 'new'},
            previousValue: {status: 'old'},
        });
    });

    it('compares the first source task against an empty initial data object', () => {
        const sourceTask = createTask(1, 10, {created: true});
        const targetTask = createTask(2, 20, {created: true}, {
            previousProcessNodeId: 10,
            previousProcessNodePortKey: 'success',
        });

        expect(getTransferredProcessDataComparisonForEdge(
            [sourceTask, targetTask],
            10,
            20,
            'success',
        )).toEqual({
            currentValue: {created: true},
            previousValue: {},
        });
    });

    it('omits the comparison when the referenced predecessor was not loaded', () => {
        const sourceTask = createTask(2, 10, {status: 'new'}, {
            previousProcessInstanceTaskId: 99,
        });
        const targetTask = createTask(3, 20, {status: 'new'}, {
            previousProcessNodeId: 10,
            previousProcessNodePortKey: 'success',
        });

        expect(getTransferredProcessDataComparisonForEdge(
            [sourceTask, targetTask],
            10,
            20,
            'success',
        )).toEqual({
            currentValue: {status: 'new'},
            previousValue: null,
        });
    });
});
