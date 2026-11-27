import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {PersonAssignmentDialog} from './person-assignment-dialog';

function setup(overrides: Partial<React.ComponentProps<typeof PersonAssignmentDialog>> = {}) {
    const props = {
        title: 'Vorgang zuweisen',
        description: 'Wählen Sie eine Person.',
        assignedUserId: null,
        loadOptions: vi.fn().mockResolvedValue([
            {
                value: 'kim',
                label: 'Kim Beispiel',
                subLabel: 'kim@example.org',
            },
        ]),
        onSave: vi.fn().mockResolvedValue(undefined),
        onClose: vi.fn(),
        ...overrides,
    };
    render(<PersonAssignmentDialog {...props} />);
    return props;
}

async function chooseKim() {
    const user = userEvent.setup();
    const input = screen.getByRole('combobox', {name: /Zugewiesen an/});
    await waitFor(() => expect(input).not.toBeDisabled());
    await user.click(input);
    await user.click(await screen.findByRole('option', {name: /Kim Beispiel/}));
    return user;
}

describe('PersonAssignmentDialog', () => {
    it.each(['kim', 'former', null])('shows loading before evaluating the current assignment (%s)', async (assignedUserId) => {
        let complete!: (options: {value: string; label: string}[]) => void;
        const props = setup({
            assignedUserId,
            loadOptions: vi.fn(
                () => new Promise<{value: string; label: string}[]>((resolve) => {
                    complete = resolve;
                }),
            ),
        });
        const input = screen.getByRole('combobox', {name: /Zugewiesen an/});
        expect(input).toBeDisabled();
        expect(input).toHaveAttribute('placeholder', 'Personen werden geladen …');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Zuweisung speichern'})).toBeDisabled();

        complete([{value: 'kim', label: 'Kim Beispiel'}]);
        await waitFor(() => expect(input).not.toBeDisabled());
        expect(input).not.toHaveAttribute('placeholder', 'Personen werden geladen …');
        expect(input).toHaveValue(assignedUserId === 'kim' ? 'Kim Beispiel' : '');
        if (assignedUserId === 'former') {
            expect(screen.getByRole('alert')).toHaveTextContent('Die bisher zugewiesene Person steht nicht mehr zur Auswahl.');
        } else {
            expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        }
        expect(props.onSave).not.toHaveBeenCalled();
    });

    it('saves a selected person and closes only after the request succeeds', async () => {
        let complete!: () => void;
        const props = setup({
            onSave: vi.fn(
                () =>
                    new Promise<void>((resolve) => {
                        complete = resolve;
                    }),
            ),
        });
        expect(screen.getByRole('dialog', {name: 'Vorgang zuweisen'})).toHaveAccessibleDescription(
            'Wählen Sie eine Person.',
        );
        expect(screen.getByRole('button', {name: 'Zuweisung speichern'})).toBeDisabled();
        const user = await chooseKim();
        await user.click(screen.getByRole('button', {name: 'Zuweisung speichern'}));
        expect(props.onSave).toHaveBeenCalledWith('kim');
        expect(props.onClose).not.toHaveBeenCalled();
        expect(screen.getByRole('button', {name: 'Abbrechen'})).toBeDisabled();
        await user.keyboard('{Escape}');
        expect(props.onClose).not.toHaveBeenCalled();
        complete();
        await waitFor(() => expect(props.onClose).toHaveBeenCalledOnce());
    });

    it('allows clearing a former assignment that is no longer available', async () => {
        const props = setup({
            assignedUserId: 'former',
            loadOptions: vi.fn().mockResolvedValue([]),
        });
        await screen.findByText(/Die bisher zugewiesene Person steht nicht mehr zur Auswahl/);
        const user = userEvent.setup();
        await user.click(screen.getByRole('button', {name: 'Zuweisung aufheben'}));
        expect(props.onSave).toHaveBeenCalledWith(null);
        await waitFor(() => expect(props.onClose).toHaveBeenCalledOnce());
    });

    it('requires a person and keeps the separate removal action disabled without a persisted assignment', async () => {
        setup();
        const save = screen.getByRole('button', {name: 'Zuweisung speichern'});
        expect(
            within(save.parentElement!)
                .getAllByRole('button')
                .map((button) => button.textContent),
        ).toEqual(['Zuweisung speichern', 'Abbrechen', 'Zuweisung aufheben']);
        expect(screen.getByRole('combobox', {name: /Zugewiesen an/})).toBeRequired();
        expect(screen.queryByText(/Mit „Zuweisung aufheben“/)).not.toBeInTheDocument();
        expect(save).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Zuweisung aufheben'})).toBeDisabled();
        await chooseKim();
        expect(save).toBeEnabled();
        expect(screen.getByRole('button', {name: 'Zuweisung aufheben'})).toBeDisabled();
    });

    it('allows removing the persisted assignment even if candidate loading fails', async () => {
        const props = setup({
            assignedUserId: 'former',
            loadOptions: vi.fn().mockRejectedValue(new Error()),
        });
        await screen.findByText('Die verfügbaren Personen konnten nicht geladen werden.');
        const user = userEvent.setup();
        await user.click(screen.getByRole('button', {name: 'Zuweisung aufheben'}));
        expect(props.onSave).toHaveBeenCalledWith(null);
    });

    it('keeps a required assignment when candidate loading fails', async () => {
        const props = setup({
            assignedUserId: 'former',
            allowUnassign: false,
            loadOptions: vi.fn().mockRejectedValue(new Error()),
        });
        await screen.findByText('Die verfügbaren Personen konnten nicht geladen werden.');
        expect(screen.queryByRole('button', {name: 'Zuweisung aufheben'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Zuweisung speichern'})).toBeDisabled();
        expect(props.onSave).not.toHaveBeenCalled();
    });

    it('preserves the dialog and reports a failed removal accurately', async () => {
        const props = setup({
            assignedUserId: 'kim',
            onSave: vi.fn().mockRejectedValue(new Error()),
        });
        await waitFor(() => expect(screen.getByRole('combobox', {name: /Zugewiesen an/})).not.toBeDisabled());
        const user = userEvent.setup();
        await user.click(screen.getByRole('button', {name: 'Zuweisung aufheben'}));
        expect(await screen.findByRole('alert')).toHaveTextContent('Die Zuweisung konnte nicht aufgehoben werden.');
        expect(props.onClose).not.toHaveBeenCalled();
        expect(screen.getByRole('combobox', {name: /Zugewiesen an/})).toHaveValue('Kim Beispiel');
    });

    it('retains the draft after a rejected save', async () => {
        const props = setup({onSave: vi.fn().mockRejectedValue(new Error('No longer eligible'))});
        const user = await chooseKim();
        await user.click(screen.getByRole('button', {name: 'Zuweisung speichern'}));
        expect(await screen.findByRole('alert')).toHaveTextContent('Die Zuweisung konnte nicht gespeichert werden.');
        expect(screen.getByRole('combobox', {name: /Zugewiesen an/})).toHaveValue('Kim Beispiel');
        expect(props.onClose).not.toHaveBeenCalled();
    });

    it('distinguishes a failed candidate lookup and supports retrying', async () => {
        const loadOptions = vi.fn().mockRejectedValueOnce(new Error()).mockResolvedValueOnce([]);
        setup({loadOptions});
        expect(await screen.findByRole('alert')).toHaveTextContent(
            'Die verfügbaren Personen konnten nicht geladen werden.',
        );
        expect(screen.getByRole('button', {name: 'Zuweisung speichern'})).toBeDisabled();
        const user = userEvent.setup();
        await user.click(screen.getByRole('button', {name: 'Erneut laden'}));
        await screen.findByText('Derzeit erfüllt keine Person die Voraussetzungen für eine Zuweisung.');
        expect(loadOptions).toHaveBeenCalledTimes(2);
    });
});
