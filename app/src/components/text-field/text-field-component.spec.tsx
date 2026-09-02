import {afterEach, describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import {AutocompleteTextField, TextFieldComponent} from './text-field-component';

vi.mock('../copy-to-clipboard-button/copy-to-clipboard-button', () => ({
    CopyToClipboardButton: ({text, disabled, ariaLabel}: {
        text: string;
        disabled?: boolean;
        ariaLabel?: string;
    }) => (
        <button
            type="button"
            data-testid="copy-button"
            data-text={text}
            disabled={disabled}
            aria-label={ariaLabel ?? 'copy'}
        >
            copy
        </button>
    ),
}));

describe('TextFieldComponent', () => {
    afterEach(() => {
        vi.useRealTimers();
    });

    it('should only render the copy button when copyable is enabled', () => {
        const {rerender} = render(
            <TextFieldComponent
                label="API Key"
                onChange={vi.fn()}
            />,
        );

        expect(screen.queryByTestId('copy-button')).not.toBeInTheDocument();

        rerender(
            <TextFieldComponent
                label="API Key"
                onChange={vi.fn()}
                copyable
            />,
        );

        expect(screen.getByTestId('copy-button')).toBeDisabled();
    });

    it('should preserve an existing end adornment when the copy button is enabled', () => {
        render(
            <TextFieldComponent
                label="API Key"
                value="secret-value"
                onChange={vi.fn()}
                copyable
                muiPassTroughProps={{
                    slotProps: {
                        input: {
                            endAdornment: <span data-testid="existing-end-adornment">existing</span>,
                        },
                    },
                }}
            />,
        );

        expect(screen.getByTestId('copy-button')).toBeInTheDocument();
        expect(screen.getByTestId('existing-end-adornment')).toBeInTheDocument();
    });

    it('should resolve and preserve callback-based pass-through slot props', () => {
        const inputSlot = vi.fn(() => ({
            endAdornment: <span data-testid="existing-end-adornment">existing</span>,
            readOnly: true,
        }));
        const htmlInputSlot = vi.fn(() => ({
            'data-testid': 'native-input',
        }));
        render(
            <TextFieldComponent
                label="API Key"
                hint="Keep this value private"
                value="secret-value"
                onChange={vi.fn()}
                copyable
                muiPassTroughProps={{
                    slotProps: {
                        input: inputSlot,
                        htmlInput: htmlInputSlot,
                    },
                }}
            />,
        );

        expect(inputSlot).toHaveBeenCalled();
        expect(htmlInputSlot).toHaveBeenCalled();
        expect(screen.getByTestId('copy-button')).toBeInTheDocument();
        expect(screen.getByTestId('existing-end-adornment')).toBeInTheDocument();
        expect(screen.getByTestId('native-input')).toHaveAccessibleName('API Key – optional');
        expect(screen.getByTestId('native-input')).toHaveAccessibleDescription('Keep this value private');
        expect(screen.getByTestId('native-input')).toHaveAttribute('readonly');
        expect(screen.getByTestId('native-input')).not.toHaveAttribute('aria-labelledby');
        expect(screen.getByTestId('native-input').closest('.MuiInputBase-root')).toHaveClass('MuiInputBase-sizeSmall');
    });

    it('applies sx to the field root and controlSx to the input control', () => {
        const {container} = render(
            <TextFieldComponent
                label="API Key"
                onChange={vi.fn()}
                sx={{width: '321px'}}
                controlSx={{backgroundColor: 'rgb(1, 2, 3)'}}
            />,
        );

        const fieldRoot = container.querySelector<HTMLElement>('[data-form-field]');
        const controlRoot = screen.getByRole('textbox', {name: 'API Key – optional'})
            .closest<HTMLElement>('.MuiTextField-root');

        expect(getComputedStyle(fieldRoot!).width).toBe('321px');
        expect(getComputedStyle(controlRoot!).backgroundColor).toBe('rgb(1, 2, 3)');
        expect(getComputedStyle(fieldRoot!).backgroundColor).not.toBe('rgb(1, 2, 3)');
    });

    it('should copy the live input value before the debounced change is flushed', () => {
        vi.useFakeTimers();
        const onChange = vi.fn();

        render(
            <TextFieldComponent
                label="API Key"
                value="initial"
                onChange={onChange}
                copyable
                debounce={1000}
            />,
        );

        fireEvent.change(screen.getByRole('textbox', {name: 'API Key – optional'}), {
            target: {
                value: 'draft-value',
            },
        });

        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByTestId('copy-button')).toHaveAttribute('data-text', 'draft-value');
    });

    it('should not emit a debounced value again when the input blurs after the timer elapsed', () => {
        vi.useFakeTimers();
        const onChange = vi.fn();

        render(
            <TextFieldComponent
                label="Name"
                value=""
                onChange={onChange}
                debounce={100}
            />,
        );

        const input = screen.getByRole('textbox', {name: 'Name – optional'});
        fireEvent.change(input, {target: {value: 'Prosuna'}});
        vi.advanceTimersByTime(100);

        expect(onChange).toHaveBeenCalledTimes(1);
        expect(onChange).toHaveBeenLastCalledWith('Prosuna');

        fireEvent.blur(input);
        expect(onChange).toHaveBeenCalledTimes(1);
    });

    it('should trim the displayed and emitted value on blur', () => {
        const onChange = vi.fn();

        render(
            <TextFieldComponent
                label="Name"
                value=""
                onChange={onChange}
            />,
        );

        const input = screen.getByRole('textbox', {name: 'Name – optional'});
        fireEvent.change(input, {target: {value: '  Prosuna  '}});
        fireEvent.blur(input);

        expect(onChange).toHaveBeenLastCalledWith('Prosuna');
        expect(input).toHaveValue('Prosuna');
    });

    it('should copy the templated live input value when a copy value template is configured', () => {
        render(
            <TextFieldComponent
                label="URL-Segment"
                value="antrag"
                onChange={vi.fn()}
                copyable
                copyValueTemplate="https://example.test/form/process/{value}/"
            />,
        );

        expect(screen.getByTestId('copy-button')).toHaveAttribute('data-text', 'https://example.test/form/process/antrag/');
    });

    it('should emit null when the user clears the value', () => {
        const onChange = vi.fn();

        render(
            <TextFieldComponent
                label="Name"
                value="Ma"
                onChange={onChange}
            />,
        );

        fireEvent.change(screen.getByRole('textbox', {name: 'Name – optional'}), {
            target: {
                value: '',
            },
        });

        expect(onChange).toHaveBeenCalledWith(null);
    });

    it('should switch between input and display mode without changing the hook order', () => {
        const baseProps = {
            label: 'Name',
            value: 'Example',
            onChange: vi.fn(),
        };
        const {rerender} = render(<TextFieldComponent {...baseProps}/>);

        expect(screen.getByRole('textbox', {name: 'Name – optional'})).toBeInTheDocument();

        rerender(<TextFieldComponent {...baseProps} display/>);
        expect(screen.queryByRole('textbox', {name: 'Name – optional'})).not.toBeInTheDocument();
        expect(screen.getByText('Example')).toBeInTheDocument();

        rerender(<TextFieldComponent {...baseProps}/>);
        expect(screen.getByRole('textbox', {name: 'Name – optional'})).toBeInTheDocument();
    });

    it('should autosize multiline inputs within the default row limits', () => {
        const inputSlot = vi.fn(() => ({}));

        render(
            <TextFieldComponent
                label="Beschreibung"
                multiline
                onChange={vi.fn()}
                muiPassTroughProps={{
                    slotProps: {
                        input: inputSlot,
                    },
                }}
            />,
        );

        expect(inputSlot).toHaveBeenCalledWith(expect.objectContaining({
            multiline: true,
            rows: undefined,
            minRows: 4,
            maxRows: 12,
        }));
    });

    it('should preserve explicitly configured fixed rows for multiline inputs', () => {
        const inputSlot = vi.fn(() => ({}));

        render(
            <TextFieldComponent
                label="Beschreibung"
                multiline
                rows={6}
                onChange={vi.fn()}
                muiPassTroughProps={{
                    slotProps: {
                        input: inputSlot,
                    },
                }}
            />,
        );

        expect(inputSlot).toHaveBeenCalledWith(expect.objectContaining({
            multiline: true,
            rows: 6,
            minRows: undefined,
            maxRows: undefined,
        }));
    });

    it('should preserve the autocomplete input bindings with the external label', () => {
        render(
            <AutocompleteTextField
                id="city"
                label="Ort"
                value={null}
                suggestions={['Berlin', 'Hamburg']}
                onChange={vi.fn()}
                hint="Beginnen Sie mit der Eingabe."
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Ort – optional'});
        expect(input).toHaveAttribute('id', 'city');
        expect(input).toHaveAttribute('aria-autocomplete', 'list');
        expect(input).toHaveAccessibleDescription('Beginnen Sie mit der Eingabe.');
    });

    it('should preserve a free-text autocomplete value when it is confirmed', () => {
        const onChange = vi.fn();

        render(
            <AutocompleteTextField
                label="Ort"
                value={null}
                suggestions={['Berlin', 'Hamburg']}
                onChange={onChange}
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Ort – optional'});
        fireEvent.change(input, {target: {value: 'Leipzig'}});
        fireEvent.keyDown(input, {key: 'Enter'});

        expect(onChange).toHaveBeenLastCalledWith('Leipzig');
        expect(onChange).not.toHaveBeenCalledWith(null);
    });
});
