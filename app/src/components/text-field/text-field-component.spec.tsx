import {fireEvent, render, screen} from '@testing-library/react';
import {TextFieldComponent} from './text-field-component';

jest.mock('../copy-to-clipboard-button/copy-to-clipboard-button', () => ({
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
        jest.useRealTimers();
    });

    it('should only render the copy button when copyable is enabled', () => {
        const {rerender} = render(
            <TextFieldComponent
                label="API Key"
                onChange={jest.fn()}
            />,
        );

        expect(screen.queryByTestId('copy-button')).not.toBeInTheDocument();

        rerender(
            <TextFieldComponent
                label="API Key"
                onChange={jest.fn()}
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
                onChange={jest.fn()}
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
        jest.useFakeTimers();
        const onChange = jest.fn();

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
});
