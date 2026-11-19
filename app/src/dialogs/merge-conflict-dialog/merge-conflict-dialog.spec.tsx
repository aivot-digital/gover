import {type ComponentProps} from 'react';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {MergeConflictDialog} from './merge-conflict-dialog';

const restoreTitle = 'Ungespeicherte Änderungen wiederherstellen?';

function setup(overrides: Partial<ComponentProps<typeof MergeConflictDialog>> = {}) {
    const props = {
        open: true,
        mode: 'restore' as const,
        storedStateChanged: false,
        conflicts: [],
        onApply: vi.fn(),
        onDiscard: vi.fn(),
        onCancel: vi.fn(),
        ...overrides,
    };
    render(<MergeConflictDialog {...props}/>);
    return props;
}

describe('MergeConflictDialog', () => {
    it('explains recovery and discarding before restoring changes without conflicts', async () => {
        const props = setup();
        const dialog = screen.getByRole('dialog', {name: restoreTitle});
        expect(within(dialog).getByText(/Aus Ihrer letzten Bearbeitung liegen ungespeicherte Änderungen vor/))
            .toBeInTheDocument();
        expect(within(dialog).getByText(/Speichern Sie Ihre Änderungen nach der Wiederherstellung/))
            .toHaveTextContent('Beim Verwerfen wird die zwischengespeicherte Fassung gelöscht.');
        expect(within(dialog).queryByText(/Zwischenzeitlich wurde ein neuerer Stand gespeichert/))
            .not.toBeInTheDocument();
        expect(within(dialog).getAllByRole('button').map((button) => button.textContent))
            .toEqual(['Änderungen wiederherstellen', 'Änderungen verwerfen']);

        await userEvent.setup().click(within(dialog).getByRole('button', {name: 'Änderungen wiederherstellen'}));

        expect(props.onApply).toHaveBeenCalledExactlyOnceWith({});
        expect(props.onDiscard).not.toHaveBeenCalled();
        expect(props.onCancel).not.toHaveBeenCalled();
    });

    it('explains that recovery combines changes with a newer stored state when there are no conflicts', async () => {
        const props = setup({storedStateChanged: true});
        const dialog = screen.getByRole('dialog', {name: restoreTitle});
        expect(within(dialog).getByText(/Zwischenzeitlich wurde ein neuerer Stand gespeichert/))
            .toHaveTextContent('Ihre Änderungen können mit diesem Stand zusammengeführt werden.');
        expect(within(dialog).queryByRole('radio')).not.toBeInTheDocument();

        await userEvent.setup().click(within(dialog).getByRole('button', {name: 'Änderungen wiederherstellen'}));

        expect(props.onApply).toHaveBeenCalledExactlyOnceWith({});
        expect(props.onDiscard).not.toHaveBeenCalled();
    });

    it.each(['restore', 'save'] as const)('passes the selected conflict values to the %s action', async (mode) => {
        const props = setup({
            mode,
            storedStateChanged: true,
            conflicts: [
                {id: 'name', label: 'Name des Elementes', mine: 'Mein Name', theirs: 'Gespeicherter Name'},
                {id: 'description', label: 'Beschreibung', mine: 'Meine Beschreibung', theirs: 'Gespeicherte Beschreibung'},
            ],
        });
        const dialog = screen.getByRole('dialog', {
            name: mode === 'restore' ? restoreTitle : 'Neuerer Stand gespeichert',
        });
        const name = within(dialog).getByRole('group', {name: 'Name des Elementes'});
        const description = within(dialog).getByRole('group', {name: 'Beschreibung'});
        expect(within(name).getByRole('radio', {name: 'Meine Änderung'})).toBeChecked();
        expect(within(description).getByRole('radio', {name: 'Meine Änderung'})).toBeChecked();
        if (mode === 'restore') {
            expect(within(dialog).getByText(/Einige Angaben wurden in beiden Fassungen unterschiedlich geändert/))
                .toHaveTextContent('Alle übrigen Änderungen werden zusammengeführt.');
        }

        const user = userEvent.setup();
        await user.click(within(name).getByRole('radio', {name: 'Gespeicherter Stand'}));
        await user.click(within(dialog).getByRole('button', {
            name: mode === 'restore' ? 'Änderungen wiederherstellen' : 'Übernehmen und speichern',
        }));

        expect(props.onApply).toHaveBeenCalledExactlyOnceWith({name: 'theirs', description: 'mine'});
        expect(props.onDiscard).not.toHaveBeenCalled();
        expect(props.onCancel).not.toHaveBeenCalled();
    });

    it.each(['restore', 'save'] as const)('discards changes only after the explicit %s discard action', async (mode) => {
        const props = setup({mode});

        await userEvent.setup().click(screen.getByRole('button', {name: 'Änderungen verwerfen'}));

        expect(props.onDiscard).toHaveBeenCalledOnce();
        expect(props.onApply).not.toHaveBeenCalled();
        expect(props.onCancel).not.toHaveBeenCalled();
    });

    it.each(['Escape', 'background'] as const)('keeps the recovery decision pending on %s', async (trigger) => {
        const props = setup();
        const dialog = screen.getByRole('dialog', {name: restoreTitle});
        expect(within(dialog).queryByRole('button', {name: 'Schließen'})).not.toBeInTheDocument();
        expect(within(dialog).queryByRole('button', {name: 'Abbrechen'})).not.toBeInTheDocument();
        const user = userEvent.setup();

        if (trigger === 'Escape') {
            await user.keyboard('{Escape}');
        } else {
            await user.click(dialog.parentElement!);
        }

        expect(screen.getByRole('dialog', {name: restoreTitle})).toBe(dialog);
        expect(props.onApply).not.toHaveBeenCalled();
        expect(props.onDiscard).not.toHaveBeenCalled();
        expect(props.onCancel).not.toHaveBeenCalled();
    });

    it.each(['Abbrechen', 'Schließen', 'Escape', 'background'] as const)(
        'cancels a save conflict through %s without applying or discarding changes',
        async (trigger) => {
            const props = setup({mode: 'save', storedStateChanged: true});
            const dialog = screen.getByRole('dialog', {name: 'Neuerer Stand gespeichert'});
            const apply = within(dialog).getByRole('button', {name: 'Übernehmen und speichern'});
            expect(within(apply.parentElement!).getAllByRole('button').map((button) => button.textContent))
                .toEqual(['Übernehmen und speichern', 'Änderungen verwerfen', 'Abbrechen']);
            const user = userEvent.setup();

            if (trigger === 'Escape') {
                await user.keyboard('{Escape}');
            } else if (trigger === 'background') {
                await user.click(dialog.parentElement!);
            } else {
                await user.click(within(dialog).getByRole('button', {name: trigger}));
            }

            expect(props.onCancel).toHaveBeenCalledOnce();
            expect(props.onApply).not.toHaveBeenCalled();
            expect(props.onDiscard).not.toHaveBeenCalled();
        },
    );
});
