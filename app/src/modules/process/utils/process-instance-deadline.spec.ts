import {describe, expect, it} from 'vitest';
import {ProcessInstanceStatus} from '../enums/process-instance-status';
import {getProcessInstanceDeadlineState} from './process-instance-deadline';

const started = '2026-10-01T08:00:00Z';
const deadline = '2026-10-11T08:00:00Z';
const at = (value: string) => Date.parse(value);

describe('getProcessInstanceDeadlineState', () => {
    it('stays open until half of the period has passed', () => {
        const instance = {status: ProcessInstanceStatus.Running, started, deadline};

        expect(getProcessInstanceDeadlineState(instance, at('2026-10-01T08:00:00Z'))).toBe('open');
        expect(getProcessInstanceDeadlineState(instance, at('2026-10-06T07:59:59Z'))).toBe('open');
    });

    it('warns from the halfway point until the deadline', () => {
        const instance = {status: ProcessInstanceStatus.Running, started, deadline};

        expect(getProcessInstanceDeadlineState(instance, at('2026-10-06T08:00:00Z'))).toBe('warning');
        expect(getProcessInstanceDeadlineState(instance, at('2026-10-11T07:59:59Z'))).toBe('warning');
    });

    it('reports the deadline as exceeded once it is reached', () => {
        const instance = {status: ProcessInstanceStatus.Paused, started, deadline};

        expect(getProcessInstanceDeadlineState(instance, at('2026-10-11T08:00:00Z'))).toBe('exceeded');
        expect(getProcessInstanceDeadlineState(instance, at('2027-01-01T00:00:00Z'))).toBe('exceeded');
    });

    it.each([
        ProcessInstanceStatus.Completed,
        ProcessInstanceStatus.Aborted,
        ProcessInstanceStatus.Failed,
    ])('does not evaluate instances with status %s', (status) => {
        expect(getProcessInstanceDeadlineState({status, started, deadline}, at('2027-01-01T00:00:00Z'))).toBeNull();
    });

    it('does not evaluate instances without a deadline', () => {
        expect(getProcessInstanceDeadlineState({
            status: ProcessInstanceStatus.Created,
            started,
            deadline: null,
        }, at('2027-01-01T00:00:00Z'))).toBeNull();
    });
});
