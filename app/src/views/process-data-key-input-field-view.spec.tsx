import {useState} from 'react';
import {fireEvent} from '@testing-library/react';
import {copyToClipboardText} from '../utils/copy-to-clipboard';
import {describe, expect, it, vi} from 'vitest';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
    createProcessDataKeySuggestions,
    ProcessDataKeyInputComponent,
} from './process-data-key-input-field-view';
import {type ProcessNodeDefinitionMetadataForwardedProcessDataKey} from '../modules/process/entities/process-node-definition-metadata';

vi.mock('../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../utils/copy-to-clipboard', () => ({copyToClipboardText: vi.fn().mockResolvedValue(true)}));

describe('createProcessDataKeySuggestions', () => {
    it('should keep the existing root behavior and hide wildcard keys', () => {
        const suggestions = createProcessDataKeySuggestions([
            hint('globalKey'),
            hint('replizierendeListe'),
            hint('replizierendeListe[*].einzelnachweisOhneDateinameReplList'),
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
            hint('replizierendeListe[*].einzelnachweisOhneDateinameReplList'),
        ], {
            disableWildCards: true,
            prefix: 'replizierendeListe[*].',
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
            hint('replizierendeListe[*].einzelnachweisOhneDateinameReplList', 1),
            hint('replizierendeListe[*].einzelnachweisOhneDateinameReplList', 2),
        ], {
            disableWildCards: true,
            prefix: 'replizierendeListe[*].',
        });

        expect(suggestions.map((suggestion) => suggestion.id)).toEqual([
            'einzelnachweisOhneDateinameReplList',
        ]);
    });

    it('should keep scoped process data key suggestions working', () => {
        const suggestions = createProcessDataKeySuggestions([
            hint('replizierendeListe[*].einzelnachweisOhneDateinameReplList'),
            hint('replizierendeListe.nachweisOhneWildcard'),
            hint('otherList[*].ignored'),
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

    it.each(['counter.currentValue', 'person.Name', 'people[0].name'])('accepts the custom path %s without persisting the display prefix', async (path) => {
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
        await user.click(screen.getByRole('textbox', {name: /Vorgangsdatenpfade durchsuchen oder eigenen Pfad eingeben/}));
        await user.paste(`$.${path}`);
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

    it('lets a scoped path be chosen from the suggestions but not typed', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(
            <ProcessDataKeyInputComponent
                label="Vorgangsdatenvariable"
                value={null}
                onChange={onChange}
                scopeProcessDataKey="people"
                suggestions={[{id: 'name', label: 'Name'}]}
            />,
        );

        expect(screen.getByRole('textbox', {name: /Vorgangsdatenvariable/})).toHaveAttribute('readonly');
        await user.click(screen.getByLabelText('Vorgangsdatenpfad auswählen'));
        const search = screen.getByRole('textbox', {name: /Vorgangsdatenpfade durchsuchen/});
        expect(search).not.toHaveAccessibleName(/eigenen Pfad/);
        await user.type(search, 'custom');
        expect(screen.queryByRole('radio', {name: /Eigenen Pfad verwenden/})).not.toBeInTheDocument();
        expect(screen.queryByText(/eigenen Pfad verwenden/)).not.toBeInTheDocument();

        await user.clear(search);
        await user.click(screen.getByRole('radio', {name: /\$\.people\[\*\]\.name/}));
        await user.click(screen.getByRole('button', {name: 'Pfad übernehmen'}));

        expect(onChange).toHaveBeenCalledWith('name');
    });

    it('keeps a scoped path clearable', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(scopedPath(onChange, 'people', 'name'));

        await user.click(screen.getByLabelText('Vorgangsdatenpfad leeren'));

        expect(onChange).toHaveBeenCalledWith(null);
    });

    it('keeps a saved scoped path that is missing from the suggestions', () => {
        const onChange = vi.fn();
        render(
            <ProcessDataKeyInputComponent
                label="Vorgangsdatenvariable"
                value="first-name"
                onChange={onChange}
                scopeProcessDataKey="people"
                suggestions={[{id: 'name', label: 'Name'}]}
            />,
        );

        expect(screen.getByRole('textbox', {name: /Vorgangsdatenvariable/})).toHaveValue('first-name');
        expect(onChange).not.toHaveBeenCalled();
    });

    it('clears a scoped path that the changed scope does not suggest', () => {
        const onChange = vi.fn();
        const {rerender} = render(scopedPath(onChange, 'people', 'name'));

        rerender(scopedPath(onChange, 'persons', 'other'));

        expect(onChange).toHaveBeenCalledWith(null);
    });

    it('keeps a scoped path that the changed scope also suggests', () => {
        const onChange = vi.fn();
        const {rerender} = render(scopedPath(onChange, 'people', 'name'));

        rerender(scopedPath(onChange, 'persons', 'name'));

        expect(onChange).not.toHaveBeenCalled();
    });
});

function scopedPath(onChange: (value: string | null) => void, scopeProcessDataKey: string, suggestionId: string) {
    return (
        <ProcessDataKeyInputComponent
            label="Vorgangsdatenvariable"
            value="name"
            onChange={onChange}
            scopeProcessDataKey={scopeProcessDataKey}
            suggestions={[{id: suggestionId, label: 'Name'}]}
        />
    );
}

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

describe('process data path validation', () => {
    it('validates on blur and revalidates while correcting without rewriting references', () => {
        function Harness() {
            const [value, setValue] = useState<string | null>(null);
            return <ProcessDataKeyInputComponent label="Pfad" value={value} onChange={setValue}/>;
        }
        render(<Harness/>);
        const input = screen.getByRole('textbox', {name: /Pfad/});
        fireEvent.change(input, {target: {value: 'person.'}});
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
        fireEvent.blur(input);
        expect(input).toHaveAttribute('aria-invalid', 'true');
        fireEvent.change(input, {target: {value: 'person.Name'}});
        expect(input).toHaveValue('person.Name');
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
        fireEvent.change(input, {target: {value: 'items[2147483648]'}});
        expect(input).toHaveAttribute('aria-invalid', 'true');
    });

    it('copies the complete scoped path while keeping only its relative value', async () => {
        const onChange = vi.fn();
        render(<ProcessDataKeyInputComponent label="Pfad" value="Name" onChange={onChange} prefix="people[*]." readOnly/>);
        await userEvent.setup().click(screen.getByRole('button', {name: 'Pfad kopieren'}));
        expect(copyToClipboardText).toHaveBeenCalledWith('$.people[*].Name');
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.queryByRole('button', {name: 'Vorgangsdatenpfad leeren'})).not.toBeInTheDocument();
    });
});
