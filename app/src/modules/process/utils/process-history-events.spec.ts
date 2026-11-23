import {describe, expect, it} from 'vitest';
import {ProcessTaskStatus} from '../enums/process-task-status';
import {ProcessNodeExecutionLogLevel, type ProcessInstanceHistoryEvent} from '../entities/process-instance-event-entity';
import {type ProcessInstanceTaskEntity} from '../entities/process-instance-task-entity';
import {ProcessInstanceEventApiService} from '../services/process-instance-event-api-service';
import {ProcessInstanceTaskApiService} from '../services/process-instance-task-api-service';
import {getTaskHistoryEvents} from './process-history-events';

function task(id: number, overrides: Partial<ProcessInstanceTaskEntity> = {}): ProcessInstanceTaskEntity {
    return {...new ProcessInstanceTaskApiService().initialize(), id, processInstanceId: 17, processNodeId: 5,
        status: ProcessTaskStatus.Restarted, ...overrides};
}

function event(id: number, taskId: number, manual = true, overrides: Partial<ProcessInstanceHistoryEvent> = {}): ProcessInstanceHistoryEvent {
    return {...new ProcessInstanceEventApiService().initialize(), id, processInstanceId: 17, processInstanceTaskId: taskId,
        details: manual ? {manualIntervention: true} : {}, timestamp: '2026-11-23T10:00:00Z', ...overrides};
}

function collect(current: ProcessInstanceTaskEntity, tasks: ProcessInstanceTaskEntity[], events: ProcessInstanceHistoryEvent[]) {
    return getTaskHistoryEvents(current, new Map(tasks.map(task => [task.id, task])), events);
}

describe('task history events across restart attempts', () => {
    it('inherits only explicit manual interventions and preserves event ownership and timestamps', () => {
        const previous = task(42);
        const current = task(43, {status: ProcessTaskStatus.Running, restartForTaskId: 42});
        const intervention = event(1, 42);
        const original = {...intervention};
        const result = collect(current, [previous, current], [intervention, event(2, 42, false, {level: ProcessNodeExecutionLogLevel.Error, historyRelevant: true}), event(3, 43, false)]);
        expect(result.map(item => [item.id, item.fromPreviousAttempt])).toEqual([[1, true], [3, false]]);
        expect(result[0]).toMatchObject(original);
        expect(intervention).toEqual(original);
        expect(intervention).not.toHaveProperty('fromPreviousAttempt');
    });

    it('follows multiple restarts, deduplicates events and sorts by timestamp and ID', () => {
        const first = task(40);
        const previous = task(42, {restartForTaskId: 40});
        const current = task(43, {status: ProcessTaskStatus.Completed, restartForTaskId: 42});
        const oldest = event(3, 40, true, {timestamp: '2026-11-23T09:00:00Z'});
        const result = collect(current, [first, previous, current], [event(4, 43, false), event(2, 42), oldest, oldest, event(1, 42)]);
        expect(result.map(item => item.id)).toEqual([3, 1, 2, 4]);
        expect(result.filter(item => item.fromPreviousAttempt)).toHaveLength(3);
    });

    it('does not follow ordinary workflow predecessors, even when the node is the same', () => {
        const previous = task(42, {status: ProcessTaskStatus.Completed});
        const current = task(43, {status: ProcessTaskStatus.Running, previousProcessInstanceTaskId: 42});
        expect(collect(current, [previous, current], [event(1, 42), event(2, 43, false)]).map(item => item.id)).toEqual([2]);
    });

    it('does not inherit unrelated replaced attempts of the same node', () => {
        const current = task(43, {status: ProcessTaskStatus.Running});
        expect(collect(current, [task(42), current], [event(1, 42)])).toEqual([]);
    });

    it.each([
        {processInstanceId: 99}, {processNodeId: 99}, {status: ProcessTaskStatus.Completed},
    ])('stops at an invalid restart reference (%j)', overrides => {
        const previous = task(42, overrides);
        const current = task(43, {status: ProcessTaskStatus.Running, restartForTaskId: 42});
        expect(collect(current, [previous, current], [event(1, 42), event(2, 43, false)]).map(item => item.id)).toEqual([2]);
    });

    it('keeps available events if an earlier attempt is missing', () => {
        const previous = task(42, {restartForTaskId: 99});
        const current = task(43, {status: ProcessTaskStatus.Running, restartForTaskId: 42});
        expect(collect(current, [previous, current], [event(1, 42), event(2, 99)]).map(item => item.id)).toEqual([1]);
    });

    it('terminates cyclic restart references without duplicating current events', () => {
        const previous = task(42, {restartForTaskId: 43});
        const current = task(43, {status: ProcessTaskStatus.Running, restartForTaskId: 42});
        expect(collect(current, [previous, current], [event(1, 42), event(2, 43, false)]).map(item => [item.id, item.fromPreviousAttempt])).toEqual([[1, true], [2, false]]);
        expect(collect({...current, restartForTaskId: 43}, [current], [event(2, 43, false)])).toHaveLength(1);
    });

    it('ignores events belonging to a different instance', () => {
        const current = task(43, {status: ProcessTaskStatus.Running});
        expect(collect(current, [current], [event(1, 43, true, {processInstanceId: 99})])).toEqual([]);
    });
});
