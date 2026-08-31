import {describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import {SelectFieldComponent} from './select-field-component';
import {SelectFieldPresentation} from '../../models/elements/form/input/select-field-presentation';

describe('SelectFieldComponent', () => {
    it('associates its external label and hint with the combobox', () => {
        const {container} = render(
            <SelectFieldComponent
                label="Kategorie"
                value="request"
                onChange={vi.fn()}
                hint="Wählen Sie eine Kategorie."
                required
                options={[
                    {value: 'request', label: 'Antrag'},
                ]}
            />,
        );

        const select = container.querySelector('[role="combobox"]');
        expect(select).not.toBeNull();
        expect(select).toHaveAccessibleName('Kategorie');
        expect(select).toHaveAccessibleDescription('Wählen Sie eine Kategorie.');
        expect(select).toHaveAttribute('aria-required', 'true');
        expect(select?.closest('.MuiInputBase-root')).toHaveClass('MuiInputBase-sizeSmall');
    });

    it('exposes validation errors on the combobox', () => {
        const {container} = render(
            <SelectFieldComponent
                label="Kategorie"
                value={null}
                onChange={vi.fn()}
                error="Bitte wählen Sie eine Kategorie."
                options={[]}
            />,
        );

        const select = container.querySelector('[role="combobox"]');
        expect(select).not.toBeNull();
        expect(select).toHaveAccessibleName('Kategorie – optional');
        expect(select).toHaveAttribute('aria-invalid', 'true');
        expect(select).toHaveAccessibleDescription('Bitte wählen Sie eine Kategorie.');
    });

    it('uses the dropdown presentation by default', () => {
        const {container} = render(
            <SelectFieldComponent
                label="Kategorie"
                value="request"
                onChange={vi.fn()}
                options={[{value: 'request', label: 'Antrag'}]}
            />,
        );

        const control = container.querySelector('[role="combobox"]');
        expect(control).toHaveAccessibleName('Kategorie – optional');
        expect(control?.tagName).toBe('DIV');
        expect(control).toHaveTextContent('Antrag');
        expect(control).not.toHaveAttribute('aria-autocomplete');
    });

    it('preserves numeric option values when a dropdown selection changes', () => {
        const onChange = vi.fn();

        const {container} = render(
            <SelectFieldComponent
                label="Systemrolle"
                value={1}
                onChange={onChange}
                options={[
                    {value: 1, label: 'Administration'},
                    {value: 2, label: 'Sachbearbeitung'},
                ]}
            />,
        );

        const control = container.querySelector('[role="combobox"]');
        expect(control).toHaveAccessibleName('Systemrolle – optional');
        fireEvent.mouseDown(control!);
        const option = Array.from(document.querySelectorAll('[role="option"]'))
            .find((candidate) => candidate.textContent?.includes('Sachbearbeitung'));
        expect(option).toBeDefined();
        fireEvent.click(option!);

        expect(onChange).toHaveBeenCalledWith(2);
    });

    it('matches legacy numeric input against string-backed options', () => {
        const {container} = render(
            <SelectFieldComponent
                label="Systemrolle"
                value={4 as unknown as string}
                onChange={vi.fn()}
                options={[{value: '4', label: 'Sachbearbeitung'}]}
            />,
        );

        const control = container.querySelector('[role="combobox"]');
        expect(control).toHaveAccessibleName('Systemrolle – optional');
        expect(control).toHaveTextContent('Sachbearbeitung');
    });

    it('renders a searchable, constrained combobox and emits its option value', () => {
        const onChange = vi.fn();

        render(
            <SelectFieldComponent
                label="Kategorie"
                value={null}
                onChange={onChange}
                presentation={SelectFieldPresentation.Combobox}
                options={[
                    {value: 'request', label: 'Antrag', subLabel: 'Neuen Antrag erfassen'},
                    {value: 'approval', label: 'Freigabe', subLabel: 'Entscheidung einholen'},
                ]}
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Kategorie – optional'});
        expect(input.tagName).toBe('INPUT');
        expect(input).toHaveAttribute('aria-autocomplete', 'list');

        fireEvent.change(input, {target: {value: 'Entscheidung'}});
        fireEvent.click(screen.getByRole('option', {name: /Freigabe/}));

        expect(onChange).toHaveBeenCalledWith('approval');
    });
});
