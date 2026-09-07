import {describe, expect, it, vi} from 'vitest';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
    createProcessDataKeySuggestions,
    ProcessDataKeyInputComponent,
} from './process-data-key-input-field-view';
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

describe('ProcessDataKeyInputComponent', () => {
    const suggestions = [{
        id: 'person.name',
        label: 'Name',
        subLabel: 'Antrag eingereicht',
    }];

    it('selects a suggested process-data path', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(
            <ProcessDataKeyInputComponent
                label="Vorgangsdatenvariable"
                value={null}
                onChange={onChange}
                suggestions={suggestions}
            />,
        );

        await user.click(screen.getByLabelText('Vorgangsdatenpfad auswählen'));
        await user.click(screen.getByRole('radio', {name: /\$\.person\.name/}));
        await user.click(screen.getByRole('button', {name: 'Pfad übernehmen'}));

        expect(onChange).toHaveBeenCalledWith('person.name');
    });

    it.each(['counter.currentValue', 'person.Name'])('accepts the custom path %s without persisting the display prefix', async (path) => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(
            <ProcessDataKeyInputComponent
                label="Vorgangsdatenvariable"
                value={null}
                onChange={onChange}
                suggestions={suggestions}
            />,
        );

        await user.click(screen.getByLabelText('Vorgangsdatenpfad auswählen'));
        await user.type(
            screen.getByRole('textbox', {name: /Vorgangsdatenpfade durchsuchen oder eigenen Pfad eingeben/}),
            `$.${path}`,
        );
        await user.click(screen.getByRole('radio', {name: /Eigenen Pfad verwenden/}));
        await user.click(screen.getByRole('button', {name: 'Pfad übernehmen'}));

        expect(onChange).toHaveBeenCalledWith(path);
    });

    it('keeps a populated process-data path clearable', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(
            <ProcessDataKeyInputComponent
                label="Vorgangsdatenvariable"
                value="person.name"
                onChange={onChange}
                suggestions={suggestions}
            />,
        );

        await user.click(screen.getByLabelText('Vorgangsdatenpfad leeren'));

        expect(onChange).toHaveBeenCalledWith(null);
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
