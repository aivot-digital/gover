import {describe, expect, it} from 'vitest';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {ProcessDefinitionVersionApiService} from '../../services/process-definition-version-api-service';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {applySavedProcessNode} from './apply-saved-process-node';
import {type ProcessFlow} from './process-details-page';

const nodeA = {...ProcessNodeApiService.initialize(), id: 1, processId: 10, processVersion: 3, lockVersion: 4};
const nodeB = {...nodeA, id: 2, lockVersion: 1};
const savedA = {...nodeA, name: 'Gespeichert', lockVersion: 5};
const flow: ProcessFlow = {
    definition: {...ProcessDefinitionApiService.initialize(), id: 10},
    version: {...ProcessDefinitionVersionApiService.initialize(), processId: 10, processVersion: 3},
    nodes: [nodeA, nodeB],
    edges: [],
};

describe('applySavedProcessNode', () => {
    it('keeps changes to another node and the edges made while a save was pending', () => {
        const newerB = {...nodeB, notes: 'Zwischenzeitlich geändert', lockVersion: 2};
        const edge = {id: 17, processId: 10, processVersion: 3, fromNodeId: 1, toNodeId: 2, viaPort: 'next'};
        const current = {...flow, nodes: [nodeA, newerB], edges: [edge]};

        expect(applySavedProcessNode(current, savedA)).toEqual({...current, nodes: [savedA, newerB]});
        expect(current.nodes).toEqual([nodeA, newerB]);
    });

    it('ignores responses for a different process or version and for deleted nodes', () => {
        expect(applySavedProcessNode(null, savedA)).toBeNull();
        expect(applySavedProcessNode(flow, {...savedA, processId: 11})).toBe(flow);
        expect(applySavedProcessNode(flow, {...savedA, processVersion: 2})).toBe(flow);
        const afterDeletion = {...flow, nodes: [nodeB]};
        expect(applySavedProcessNode(afterDeletion, savedA)).toBe(afterDeletion);
    });

    it('keeps a newer lock version even when an earlier response has no version', () => {
        const newer = {...flow, nodes: [{...nodeA, lockVersion: 6}, nodeB]};
        expect(applySavedProcessNode(newer, savedA)).toBe(newer);
        expect(applySavedProcessNode(newer, {...savedA, lockVersion: null})).toBe(newer);
        const unversioned = {...flow, nodes: [{...nodeA, lockVersion: null}, nodeB]};
        expect(applySavedProcessNode(unversioned, savedA)?.nodes).toEqual([savedA, nodeB]);
    });
});
