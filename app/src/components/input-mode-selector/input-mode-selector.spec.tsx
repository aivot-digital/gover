import {describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import {DynamicTextIndicator, DynamicTextIndicatorLabel} from './dynamic-text-indicator';
import {InputModeSelector} from './input-mode-selector';

describe('InputModeSelector', () => {
    it('offers the configured modes and reports the selected mode', () => {
        const onChange = vi.fn();

        render(
            <InputModeSelector
                fieldLabel="Bezeichnung"
                controlledFieldId="field-name"
                value="literal"
                onChange={onChange}
            />,
        );

        const trigger = screen.getByRole('button', {name: 'Wert: Eingabemodus für Bezeichnung ändern'});
        expect(trigger).toHaveAttribute('aria-controls', 'field-name');
        fireEvent.click(trigger);
        const menu = document.querySelector('[role="menu"]');
        const variableOption = screen.getByText('Variable').closest('[role="menuitemradio"]');
        expect(menu).toHaveAttribute('aria-label', 'Eingabemodus für Bezeichnung');
        expect(trigger.getAttribute('aria-controls')?.split(' ')).toEqual(['field-name', menu?.id]);
        expect(variableOption).toHaveAttribute('aria-checked', 'false');
        fireEvent.click(variableOption!);

        expect(onChange).toHaveBeenCalledWith('variable');
    });

    it('disables mode selection when only one mode is allowed', () => {
        render(
            <InputModeSelector
                fieldLabel="Bezeichnung"
                value="literal"
                allowedModes={['literal']}
                onChange={vi.fn()}
            />,
        );

        expect(screen.getByRole('button', {name: /Eingabemodus für Bezeichnung/})).toBeDisabled();
    });
});

describe('DynamicTextIndicator', () => {
    it('exposes the capability description to assistive technology', () => {
        render(<DynamicTextIndicator/>);

        expect(screen.getByLabelText(DynamicTextIndicatorLabel)).toBeInTheDocument();
    });
});
