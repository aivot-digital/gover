import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ColorModeContext} from '../../providers/color-mode-context';
import {ColorModePicker} from './color-mode-picker';

describe('ColorModePicker', () => {
    it('shows the resolved mode for the system preference and changes the preference from the menu', () => {
        const setPreference = vi.fn();

        render(
            <ColorModeContext.Provider value={{mode: 'dark', preference: 'system', setPreference}}>
                <ColorModePicker showLabel/>
            </ColorModeContext.Provider>,
        );

        const button = screen.getByLabelText('Darstellung: System-Standard, aktuell dunkel');
        expect(button).toHaveTextContent('System (Dunkel)');

        fireEvent.click(button);
        fireEvent.click(screen.getByText('Hell'));

        expect(setPreference).toHaveBeenCalledWith('light');
    });
});
