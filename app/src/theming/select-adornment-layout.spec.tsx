import {describe, expect, it, vi} from 'vitest';
import {render, screen} from '@testing-library/react';
import {Autocomplete, IconButton, InputAdornment, MenuItem, TextField, ThemeProvider} from '@mui/material';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {BaseTheme} from './base-theme';

const selectedLabel = 'A selected option whose complete label is much longer than the available field width';

interface DropdownFixtureProps {
    actionCount: number;
    startIcon?: boolean;
    size?: 'small' | 'medium';
    additionalAdornment?: boolean;
}

function DropdownFixture({actionCount, startIcon, size = 'small', additionalAdornment}: DropdownFixtureProps) {
    return (
        <ThemeProvider theme={BaseTheme}>
            <SelectFieldComponent
                label="Category"
                value="selected"
                onChange={vi.fn()}
                options={[{value: 'selected', label: selectedLabel}]}
                size={size}
                startIcon={startIcon ? <span aria-hidden="true">S</span> : undefined}
                endAction={actionCount > 0 ? Array.from({length: actionCount}, (_, index) => ({
                    icon: <span aria-hidden="true">{index}</span>,
                    ariaLabel: `Action ${index + 1}`,
                    onClick: vi.fn(),
                })) : undefined}
                muiPassTroughProps={additionalAdornment ? {
                    slotProps: {
                        input: {
                            endAdornment: (
                                <InputAdornment position="end">
                                    <IconButton aria-label="Additional action">A</IconButton>
                                </InputAdornment>
                            ),
                        },
                    },
                } : undefined}
            />
        </ThemeProvider>
    );
}

function getInputRoot(control: HTMLElement): HTMLElement {
    const root = control.closest<HTMLElement>('.MuiInputBase-root');
    expect(root).not.toBeNull();
    return root!;
}

function expectFlexibleSelect(control: HTMLElement) {
    const style = getComputedStyle(control);
    expect(style.flexGrow).toBe('1');
    expect(style.flexBasis).toBe('0%');
    expect(style.minWidth).toBe('0px');
    expect(style.paddingRight).toBe('0px');
    expect(style.textOverflow).toBe('ellipsis');
    expect(style.overflow).toBe('hidden');
    expect(style.whiteSpace).toBe('nowrap');
    expect(getComputedStyle(getInputRoot(control)).paddingRight).toBe('32px');
}

