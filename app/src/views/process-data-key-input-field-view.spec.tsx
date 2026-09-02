import {describe, expect, it} from 'vitest';
import {createProcessDataKeySuggestions} from './process-data-key-input-field-view';
import {type ProcessNodeDefinitionMetadataForwardedProcessDataKey} from '../modules/process/entities/process-node-definition-metadata';

describe('createProcessDataKeySuggestions', () => {
    it('should keep the existing root behavior and hide wildcard keys', () => {
        const suggestions = createProcessDataKeySuggestions([
            hint('globalKey'),
            hint('replizierendeListe'),
            hint('replizierendeListe.*.einzelnachweisOhneDateinameReplList'),
        ], {
            disableWildCards: true,
        });

        expect(suggestions.map((suggestion) => suggestion.id)).toEqual([
            'globalKey',
            'replizierendeListe',
        ]);
    });

    it('should suggest relative child keys inside a replicating list', () => {
        const suggestions = createProcessDataKeySuggestions([
            hint('globalKey'),
            hint('replizierendeListe'),
            hint('replizierendeListe.*.einzelnachweisOhneDateinameReplList'),
        ], {
            disableWildCards: true,
            prefix: 'replizierendeListe.*.',
        });

        expect(suggestions.map((suggestion) => suggestion.id)).toEqual([
            'einzelnachweisOhneDateinameReplList',
        ]);
    });

    it('should deduplicate suggestions by their displayed key', () => {
        const suggestions = createProcessDataKeySuggestions([
            hint('shared', 1),
            hint('other', 1),
            hint('shared', 2),
        ], {
            disableWildCards: true,
        });

        expect(suggestions.map((suggestion) => suggestion.id)).toEqual([
            'shared',
            'other',
        ]);
    });

    it('should deduplicate relative keys inside a replicating list', () => {
        const suggestions = createProcessDataKeySuggestions([
            hint('replizierendeListe.*.einzelnachweisOhneDateinameReplList', 1),
            hint('replizierendeListe.*.einzelnachweisOhneDateinameReplList', 2),
        ], {
            disableWildCards: true,
            prefix: 'replizierendeListe.*.',
        });

        expect(suggestions.map((suggestion) => suggestion.id)).toEqual([
            'einzelnachweisOhneDateinameReplList',
        ]);
    });

    it('should keep scoped process data key suggestions working', () => {
        const suggestions = createProcessDataKeySuggestions([
            hint('replizierendeListe.*.einzelnachweisOhneDateinameReplList'),
            hint('replizierendeListe.nachweisOhneWildcard'),
            hint('otherList.*.ignored'),
        ], {
            disableWildCards: true,
            scopeProcessDataKey: 'replizierendeListe',
        });

        expect(suggestions.map((suggestion) => suggestion.id)).toEqual([
            'einzelnachweisOhneDateinameReplList',
            'nachweisOhneWildcard',
        ]);
    });
});

function hint(
    processDataKey: string,
    originId = 1,
): ProcessNodeDefinitionMetadataForwardedProcessDataKey {
    return {
        processDataKey,
        label: processDataKey,
        subLabel: null,
        origin: {
            id: originId,
            name: `Node ${originId}`,
        } as ProcessNodeDefinitionMetadataForwardedProcessDataKey['origin'],
    };
}
