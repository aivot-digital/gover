import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {CodeInputFieldComponent} from './code-input-field-component';

vi.mock('../code-editor/code-editor', () => ({
    CodeEditor: (props: {
        id?: string;
        ariaLabel?: string;
        ariaLabelledBy?: string;
        ariaDescribedBy?: string;
        value?: string | null;
    }) => (
        <textarea
            id={props.id}
            aria-label={props.ariaLabelledBy == null ? props.ariaLabel : undefined}
            aria-labelledby={props.ariaLabelledBy}
            aria-describedby={props.ariaDescribedBy}
            value={props.value ?? ''}
            readOnly
        />
    ),
}));

describe('CodeInputFieldComponent', () => {
    it('keeps the field label, helper and expand action accessible', () => {
        render(
            <CodeInputFieldComponent
                label="Validierungslogik"
                value="return true;"
                onChange={vi.fn()}
                hint="Die Funktion muss einen Wahrheitswert zurückgeben."
                labelAction={<button type="button">Eingabemodus</button>}
            />,
        );

        const label = screen.getByTitle('Validierungslogik');
        const editor = document.getElementById(label.getAttribute('for')!);
        const expand = screen.getByLabelText('Validierungslogik: In großem Editor öffnen');

        expect(editor).toHaveAccessibleName('Validierungslogik – optional');
        expect(editor).toHaveAccessibleDescription('Die Funktion muss einen Wahrheitswert zurückgeben.');
        expect(editor).toHaveValue('return true;');
        expect(expand).toHaveAttribute('aria-haspopup', 'dialog');
        expect(expand).toHaveAttribute('aria-expanded', 'false');
        expect(expand).not.toHaveAttribute('aria-controls');
        expect(screen.getByRole('button', {name: 'Eingabemodus'})).toBeInTheDocument();

        fireEvent.click(expand);

        expect(document.querySelector('[role="dialog"]')).toBeInTheDocument();
        expect(expand).toHaveAttribute('aria-expanded', 'true');
        expect(expand).toHaveAttribute('aria-controls');
        expect(document.querySelector('textarea[aria-label="Validierungslogik im großen Editor"]'))
            .toHaveAccessibleDescription('Die Funktion muss einen Wahrheitswert zurückgeben.');
    });
});
