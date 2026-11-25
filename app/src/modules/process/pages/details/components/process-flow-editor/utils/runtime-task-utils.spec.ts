import {getExecutionsForNode} from '../../../../../utils/process-task-executions';
import {describe, expect, it} from 'vitest';
import {ProcessTaskStatus} from '../../../../../enums/process-task-status';
import {type ProcessInstanceTaskEntity} from '../../../../../entities/process-instance-task-entity';
import {
    getLatestTaskForNode,
    getLatestTaskForEdge,
    getTransferredProcessDataComparisonForEdge,
} from './runtime-task-utils';

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
        restartForTaskId: null,
        previousProcessNodeId: null,
        previousProcessNodePortKey: null,
        status: ProcessTaskStatus.Completed,
        statusOverride: null,
        executionSummaryMarkdown: null,
        started: `2025-01-01T00:0${id}:00Z`,
        updated: `2025-01-01T00:0${id}:30Z`,
        finished: `2025-01-01T00:0${id}:45Z`,
        runtime: 1000,
        runtimeData: {},
        nodeData: {},
        processData,
        processDataDiff: {},
        assignedUserId: null,
        assignedCustomerIdentityId: null,
        deadline: null,
        deadlineKey: null,
        deadlineStrict: false,
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
            previousProcessInstanceTaskId: sourceTask.id,
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
            previousProcessInstanceTaskId: sourceTask.id,
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
            previousProcessInstanceTaskId: sourceTask.id,
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


describe('node executions', () => {
    it('sorts by start and ID while excluding replaced attempts and other nodes', () => {
        const first = createTask(1, 10, {}, {updated: '2030-01-01T00:00:00Z'});
        const last = createTask(4, 10, {}, {started: '2025-01-01T00:02:00Z'});
        const second = createTask(2, 10, {}, {started: last.started});
        const tasks = [last, createTask(3, 10, {}, {status: ProcessTaskStatus.Restarted}), second, first, createTask(5, 20, {})];
        expect(getExecutionsForNode(tasks, 10).map(task => task.id)).toEqual([1, 2, 4]);
        expect(tasks[0]).toBe(last);
    });

    it.each([false, true])('keeps a restarted attempt as the node state until its replacement exists (older loop: %s)', hasOlderLoop => {
        const restarted = createTask(2, 10, {attempt: 'restarting'}, {status: ProcessTaskStatus.Restarted});
        const tasks = [restarted, ...(hasOlderLoop ? [createTask(1, 10, {attempt: 'older loop'})] : [])];

        expect(getLatestTaskForNode(tasks, 10)).toBe(restarted);
        expect(getExecutionsForNode(tasks, 10)).not.toContain(restarted);

        const replacement = createTask(3, 10, {attempt: 'replacement'}, {
            status: ProcessTaskStatus.Running,
            restartForTaskId: restarted.id,
        });
        expect(getLatestTaskForNode([replacement, ...tasks], 10)).toBe(replacement);
    });

    it('uses the ID to order a replacement with the same start time and ignores later updates of older attempts', () => {
        const restarted = createTask(1, 10, {}, {
            status: ProcessTaskStatus.Restarted,
            updated: '2030-01-01T00:00:00Z',
        });
        const replacement = createTask(2, 10, {}, {
            started: restarted.started,
            restartForTaskId: restarted.id,
        });

        expect(getLatestTaskForNode([replacement, restarted, createTask(3, 20, {})], 10)).toBe(replacement);
    });

    it('selects the last traversal by start rather than a later update of an old traversal', () => {
        const first = createTask(1, 20, {}, {
            previousProcessNodeId: 10,
            previousProcessNodePortKey: 'next',
            updated: '2030-01-01T00:00:00Z',
        });
        const last = createTask(2, 20, {}, {
            previousProcessNodeId: 10,
            previousProcessNodePortKey: 'next',
        });
        expect(getLatestTaskForEdge([last, first], 10, 20, 'next')).toBe(last);
    });

    it('uses the referenced source even when another loop execution finished later', () => {
        const source = createTask(1, 10, {correct: true});
        const unrelated = createTask(2, 10, {wrong: true});
        const target = createTask(3, 20, {}, {
            previousProcessInstanceTaskId: source.id,
            previousProcessNodeId: 10,
            previousProcessNodePortKey: 'next',
        });
        expect(getTransferredProcessDataComparisonForEdge([source, unrelated, target], 10, 20, 'next')?.currentValue).toEqual({correct: true});
    });

    it('does not guess an unloaded source from timestamps', () => {
        const source = createTask(1, 10, {wrong: true});
        const target = createTask(2, 20, {}, {
            previousProcessInstanceTaskId: 99,
            previousProcessNodeId: 10,
            previousProcessNodePortKey: 'next',
        });
        expect(getTransferredProcessDataComparisonForEdge([source, target], 10, 20, 'next')).toBeNull();
    });
});
