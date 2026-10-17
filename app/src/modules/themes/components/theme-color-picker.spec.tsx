import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React, {useState} from 'react';
import {describe, expect, it, vi} from 'vitest';
import {ThemeColorPicker} from './theme-color-picker';

describe('ThemeColorPicker', () => {
    it('updates a valid HEX value', async () => {
        const user = userEvent.setup();
        const handleChange = vi.fn();

        render(
            <ThemeColorPicker
                label="Primärfarbe"
                value="#FF6B43"
                onChange={handleChange}
            />,
        );

        const input = screen.getByLabelText('Primärfarbe');
        await user.clear(input);
        await user.type(input, '123456');

        expect(handleChange).toHaveBeenLastCalledWith('#123456');
    });

    it('keeps HEX, RGB and preset colors synchronized', async () => {
        const user = userEvent.setup({skipHover: true});

        render(<ColorPickerHarness />);

        await user.click(screen.getByLabelText('Primärfarbe visuell auswählen'));

        expect(document.querySelector('[role="dialog"][aria-label="Primärfarbe auswählen"]')).toBeInTheDocument();
        expect(screen.getByLabelText('Rotwert')).toHaveValue(255);
        expect(screen.getByLabelText('Grünwert')).toHaveValue(107);
        expect(screen.getByLabelText('Blauwert')).toHaveValue(67);

        await user.click(screen.getByLabelText('Grünwert'));
        await user.keyboard('200');

        expect(screen.getByLabelText('Primärfarbe')).toHaveValue('#FFC843');

        await user.click(screen.getByLabelText('Blau: #2196F3'));

        expect(screen.getByLabelText('Primärfarbe')).toHaveValue('#2196F3');
        expect(screen.getByLabelText('Rotwert')).toHaveValue(33);
        expect(screen.getByLabelText('Grünwert')).toHaveValue(150);
        expect(screen.getByLabelText('Blauwert')).toHaveValue(243);
        expect(document.querySelectorAll('[aria-pressed]')).toHaveLength(18);
        expect(screen.getByLabelText('Standard-Sekundärfarbe im dunklen Farbschema: #A0C9CB'))
            .toBeInTheDocument();
        expect(screen.getByLabelText('Blaugrau: #607D8B')).toBeInTheDocument();
    });

    it('does not open the visual picker when disabled', () => {
        render(
            <ThemeColorPicker
                label="Primärfarbe"
                value="#FF6B43"
                onChange={vi.fn()}
                disabled
            />,
        );

        const pickerButton = screen.getByLabelText('Primärfarbe visuell auswählen');
        expect(pickerButton).toBeDisabled();
        expect(document.querySelector('[role="dialog"]')).not.toBeInTheDocument();
    });

    it('shows the configured color contrast against the standard paper surface', () => {
        render(
            <ThemeColorPicker
                label="Primärfarbe"
                value="#FF6B43"
                contrastTextColor="#000000"
                contrastBackgroundColor="#FFFFFF"
                onChange={vi.fn()}
            />,
        );

        expect(screen.getByText(/Gefüllt mit schwarzer Schrift: 7,44:1/)).toBeInTheDocument();
        expect(screen.getByText(/Symbol auf Standardfläche: 2,82:1/)).toBeInTheDocument();
        expect(screen.queryByText(/WCAG/)).not.toBeInTheDocument();
    });
});

function ColorPickerHarness() {
    const [value, setValue] = useState('#FF6B43');

    return (
        <ThemeColorPicker
            label="Primärfarbe"
            value={value}
            onChange={setValue}
        />
    );
}
