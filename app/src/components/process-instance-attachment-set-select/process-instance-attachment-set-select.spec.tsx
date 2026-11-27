import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ThemeProvider} from '@mui/material';
import {describe, expect, it, vi} from 'vitest';
import {BaseTheme} from '../../theming/base-theme';
import {FormFieldTokens} from '../../theming/form-field-tokens';
import {ProcessInstanceAttachmentSetSelect, type ProcessInstanceAttachmentSetSelectProps} from './process-instance-attachment-set-select';

function renderSelect(props: Partial<ProcessInstanceAttachmentSetSelectProps> = {}) {
    return render(
        <ThemeProvider theme={BaseTheme}>
            <ProcessInstanceAttachmentSetSelect
                label="Anlagensatz"
                attachmentSets={[]}
                value={['summary']}
                onChange={vi.fn()}
                hint="Wählen Sie den Anlagensatz."
                required
                {...props}
            />
        </ThemeProvider>,
    );
}

describe('ProcessInstanceAttachmentSetSelect', () => {
    it.each([1, 3])('uses shared spacing and dimensions with maxItems=%i', (maxItems) => {
        renderSelect({maxItems});

        const input = screen.getByRole('combobox', {name: 'Anlagensatz'});
        expect(input).toBeRequired();
        expect(input).toHaveAccessibleDescription(`Wählen Sie den Anlagensatz. 1/${maxItems}`);
        const textField = input.closest('.MuiTextField-root')!;
        expect(textField).not.toHaveClass('MuiFormControl-marginNormal');
        expect(getComputedStyle(textField).marginTop).toBe('0px');
        expect(getComputedStyle(textField).marginBottom).toBe('0px');
        expect(getComputedStyle(input.closest('.MuiInputBase-root')!).minHeight)
            .toBe(`${FormFieldTokens.controlMinHeight}px`);
        if (maxItems > 1) {
            expect(screen.getByText('summary').closest('.MuiChip-root')).toHaveClass('MuiChip-sizeSmall');
        }
    });

    it.each([1, 3])('keeps the selection clearable with maxItems=%i', async (maxItems) => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        renderSelect({maxItems, onChange});

        await user.click(screen.getByRole('combobox', {name: 'Anlagensatz'}));
        await user.click(screen.getByRole('button', {name: 'Leeren'}));
        expect(onChange).toHaveBeenCalledWith(null);
    });

    it.each([{errors: []}, {errors: [' ', '']}])('does not mark the counter as an error for %j', ({errors}) => {
        renderSelect({maxItems: 1, errors});

        const input = screen.getByRole('combobox', {name: 'Anlagensatz'});
        expect(input).not.toBeInvalid();
        expect(input).toHaveAccessibleDescription('Wählen Sie den Anlagensatz. 1/1');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('associates actual validation errors with the control', () => {
        renderSelect({maxItems: 1, errors: ['Der Anlagensatz ist nicht verfügbar.']});

        const input = screen.getByRole('combobox', {name: 'Anlagensatz'});
        expect(input).toBeInvalid();
        expect(input).toHaveAccessibleDescription('Der Anlagensatz ist nicht verfügbar. 1/1');
        expect(screen.getAllByText('1/1')).toHaveLength(1);
    });

    it.each([{disabled: true}, {readOnly: true}, {busy: true}])('prevents editing when %j', (state) => {
        renderSelect({maxItems: 1, ...state});

        const input = screen.getByRole('combobox', {name: 'Anlagensatz'});
        if ('readOnly' in state) {
            expect(input).toHaveAttribute('readonly');
        } else {
            expect(input).toBeDisabled();
        }
        expect(screen.queryByRole('button', {name: 'Leeren'})).not.toBeInTheDocument();
    });
});
