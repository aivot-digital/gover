import {fireEvent, render, screen, within} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {NoCodeOperandEditorSelector} from './no-code-operand-editor-selector';

describe('NoCodeOperandEditorSelector', () => {
    it('explains a kind of operand with the texts shared with the pill editor', async () => {
        render(<NoCodeOperandEditorSelector allOperators={[]} label="Wert" onChange={vi.fn()}/>);

        // The help button follows the button of its kind in the same group.
        const group = screen.getByRole('button', {name: 'Wert eines Elements'}).parentElement!;
        fireEvent.click(within(group).getAllByRole('button')[1]);

        const dialog = await screen.findByRole('dialog', {name: 'Wert eines Elements'});
        expect(dialog).toHaveTextContent('auf Werte zugreifen, die in anderen Feldern');
    });
});
