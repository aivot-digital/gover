import {describe, expect, it} from 'vitest';
import type {ProcessNodeProblems} from '../entities/process-node-problems';
import {includeNodeProblems} from './include-node-problems';

describe('includeNodeProblems', () => {
    it('shows fresh validation problems independently of the persisted node error flag', () => {
        const nodeProblems = [
            {
                node: {
                    id: 42,
                    savedWithErrors: false,
                },
            },
        ] as ProcessNodeProblems[];

        expect(includeNodeProblems({7: true}, nodeProblems)).toEqual({
            7: true,
            42: true,
        });
    });
});
