import {describe, expect, it} from 'vitest';
import {type ProcessNodeDefinitionMetadataForwardedProcessDataKey} from '../../process/entities/process-node-definition-metadata';
import {findForwardedProcessDataKeys} from './use-process-data-reference-options';

describe('findForwardedProcessDataKeys', () => {
    it('matches the canonical form of a referenced path', () => {
        const key = forwardedKey('people[0].status');

        expect(findForwardedProcessDataKeys([forwardedKey('status'), key], ' people [ 0 ] . status ')).toEqual([key]);
    });

    it('matches wildcard rows for a concrete index', () => {
        const key = forwardedKey('people[*].addresses[*].type');

        expect(findForwardedProcessDataKeys([key], 'people[1].addresses[0].type')).toEqual([key]);
    });

    it('prefers exact matches over wildcard rows', () => {
        const exactKey = forwardedKey('people[0].status');

        expect(findForwardedProcessDataKeys([forwardedKey('people[*].status'), exactKey], 'people[0].status'))
            .toEqual([exactKey]);
    });

    it('returns every forwarded key with the referenced path', () => {
        const firstKey = forwardedKey('status');
        const secondKey = forwardedKey('status');

        expect(findForwardedProcessDataKeys([firstKey, secondKey], 'status')).toEqual([firstKey, secondKey]);
    });

    it.each([
        ['a different property', 'people[0].name'],
        ['a shorter path', 'people[0]'],
        ['a longer path', 'people[0].status.code'],
        ['a property instead of a row', 'people.status'],
    ])('does not match %s', (_, path) => {
        expect(findForwardedProcessDataKeys([forwardedKey('people[*].status')], path)).toEqual([]);
    });

    it.each([
        ['an invalid path', 'people..status'],
        ['an empty path', ' '],
        ['a missing path', null],
    ])('does not match %s', (_, path) => {
        expect(findForwardedProcessDataKeys([forwardedKey('people[*].status')], path)).toEqual([]);
    });
});

function forwardedKey(processDataKey: string): ProcessNodeDefinitionMetadataForwardedProcessDataKey {
    return {
        processDataKey,
        label: processDataKey,
        subLabel: null,
        origin: {} as ProcessNodeDefinitionMetadataForwardedProcessDataKey['origin'],
    };
}
