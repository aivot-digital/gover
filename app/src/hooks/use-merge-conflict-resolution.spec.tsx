import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {type MergeSpec} from '../utils/three-way-merge';
import {type MergeConflictDialogMode} from '../dialogs/merge-conflict-dialog/merge-conflict-dialog';
import {InputMode, InputVariableSource} from '../models/input-mode';
import {variableReferenceMergeRules} from '../utils/variable-reference-merge';
import {prepareMerge, useMergeConflictResolution} from './use-merge-conflict-resolution';

function setup<T>(request: {
    base: T;
    mine: T;
    theirs: T;
    spec: MergeSpec<T>;
    mode?: MergeConflictDialogMode;
}) {
    const onOutcome = vi.fn();

    function Harness() {
        const {resolve, dialog} = useMergeConflictResolution();
        return <>
            <button onClick={() => {
                void resolve({...request, mode: request.mode ?? 'save', storedStateChanged: true}).then(onOutcome);
            }}>
                Konflikt prüfen
            </button>
            {dialog}
        </>;
    }

    render(<Harness/>);
    return {onOutcome, user: userEvent.setup()};
}

describe('useMergeConflictResolution', () => {
    it('recognizes a chosen draft with equivalent legacy reference fields as already stored', () => {
        const reference = {source: InputVariableSource.ProcessData, path: 'contact.email'};
        const mine = {value: {type: InputMode.Variable, reference}};
        const theirs = {value: {type: InputMode.Variable, reference: {...reference, nodeDataKey: null, markdown: false}}};
        const merge = prepareMerge({base: mine, mine, theirs, spec: variableReferenceMergeRules}, {'': 'mine'});
        expect(merge.value).toBe(mine);
        expect(merge.changed).toBe(false);
    });

    it('shows grouped property labels and applies mixed choices together with non-conflicting stored changes', async () => {
        const {user, onOutcome} = setup({
            base: {name: 'Alt', description: 'Alt', notes: 'Alt', lockVersion: 1},
            mine: {name: 'Mein Titel', description: 'Meine Beschreibung', notes: 'Alt', lockVersion: 1},
            theirs: {name: 'Gespeicherter Titel', description: 'Gespeicherte Beschreibung', notes: 'Neue Notiz', lockVersion: 2},
            spec: {
                serverManaged: ['lockVersion'],
                label: () => 'Bisherige vollständige Bezeichnung',
                describeConflict: (path) => ({
                    group: {id: 'element-1', label: 'Textfeld „Vorname“'},
                    propertyLabel: path[0] === 'name' ? 'Titel' : 'Beschreibung',
                }),
            },
        });

        await user.click(screen.getByRole('button', {name: 'Konflikt prüfen'}));

        expect(screen.getByRole('button', {name: /Textfeld „Vorname“/})).toHaveAttribute('aria-expanded', 'true');
        const title = screen.getByRole('group', {name: 'Abweichende Einstellung: Titel'});
        const description = screen.getByRole('group', {name: 'Abweichende Einstellung: Beschreibung'});
        expect(within(title).getByRole('radio', {name: 'Meine Änderung soll behalten werden'}))
            .toHaveAccessibleDescription('Mein Titel');
        expect(within(description).getByRole('radio', {name: 'Meine Änderung soll behalten werden'}))
            .toBeChecked();

        await user.click(within(title).getByRole('radio', {name: 'Gespeicherter Stand soll übernommen werden'}));
        await user.click(screen.getByRole('button', {name: 'Übernehmen und speichern'}));

        await waitFor(() => expect(onOutcome).toHaveBeenCalledExactlyOnceWith({
            status: 'apply',
            value: {name: 'Gespeicherter Titel', description: 'Meine Beschreibung', notes: 'Neue Notiz', lockVersion: 2},
        }));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it.each([
        {spec: {label: () => 'Titel'}, fieldLabel: 'Titel'},
        {spec: {}, fieldLabel: 'name'},
    ])('uses the existing label fallback "$fieldLabel" without presentation metadata', async ({spec, fieldLabel}) => {
        const {user, onOutcome} = setup({
            base: {name: 'Alt'},
            mine: {name: 'Mein Titel'},
            theirs: {name: 'Gespeicherter Titel'},
            spec,
        });

        await user.click(screen.getByRole('button', {name: 'Konflikt prüfen'}));

        expect(screen.getByRole('group', {name: `Abweichende Einstellung: ${fieldLabel}`})).toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Übernehmen und speichern'}));

        await waitFor(() => expect(onOutcome).toHaveBeenCalledExactlyOnceWith({status: 'apply', value: {name: 'Mein Titel'}}));
    });

    it.each([
        {action: 'Abbrechen', status: 'cancel'},
        {action: 'Änderungen verwerfen', status: 'discard'},
    ])('returns $status independently of the selected conflict values', async ({action, status}) => {
        const {user, onOutcome} = setup({
            base: {name: 'Alt'},
            mine: {name: 'Mein Titel'},
            theirs: {name: 'Gespeicherter Titel'},
            spec: {label: () => 'Titel'},
        });

        await user.click(screen.getByRole('button', {name: 'Konflikt prüfen'}));
        await user.click(screen.getByRole('radio', {name: 'Gespeicherter Stand soll übernommen werden'}));
        await user.click(screen.getByRole('button', {name: action}));

        await waitFor(() => expect(onOutcome).toHaveBeenCalledExactlyOnceWith({status}));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('asks for the whole object when individual choices violate the merge invariant', async () => {
        const {user, onOutcome} = setup({
            base: {start: 1, end: 2},
            mine: {start: 3, end: 4},
            theirs: {start: 5, end: 6},
            spec: {
                validate: (value) => value.start < value.end,
                describeConflict: (path) => ({
                    group: {id: 'range', label: 'Zeitraum'},
                    propertyLabel: path.length === 0 ? 'Gesamter Zeitraum' : path[0] === 'start' ? 'Beginn' : 'Ende',
                }),
            },
        });

        await user.click(screen.getByRole('button', {name: 'Konflikt prüfen'}));
        const start = screen.getByRole('group', {name: 'Abweichende Einstellung: Beginn'});
        await user.click(within(start).getByRole('radio', {name: 'Gespeicherter Stand soll übernommen werden'}));
        await user.click(screen.getByRole('button', {name: 'Übernehmen und speichern'}));

        const wholeRange = await screen.findByRole('group', {name: 'Abweichende Einstellung: Gesamter Zeitraum'});
        expect(onOutcome).not.toHaveBeenCalled();
        expect(screen.queryByRole('group', {name: 'Abweichende Einstellung: Beginn'})).not.toBeInTheDocument();
        await user.click(within(wholeRange).getByRole('radio', {name: 'Gespeicherter Stand soll übernommen werden'}));
        await user.click(screen.getByRole('button', {name: 'Übernehmen und speichern'}));

        await waitFor(() => expect(onOutcome).toHaveBeenCalledExactlyOnceWith({status: 'apply', value: {start: 5, end: 6}}));
    });

    it('applies a save without conflicting fields without opening the dialog', async () => {
        const {user, onOutcome} = setup({
            base: {name: 'Alt', notes: 'Alt'},
            mine: {name: 'Mein Titel', notes: 'Alt'},
            theirs: {name: 'Alt', notes: 'Neue Notiz'},
            spec: {},
        });

        await user.click(screen.getByRole('button', {name: 'Konflikt prüfen'}));

        await waitFor(() => expect(onOutcome).toHaveBeenCalledExactlyOnceWith({
            status: 'apply', value: {name: 'Mein Titel', notes: 'Neue Notiz'},
        }));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('does not offer recovery when the draft is already present in the stored state', async () => {
        const {user, onOutcome} = setup({
            base: {name: 'Alt'},
            mine: {name: 'Neuer Titel'},
            theirs: {name: 'Neuer Titel'},
            spec: {},
            mode: 'restore',
        });

        await user.click(screen.getByRole('button', {name: 'Konflikt prüfen'}));

        await waitFor(() => expect(onOutcome).toHaveBeenCalledExactlyOnceWith({status: 'unchanged'}));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
});
