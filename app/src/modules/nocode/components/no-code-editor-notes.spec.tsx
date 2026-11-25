import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {NoCodeEditorNotes} from './no-code-editor-notes';

describe('NoCodeEditorNotes', () => {
    it('explains the disabled editor until an expression exists and then restores the regular hint', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        const {rerender} = render(<NoCodeEditorNotes hasExpression={false}
                                                    disabled
                                                    onChange={onChange}/>);

        await user.click(screen.getByRole('button', {name: 'Notizen'}));
        const editor = await screen.findByRole('textbox', {name: 'Notizen zum Ausdruck'});
        const missingExpressionHint = 'Notizen können erst erfasst werden, wenn ein Ausdruck festgelegt ist.';
        expect(screen.getByText(missingExpressionHint)).toBeVisible();
        expect(editor).toHaveAccessibleDescription(missingExpressionHint);
        expect(editor).toHaveAttribute('contenteditable', 'false');
        expect(editor).toHaveAttribute('aria-disabled');

        rerender(<NoCodeEditorNotes hasExpression
                                   disabled={false}
                                   onChange={onChange}/>);

        await waitFor(() => {
            expect(editor).toHaveAttribute('contenteditable', 'true');
            expect(editor).toHaveAccessibleDescription('Interne Hinweise zur fachlichen Bedeutung dieses Ausdrucks.');
        });
        expect(editor).not.toHaveAttribute('aria-disabled');
        expect(screen.queryByText(missingExpressionHint)).not.toBeInTheDocument();
    });

    it('retains the regular hint for an existing expression when editing is disabled', async () => {
        const user = userEvent.setup();
        render(<NoCodeEditorNotes hasExpression
                                  value="Fachliche Ausnahme"
                                  disabled
                                  onChange={vi.fn()}/>);

        await user.click(screen.getByRole('button', {name: 'Notizen anzeigen'}));
        const editor = await screen.findByRole('textbox', {name: 'Notizen zum Ausdruck'});
        expect(editor).toHaveTextContent('Fachliche Ausnahme');
        expect(editor).toHaveAttribute('contenteditable', 'false');
        expect(editor).toHaveAccessibleDescription('Interne Hinweise zur fachlichen Bedeutung dieses Ausdrucks.');
        expect(screen.queryByText('Notizen können erst erfasst werden, wenn ein Ausdruck festgelegt ist.')).not.toBeInTheDocument();
    });
});
