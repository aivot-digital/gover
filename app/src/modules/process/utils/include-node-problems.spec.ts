import {describe, expect, it} from 'vitest';
import type {ProcessNodeProblems} from '../entities/process-node-problems';
import {includeNodeProblems} from './include-node-problems';

describe('includeNodeProblems', () => {
    it('only reveals current problems for nodes saved with errors', () => {
        const nodeProblems = [
            {
                node: {
                    id: 42,
                    savedWithErrors: false,
                },
            },
            {
                node: {
                    id: 43,
                    savedWithErrors: true,
                },
            },
        ] as ProcessNodeProblems[];

        expect(includeNodeProblems({7: true}, nodeProblems, [
            {id: 42, savedWithErrors: false},
            {id: 43, savedWithErrors: true},
            {id: 44, savedWithErrors: true},
        ])).toEqual({
            7: true,
            43: true,
        });
    });

    it('keeps problems revealed by an explicit action', () => {
        const nodeProblems = [{node: {id: 42}}] as ProcessNodeProblems[];

        expect(includeNodeProblems({42: true}, nodeProblems, [
            {id: 42, savedWithErrors: false},
        ])).toEqual({42: true});
    });
});
