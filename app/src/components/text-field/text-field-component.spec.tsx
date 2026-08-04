import {afterEach, describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import {TextFieldComponent} from './text-field-component';

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
                    InputProps: {
                        endAdornment: <span data-testid="existing-end-adornment">existing</span>,
                    },
                }}
            />,
        );

        expect(screen.getByTestId('copy-button')).toBeInTheDocument();
        expect(screen.getByTestId('existing-end-adornment')).toBeInTheDocument();
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

        fireEvent.change(screen.getByRole('textbox', {name: 'API Key'}), {
            target: {
                value: 'draft-value',
            },
        });

        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByTestId('copy-button')).toHaveAttribute('data-text', 'draft-value');
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

        fireEvent.change(screen.getByRole('textbox', {name: 'Name'}), {
            target: {
                value: '',
            },
        });

        expect(onChange).toHaveBeenCalledWith(null);
    });
});
