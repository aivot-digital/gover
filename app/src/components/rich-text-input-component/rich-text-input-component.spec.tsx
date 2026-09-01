import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {RichTextInputComponent} from './rich-text-input-component';

describe('RichTextInputComponent', () => {
    it('associates the generated contenteditable with label and helper text', async () => {
        const {container} = render(
            <RichTextInputComponent
                label="Bearbeitungshinweis"
                value="Bitte prüfen."
                onChange={vi.fn()}
                hint="Formatierter Hinweis für die Sachbearbeitung."
                reducedMode
            />,
        );

        const editor = await screen.findByRole('textbox', {name: 'Bearbeitungshinweis – optional'});
        expect(editor).toHaveAccessibleDescription('Formatierter Hinweis für die Sachbearbeitung.');
        expect(editor).toHaveAttribute('aria-multiline', 'true');
        expect(editor).not.toHaveAttribute('aria-required');

        const toolbar = container.querySelector<HTMLElement>(
            '.prosuna-mdx-editor [class*="_toolbarRoot_"]',
        );
        expect(toolbar).not.toBeNull();
        expect(getComputedStyle(toolbar!).height).toBe('44px');
        expect(getComputedStyle(toolbar!).paddingBlock).toBe('3px');
    });

    it('does not expose empty error or helper references', async () => {
        render(
            <RichTextInputComponent
                label="Bearbeitungshinweis"
                value=""
                onChange={vi.fn()}
                error=""
                hint=""
                reducedMode
            />,
        );

        const editor = await screen.findByRole('textbox', {name: 'Bearbeitungshinweis – optional'});
        expect(editor).not.toHaveAttribute('aria-invalid');
        expect(editor).not.toHaveAttribute('aria-describedby');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
});
