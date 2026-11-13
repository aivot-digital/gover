import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ThemeProvider} from '@mui/material';
import {useState} from 'react';
import {describe, expect, it, vi} from 'vitest';
import {ElementWidthSelector} from './element-width-selector';
import {ElementType} from '../../data/element-type/element-type';
import {BaseTheme} from '../../theming/base-theme';

function ControlledSelector({elementType = ElementType.Text, onChange = vi.fn()}) {
    const [value, setValue] = useState(6);
    return <ElementWidthSelector label="Elementbreite" elementType={elementType} value={value}
                                 onChange={next => { setValue(next); onChange(next); }}/>;
}

describe('ElementWidthSelector', () => {
    it('shows all six options in a labelled group with accessible helper text', () => {
        render(
            <ThemeProvider theme={BaseTheme}>
                <ElementWidthSelector label="Elementbreite" elementType={ElementType.Text} value={6}
                                      onChange={vi.fn()} hint="Bestimmt die Breite des Elements." margin="none"/>
            </ThemeProvider>,
        );

        expect(screen.getByRole('radiogroup', {name: 'Elementbreite'}))
            .toHaveAccessibleDescription('Bestimmt die Breite des Elements.');
        expect(screen.getAllByRole('radio').map(radio => radio.closest('label')?.textContent))
            .toEqual(['25 %', '33 %', '50 %', '66 %', '75 %', '100 %']);
        expect(screen.getByRole('radio', {name: '50 %'})).toBeChecked();
        expect(screen.getByRole('radio', {name: '50 %'}))
            .toHaveAccessibleDescription('Bestimmt die Breite des Elements.');
        expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    });

    it('selects a tile directly and never clears the selected width on a second click', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<ControlledSelector onChange={onChange}/>);
        await user.click(screen.getByText('75 %'));
        expect(screen.getByRole('radio', {name: '75 %'})).toBeChecked();
        expect(screen.getByRole('radio', {name: '50 %'})).not.toBeChecked();
        expect(onChange).toHaveBeenCalledExactlyOnceWith(9);
        await user.click(screen.getByText('75 %'));
        expect(screen.getByRole('radio', {name: '75 %'})).toBeChecked();
        expect(onChange).toHaveBeenCalledTimes(1);
    });

    it('navigates between widths with the keyboard and leaves the group with Tab', async () => {
        const user = userEvent.setup();
        render(<><ControlledSelector/><button>Weiter</button></>);
        await user.tab();
        expect(screen.getByRole('radio', {name: '50 %'})).toHaveFocus();
        await user.keyboard('{ArrowRight}');
        expect(screen.getByRole('radio', {name: '66 %'})).toHaveFocus();
        expect(screen.getByRole('radio', {name: '66 %'})).toBeChecked();
        await user.keyboard('{ArrowLeft}');
        expect(screen.getByRole('radio', {name: '50 %'})).toBeChecked();
        await user.tab();
        expect(screen.getByRole('button', {name: 'Weiter'})).toHaveFocus();
    });

    it('keeps restricted widths visible but skips them when choosing by mouse or keyboard', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<ControlledSelector elementType={ElementType.Duration} onChange={onChange}/>);
        expect(screen.getByRole('radio', {name: '25 %'})).toBeDisabled();
        expect(screen.getByRole('radio', {name: '33 %'})).toBeDisabled();
        expect(screen.getByRole('radiogroup')).toHaveAccessibleDescription(
            'Für diesen Elementtyp ist mindestens eine Breite von 50 % erforderlich.',
        );
        await user.click(screen.getByText('25 %'));
        expect(onChange).not.toHaveBeenCalled();
        await user.tab();
        expect(screen.getByRole('radio', {name: '50 %'})).toHaveFocus();
        await user.keyboard('{ArrowLeft}');
        expect(screen.getByRole('radio', {name: '100 %'})).toHaveFocus();
        expect(onChange).toHaveBeenCalledExactlyOnceWith(12);
    });

    it('disables every tile when editing is disabled', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<ElementWidthSelector label="Elementbreite" elementType={ElementType.Text} value={6}
                                     onChange={onChange} disabled/>);
        for (const radio of screen.getAllByRole('radio')) expect(radio).toBeDisabled();
        await user.click(screen.getByText('100 %'));
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByRole('radio', {name: '50 %'})).toBeChecked();
    });

    it('keeps multiple selectors independent', async () => {
        const user = userEvent.setup();
        const firstChange = vi.fn();
        const secondChange = vi.fn();
        render(<><ControlledSelector onChange={firstChange}/><ControlledSelector onChange={secondChange}/></>);
        await user.click(screen.getAllByRole('radio', {name: '75 %'})[0]);
        expect(screen.getAllByRole('radio', {name: '75 %'})[0]).toBeChecked();
        expect(screen.getAllByRole('radio', {name: '50 %'})[1]).toBeChecked();
        expect(firstChange).toHaveBeenCalledExactlyOnceWith(9);
        expect(secondChange).not.toHaveBeenCalled();
    });

    it('follows external value and type changes without emitting updates', () => {
        const onChange = vi.fn();
        const {rerender} = render(<ElementWidthSelector label="Elementbreite" elementType={ElementType.Text}
                                                       value={9} onChange={onChange}/>);
        expect(screen.getByRole('radio', {name: '75 %'})).toBeChecked();
        rerender(<ElementWidthSelector label="Elementbreite" elementType={ElementType.Text}
                                       value={3} onChange={onChange}/>);
        expect(screen.getByRole('radio', {name: '25 %'})).toBeChecked();
        rerender(<ElementWidthSelector label="Elementbreite" elementType={ElementType.Duration}
                                       value={3} onChange={onChange}/>);
        expect(screen.getByRole('radio', {name: '25 %'})).toBeDisabled();
        expect(screen.getByRole('radio', {name: '50 %'})).toBeChecked();
        expect(onChange).not.toHaveBeenCalled();
    });

    it.each([
        [ElementType.Text, null, '100 %'],
        [ElementType.Duration, 3, '50 %'],
        [ElementType.Table, 3, '33 %'],
    ] as const)('preserves width normalization for %s and %s without updating stored values', (elementType, value, label) => {
        const onChange = vi.fn();
        render(<ElementWidthSelector label="Elementbreite" elementType={elementType} value={value} onChange={onChange}/>);
        expect(screen.getByRole('radio', {name: label})).toBeChecked();
        expect(onChange).not.toHaveBeenCalled();
    });
});
