import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {DepartmentSelectField} from './department-select-field';

const {department} = vi.hoisted(() => ({department: {
    id: 10,
    name: 'Fachbereich Digitalisierung',
    created: '2026-01-01T00:00:00Z',
    updated: '2026-01-01T00:00:00Z',
    depth: 1,
    parentNames: ['Bezirksamt Mitte'],
    children: [],
}}));

vi.mock('../dialogs/select-department-dialog', () => ({
    SelectDepartmentDialog: (props: {
        id?: string;
        open: boolean;
        title: string;
        onSelect: (value: typeof department) => void;
    }) => props.open ? (
        <div id={props.id} role="dialog" aria-label={props.title}>
            <button type="button" onClick={() => props.onSelect(department)}>
                Fachbereich auswählen
            </button>
        </div>
    ) : null,
}));

describe('DepartmentSelectField', () => {
    it('exposes label, selected department and helper text without a floating label', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();

        render(
            <DepartmentSelectField
                label="Organisationseinheit"
                value={department}
                onChange={onChange}
                hint="Wählen Sie die verantwortliche Organisationseinheit."
                departments={[department]}
            />,
        );

        const label = screen.getByTitle('Organisationseinheit');
        const control = document.getElementById(label.getAttribute('for')!);
        expect(control).toHaveAccessibleName('Organisationseinheit – optional Fachbereich Digitalisierung');
        expect(control).toHaveAccessibleDescription('Wählen Sie die verantwortliche Organisationseinheit.');
        expect(control).toHaveAttribute('aria-haspopup', 'dialog');
        expect(getComputedStyle(control!.parentElement!).minHeight).toBe('52px');

        await user.click(screen.getByRole('button', {name: 'Organisationseinheit: Auswahl entfernen'}));
        expect(onChange).toHaveBeenCalledWith(null);

        await user.click(control!);
        const dialog = screen.getByRole('dialog', {name: 'Organisationseinheit auswählen'});
        expect(control).toHaveAttribute('aria-controls', dialog.id);
        expect(control).toHaveAttribute('aria-expanded', 'true');

        await user.click(screen.getByRole('button', {name: 'Fachbereich auswählen'}));
        expect(onChange).toHaveBeenCalledWith(department);
    });
});