describe('Select adornment layout', () => {
    it.each([
        {actionCount: 1, startIcon: false, size: 'small' as const},
        {actionCount: 3, startIcon: false, size: 'small' as const},
        {actionCount: 1, startIcon: true, size: 'small' as const},
        {actionCount: 3, startIcon: true, size: 'small' as const},
        {actionCount: 3, startIcon: true, size: 'medium' as const},
    ])('keeps actions in the flow and selected text flexible for %j', (props) => {
        render(<DropdownFixture {...props}/>);

        const control = screen.getByRole('combobox');
        const root = getInputRoot(control);
        const endAdornment = screen.getByRole('button', {name: 'Action 1'}).closest('.MuiInputAdornment-root')!;

        expectFlexibleSelect(control);
        expect(getComputedStyle(endAdornment).position).toBe('static');
        expect(getComputedStyle(endAdornment).transform).toBe('none');
        expect(getComputedStyle(endAdornment).flexShrink).toBe('0');
        expect(getComputedStyle(endAdornment).marginRight).toBe('0px');
        expect(screen.getAllByRole('button')).toHaveLength(props.actionCount);
        expect(getComputedStyle(root.querySelector('.MuiSelect-icon')!).right).toBe('7px');

        if (props.startIcon) {
            const startAdornment = root.querySelector('.MuiInputAdornment-positionStart')!;
            expect(getComputedStyle(startAdornment).flexShrink).toBe('0');
            expect(getComputedStyle(root).paddingLeft).toBe('14px');
            expect(getComputedStyle(control).paddingLeft).toBe('0px');
        } else {
            expect(getComputedStyle(control).paddingLeft).toBe('14px');
        }
    });

    it('allocates space for separate trailing adornments', () => {
        render(<DropdownFixture actionCount={3} startIcon additionalAdornment/>);

        const control = screen.getByRole('combobox');
        expectFlexibleSelect(control);
        const endAdornments = getInputRoot(control).querySelectorAll('.MuiInputAdornment-positionEnd');
        expect(endAdornments).toHaveLength(2);
        for (const adornment of endAdornments) {
            expect(getComputedStyle(adornment).position).toBe('static');
            expect(getComputedStyle(adornment).flexShrink).toBe('0');
        }
    });

    it('responds to actions being added, changed, and removed', () => {
        const {rerender} = render(<DropdownFixture actionCount={0}/>);
        let control = screen.getByRole('combobox');
        expect(getComputedStyle(getInputRoot(control)).paddingRight).not.toBe('32px');
        expect(getComputedStyle(control).width).toBe('100%');

        for (const actionCount of [1, 3]) {
            rerender(<DropdownFixture actionCount={actionCount}/>);
            control = screen.getByRole('combobox');
            expectFlexibleSelect(control);
            expect(screen.getAllByRole('button')).toHaveLength(actionCount);
        }

        rerender(<DropdownFixture actionCount={0}/>);
        control = screen.getByRole('combobox');
        expect(getComputedStyle(getInputRoot(control)).paddingRight).not.toBe('32px');
        expect(getComputedStyle(control).width).toBe('100%');
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('applies to raw TextField selects using the same theme', () => {
        render(
            <ThemeProvider theme={BaseTheme}>
                <TextField select label="Raw category" value="selected" slotProps={{input: {
                    endAdornment: <InputAdornment position="end"><IconButton aria-label="Raw action">A</IconButton></InputAdornment>,
                }}}>
                    <MenuItem value="selected">{selectedLabel}</MenuItem>
                </TextField>
            </ThemeProvider>,
        );

        expectFlexibleSelect(screen.getByRole('combobox'));
        const adornment = screen.getByRole('button', {name: 'Raw action'}).closest('.MuiInputAdornment-root')!;
        expect(getComputedStyle(adornment).position).toBe('static');
    });

    it('preserves ordinary text input adornments', () => {
        render(
            <ThemeProvider theme={BaseTheme}>
                <TextField label="Text" value="text" slotProps={{input: {
                    endAdornment: <InputAdornment position="end" sx={{mr: 2}}>Suffix</InputAdornment>,
                }}}/>
            </ThemeProvider>,
        );

        const control = screen.getByRole('textbox');
        const rootStyle = getComputedStyle(getInputRoot(control));
        expect(rootStyle.paddingRight).toBe('var(--_trailingPad)');
        expect(rootStyle.getPropertyValue('--_trailingPad')).toBe('14px');
        expect(getComputedStyle(screen.getByText('Suffix').closest('.MuiInputAdornment-root')!).marginRight).toBe('16px');
    });

    it('preserves native Select adornment positioning', () => {
        render(
            <ThemeProvider theme={BaseTheme}>
                <TextField select label="Native category" value="selected" slotProps={{
                    select: {native: true},
                    input: {endAdornment: <InputAdornment position="end">Suffix</InputAdornment>},
                }}>
                    <option value="selected">{selectedLabel}</option>
                </TextField>
            </ThemeProvider>,
        );

        const control = screen.getByRole('combobox');
        expect(control.tagName).toBe('SELECT');
        expect(getComputedStyle(getInputRoot(control)).paddingRight).not.toBe('32px');
        expect(getComputedStyle(screen.getByText('Suffix').closest('.MuiInputAdornment-root')!).position).toBe('absolute');
    });

    it('preserves Autocomplete popup and clear positioning', () => {
        render(
            <ThemeProvider theme={BaseTheme}>
                <Autocomplete
                    options={[selectedLabel]}
                    value={selectedLabel}
                    renderInput={(params) => <TextField {...params} label="Search category"/>}
                />
            </ThemeProvider>,
        );

        const control = screen.getByRole('combobox');
        expect(control.tagName).toBe('INPUT');
        expect(getComputedStyle(getInputRoot(control)).paddingRight).toBe('65px');
        expect(getComputedStyle(screen.getByRole('button', {name: 'Öffnen'}).parentElement!).position).toBe('absolute');
    });
});
