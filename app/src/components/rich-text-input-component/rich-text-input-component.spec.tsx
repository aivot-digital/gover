import {act, render, screen, waitFor} from '@testing-library/react';
import {createRef} from 'react';
import {describe, expect, it, vi} from 'vitest';
import {RichTextInputComponent, type RichTextInputComponentMethods} from './rich-text-input-component';

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

    it('counts the stored Markdown against the maximum number of characters', async () => {
        render(
            <RichTextInputComponent
                label="Beschreibung"
                value="Hallo **Welt**"
                onChange={vi.fn()}
                hint="Beschreiben Sie Ihr Anliegen."
                maxCharacters={20}
                reducedMode
            />,
        );

        const editor = await screen.findByRole('textbox', {name: 'Beschreibung – optional'});
        expect(editor).not.toHaveAttribute('aria-invalid');
        expect(editor).toHaveAttribute('aria-describedby', expect.stringContaining('helper-text'));
        expect(screen.getByText('Beschreiben Sie Ihr Anliegen.')).toBeInTheDocument();
        expect(screen.getByLabelText('14 von 20 Zeichen verwendet')).toHaveTextContent('14/20');
    });

    it('reports Markdown above the maximum number of characters as an error', async () => {
        render(
            <RichTextInputComponent
                label="Beschreibung"
                value="Hallo **Welt**"
                onChange={vi.fn()}
                hint="Beschreiben Sie Ihr Anliegen."
                maxCharacters={10}
                reducedMode
            />,
        );

        const editor = await screen.findByRole('textbox', {name: 'Beschreibung – optional'});
        await waitFor(() => {
            expect(editor).toHaveAttribute('aria-invalid');
        });
        expect(screen.getByText('Zu viele Zeichen. Maximal 10 Zeichen erlaubt.')).toBeInTheDocument();
        expect(screen.queryByText('Beschreiben Sie Ihr Anliegen.')).not.toBeInTheDocument();
        expect(screen.getByLabelText('14 von 10 Zeichen verwendet')).toHaveTextContent('14/10');
    });

    it('prefers a provided error over the length error', async () => {
        render(
            <RichTextInputComponent
                label="Beschreibung"
                value="Hallo **Welt**"
                onChange={vi.fn()}
                error="Bitte prüfen Sie die Eingabe."
                maxCharacters={10}
                reducedMode
            />,
        );

        await screen.findByRole('textbox', {name: 'Beschreibung – optional'});
        expect(screen.getByText('Bitte prüfen Sie die Eingabe.')).toBeInTheDocument();
        expect(screen.queryByText('Zu viele Zeichen. Maximal 10 Zeichen erlaubt.')).not.toBeInTheDocument();
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

    it('renders editable dynamic-text tokens and exposes the insertion action', async () => {
        const onInsertVariable = vi.fn();
        const {container} = render(
            <RichTextInputComponent
                label="Protokollnachricht"
                value="Hallo {{ $.name }}"
                onChange={vi.fn()}
                dynamicText
                reducedMode
                endAction={{
                    icon: <span>fx</span>,
                    tooltip: 'Variablenreferenz einfügen',
                    onClick: onInsertVariable,
                }}
            />,
        );

        const editor = await screen.findByRole('textbox', {name: 'Protokollnachricht – optional'});
        expect(editor).toHaveAttribute('spellcheck', 'false');
        await waitFor(() => {
            expect(container.querySelector('.dynamic-text-token')).toHaveTextContent('{{ $.name }}');
        });

        screen.getByRole('button', {name: 'Variablenreferenz einfügen'}).click();
        expect(onInsertVariable).toHaveBeenCalledOnce();
    });

    it.each([
        ['editable', {}],
        ['disabled', {disabled: true}],
    ])('renders stored HTML as literal text when %s', async (_, stateProps) => {
        const {container} = render(
            <RichTextInputComponent
                label="Supporthinweis"
                value={[
                    'Bitte wenden Sie sich an den Support.',
                    '',
                    '<script src="https://example.invalid/payload.js"></script>',
                    '',
                    '<iframe src="javascript:alert(1)"></iframe>',
                    '',
                    '<details open ontoggle="alert(1)">Details</details>',
                    '',
                    'Text mit <span onclick="alert(1)">Inline-HTML</span>',
                ].join('\n')}
                onChange={vi.fn()}
                reducedMode
                {...stateProps}
            />,
        );

        const editor = await screen.findByRole('textbox', {name: 'Supporthinweis – optional'});
        await waitFor(() => {
            expect(editor).toHaveTextContent('Bitte wenden Sie sich an den Support.');
        });
        expect(container.querySelector('script, iframe, details, [onclick], [ontoggle]')).toBeNull();
        expect(editor).toHaveTextContent('<script src="https://example.invalid/payload.js"></script>');
        expect(editor).toHaveTextContent('<iframe src="javascript:alert(1)"></iframe>');
        expect(editor).toHaveTextContent('<details open ontoggle="alert(1)">Details</details>');
        expect(editor).toHaveTextContent('Text mit <span onclick="alert(1)">Inline-HTML</span>');
    });

    it('keeps formatting tags and reads indented lines as paragraphs', async () => {
        render(
            <RichTextInputComponent
                label="Supporthinweis"
                value={'Ein <u>unterstrichenes</u> Wort\n\n    Eingerückter Absatz'}
                onChange={vi.fn()}
                reducedMode
            />,
        );

        const editor = await screen.findByRole('textbox', {name: 'Supporthinweis – optional'});
        await waitFor(() => {
            expect(editor).toHaveTextContent('Ein unterstrichenes Wort');
        });
        expect(editor).toHaveTextContent('Eingerückter Absatz');
        expect(editor).not.toHaveTextContent('<u>');
        expect(editor.querySelector('pre, code')).toBeNull();
    });

    it('inserts a variable reference through the shared dynamic-text contract', async () => {
        const ref = createRef<RichTextInputComponentMethods>();
        const {container} = render(
            <RichTextInputComponent
                ref={ref}
                label="Protokollnachricht"
                value="Hallo "
                onChange={vi.fn()}
                dynamicText
                reducedMode
            />,
        );

        act(() => ref.current?.insertVariableReference('$.name'));

        await waitFor(() => {
            expect(container.querySelector('.dynamic-text-token')).toHaveTextContent('{{ $.name }}');
        });
    });
});
