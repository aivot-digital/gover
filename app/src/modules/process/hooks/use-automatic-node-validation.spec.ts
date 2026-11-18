import {act, renderHook} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {type ProcessDefinitionEdgeEntity} from '../entities/process-definition-edge-entity';
import {type ProcessNodeEntity} from '../entities/process-node-entity';
import {type ProcessNodeProblems} from '../entities/process-node-problems';
import {type ProcessVersionProblems} from '../entities/process-version-problems';
import {ProcessDefinitionVersionApiService} from '../services/process-definition-version-api-service';
import {useAutomaticNodeValidation} from './use-automatic-node-validation';

function deferred<T>() {
    let resolve!: (value: T) => void;
    const promise = new Promise<T>((r) => {
        resolve = r;
    });
    return {promise, resolve};
}

describe('useAutomaticNodeValidation', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('applies only the latest validation when the responses arrive out of order', async () => {
        const olderValidation = deferred<ProcessVersionProblems>();
        const newerValidation = deferred<ProcessVersionProblems>();
        vi.spyOn(ProcessDefinitionVersionApiService.prototype, 'validate')
            .mockReturnValueOnce(olderValidation.promise)
            .mockReturnValueOnce(newerValidation.promise);
        const setProcessNodeProblems = vi.fn();
        const setShowProcessNodeProblemsForNodes = vi.fn();
        const nodes = [{id: 1, savedWithErrors: true}] as ProcessNodeEntity[];
        const olderProblems = [{node: {id: 1}, problems: ['Alter Fehler.']}] as ProcessNodeProblems[];
        const newerProblems = [{node: {id: 1}, problems: ['Neuer Fehler.']}] as ProcessNodeProblems[];

        const {rerender} = renderHook(
            ({edges}) => useAutomaticNodeValidation(1, 2, {nodes, edges}, setProcessNodeProblems, setShowProcessNodeProblemsForNodes),
            {initialProps: {edges: [] as ProcessDefinitionEdgeEntity[]}},
        );
        // A new edge triggers a second validation while the first one is still pending.
        rerender({edges: [{id: 5}] as ProcessDefinitionEdgeEntity[]});

        await act(async () => {
            newerValidation.resolve({versionProblems: [], nodeProblems: newerProblems});
        });
        await act(async () => {
            olderValidation.resolve({versionProblems: [], nodeProblems: olderProblems});
        });

        expect(setProcessNodeProblems).toHaveBeenCalledTimes(1);
        expect(setProcessNodeProblems).toHaveBeenCalledWith(newerProblems);
        expect(setShowProcessNodeProblemsForNodes).toHaveBeenCalledTimes(1);
    });
});
