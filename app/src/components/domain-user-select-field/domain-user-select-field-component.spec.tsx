import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {DomainUserSelectFieldComponent} from './domain-user-select-field-component';
import type {DomainAndUserSelectOption} from './domain-user-select-options';
import Groups from '@aivot/mui-material-symbols-400-n25-outlined/Groups';

const options: DomainAndUserSelectOption[] = [
    {
        key: 'orgUnit:10',
        value: {type: 'orgUnit', id: '10'},
        label: 'Fachbereich Anträge',
        subLabel: 'Organisationseinheit',
        group: 'Organisationseinheiten',
    },
    {
        key: 'team:20',
        value: {type: 'team', id: '20'},
        label: 'Team Leistungsprüfung',
        subLabel: 'Team',
        group: 'Teams',
        icon: <Groups />,
    },
    {
        key: 'user:user-1',
        value: {type: 'user', id: 'user-1'},
        label: 'Mustermann, Max',
        subLabel: 'max.mustermann@example.org',
        group: 'Mitarbeitende',
    },
];

describe('DomainUserSelectFieldComponent', () => {
    it('combines its external label with multiple Autocomplete semantics', () => {
        const {container} = render(
            <DomainUserSelectFieldComponent
                label="Personenkreis"
                value={[{type: 'team', id: '20'}]}
                onChange={vi.fn()}
                options={options}
                hint="Organisationseinheiten, Teams und Mitarbeitende können kombiniert werden."
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Personenkreis – optional'});
        expect(input).toHaveAttribute('aria-autocomplete', 'list');
        expect(input).toHaveAttribute('aria-expanded', 'false');
        expect(input).toHaveAccessibleDescription(
            'Organisationseinheiten, Teams und Mitarbeitende können kombiniert werden.',
        );
        expect(screen.getByText('Team Leistungsprüfung')).toBeInTheDocument();
        const selectedChip = screen.getByText('Team Leistungsprüfung').closest('.MuiChip-root');
        expect(selectedChip).toHaveClass('MuiChip-sizeSmall');
        expect(getComputedStyle(selectedChip!).height).toBe('24px');
        expect(getComputedStyle(selectedChip!.querySelector('.MuiChip-icon')!).width).toBe('18px');
        expect(getComputedStyle(selectedChip!.querySelector('.MuiChip-icon')!).height).toBe('18px');
        expect(getComputedStyle(container.querySelector('.MuiInputBase-root')!).minHeight).toBe('44px');
    });

    it('reports one externally associated error message', () => {
        render(
            <DomainUserSelectFieldComponent
                label="Personenkreis"
                value={null}
                onChange={vi.fn()}
                options={options}
                error="Wählen Sie mindestens einen Eintrag."
                required
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Personenkreis'});
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription('Wählen Sie mindestens einen Eintrag.');
        expect(screen.getAllByRole('alert')).toHaveLength(1);
    });

    it('adds selected values without changing their domain type', () => {
        const onChange = vi.fn();

        render(
            <DomainUserSelectFieldComponent
                label="Personenkreis"
                value={[{type: 'team', id: '20'}]}
                onChange={onChange}
                options={options}
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Personenkreis – optional'});
        fireEvent.mouseDown(input);
        fireEvent.click(screen.getByRole('option', {name: /Mustermann, Max/}));

        expect(onChange).toHaveBeenCalledWith([
            {type: 'team', id: '20'},
            {type: 'user', id: 'user-1'},
        ]);
    });

    it('exposes deriving as a busy, read-only state', () => {
        render(
            <DomainUserSelectFieldComponent
                label="Personenkreis"
                value={[{type: 'team', id: '20'}]}
                onChange={vi.fn()}
                options={options}
                busy
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Personenkreis – optional'});
        expect(input).toHaveAttribute('aria-busy', 'true');
        expect(input).toHaveAttribute('aria-readonly', 'true');
    });
});
