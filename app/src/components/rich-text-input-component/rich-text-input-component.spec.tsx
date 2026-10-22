import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {createRef} from 'react';
import {afterEach, describe, expect, it, onTestFinished, vi} from 'vitest';
import {RichTextInputComponent, type RichTextInputComponentMethods} from './rich-text-input-component';

const appInfo = vi.hoisted(() => ({mode: 'staff'}));
vi.mock('../../app-info', () => ({AppInfo: appInfo}));

afterEach(() => {
    appInfo.mode = 'staff';
    vi.unstubAllGlobals();
});

async function selectEditorText(editor: HTMLElement, text: string, collapsed = false) {
    const walker = document.createTreeWalker(editor, NodeFilter.SHOW_TEXT);
    let node = walker.nextNode();
    while (node != null && node.textContent !== text) {
        node = walker.nextNode();
    }
    expect(node).not.toBeNull();
    await act(async () => {
        editor.focus();
        const range = document.createRange();
        range.selectNodeContents(node!);
        if (collapsed) {
            range.collapse(true);
        }
        const selection = window.getSelection()!;
        selection.removeAllRanges();
        selection.addRange(range);
        fireEvent(document, new Event('selectionchange'));
    });
}

const PROTECTED_MARKDOWN = [
    'Bitte beachten Sie:',
    '',
    '```',
    'Aktenzeichen 2026-0815',
    '```',
    '',
    '---',
    '',
    'Siehe ![Logo](https://example.org/logo.png) und [Antrag][1].',
    '',
    '[1]: https://example.org/antrag',
].join('\n');

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

    it('shows formatting tags and highlight marks as text and reads indented lines as paragraphs', async () => {
        render(
            <RichTextInputComponent
                label="Supporthinweis"
                value={'Ein <u>unterstrichenes</u> und ==markiertes== Wort\n\n    Eingerückter Absatz'}
                onChange={vi.fn()}
                reducedMode
            />,
        );

        const editor = await screen.findByRole('textbox', {name: 'Supporthinweis – optional'});
        // The Markdown renderers show these as text as well.
        await waitFor(() => {
            expect(editor).toHaveTextContent('Ein <u>unterstrichenes</u> und ==markiertes== Wort');
        });
        expect(editor).toHaveTextContent('Eingerückter Absatz');
        expect(editor.querySelector('u, mark, pre, code')).toBeNull();
    });

    it('inserts a formatted variable reference as a raw tag', async () => {
        const ref = createRef<RichTextInputComponentMethods>();
        const {container} = render(
            <RichTextInputComponent
                ref={ref}
                label="Nachricht"
                value="Antwort: "
                onChange={vi.fn()}
                dynamicText
                reducedMode
            />,
        );

        act(() => ref.current?.insertVariableReference('$.nachricht', 'markdown'));

        await waitFor(() => {
            expect(container.querySelector('.dynamic-text-token')).toHaveTextContent('{! $.nachricht !}');
        });
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

    it('applies bold but not underline with the keyboard', async () => {
        const onChange = vi.fn();
        render(<RichTextInputComponent label="Nachricht" value="Wichtig" onChange={onChange} reducedMode/>);
        const editor = await screen.findByRole('textbox', {name: 'Nachricht – optional'});
        await selectEditorText(editor, 'Wichtig');

        fireEvent.keyDown(editor, {key: 'u', code: 'KeyU', ctrlKey: true});
        expect(onChange).not.toHaveBeenCalled();

        fireEvent.keyDown(editor, {key: 'b', code: 'KeyB', ctrlKey: true});
        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith('**Wichtig**'));
    });

    it('drops underline from pasted text and keeps bold', async () => {
        // Lexical tells pasted from dropped data by these event classes, which jsdom does not provide.
        vi.stubGlobal('DragEvent', class DragEvent extends Event {});
        vi.stubGlobal('ClipboardEvent', class ClipboardEvent extends Event {});
        // Pasting scrolls the selection into view, which jsdom cannot measure.
        Range.prototype.getBoundingClientRect = () => new DOMRect();
        onTestFinished(() => {
            delete (Range.prototype as Partial<Range>).getBoundingClientRect;
        });
        const clipboard: Record<string, string> = {
            'text/html': '<b>Fett</b> und <u>unterstrichen</u><sup>2</sup>',
            'text/plain': 'Fett und unterstrichen2',
        };
        const onChange = vi.fn();
        render(<RichTextInputComponent label="Nachricht" value="Hinweis" onChange={onChange} reducedMode/>);
        const editor = await screen.findByRole('textbox', {name: 'Nachricht – optional'});
        await selectEditorText(editor, 'Hinweis');

        fireEvent.paste(editor, {
            clipboardData: {
                types: Object.keys(clipboard),
                files: [],
                getData: (type: string) => clipboard[type] ?? '',
            },
        });

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith('**Fett** und unterstrichen2'));
    });

    it('shows Markdown it cannot edit as protected parts and keeps it when the text is edited', async () => {
        const onChange = vi.fn();
        render(<RichTextInputComponent label="Nachricht" value={PROTECTED_MARKDOWN} onChange={onChange} reducedMode/>);
        const editor = await screen.findByRole('textbox', {name: 'Nachricht – optional'});

        await waitFor(() => expect(editor).toHaveTextContent('Aktenzeichen 2026-0815'));
        const note = 'Diesen Abschnitt können Sie nur in der Markdown-Quelle bearbeiten.';
        // The code block, the horizontal line and the link definition.
        expect(screen.getAllByText(note)).toHaveLength(3);
        // The image and the reference link inside the paragraph.
        expect(screen.getAllByTitle(note)).toHaveLength(2);
        expect(editor).toHaveTextContent('![Logo](https://example.org/logo.png)');
        expect(editor).toHaveTextContent('[Antrag][1]');
        expect(screen.queryByText(/Parsing of the following markdown/)).toBeNull();

        await selectEditorText(editor, 'Bitte beachten Sie:');
        fireEvent.keyDown(editor, {key: 'b', code: 'KeyB', ctrlKey: true});

        await waitFor(() => expect(onChange).toHaveBeenCalled());
        // The Markdown writer spells the horizontal line *** instead of ---, which means the same.
        expect(onChange).toHaveBeenLastCalledWith(PROTECTED_MARKDOWN
            .replace('Bitte beachten Sie:', '**Bitte beachten Sie:**')
            .replace('\n---\n', '\n***\n'));
    });

    it('shows tables as their Markdown lines and keeps them when the text is edited', async () => {
        const onChange = vi.fn();
        const markdown = 'Bitte prüfen\n\n| Feld | Wert |\n|---|---|\n| Ort | *Bonn* |\n\nEnde';
        render(<RichTextInputComponent label="Nachricht" value={markdown} onChange={onChange} reducedMode/>);
        const editor = await screen.findByRole('textbox', {name: 'Nachricht – optional'});

        // The editor has no table editor, so it reads tables as paragraphs. The renderers still show a table.
        await waitFor(() => expect(editor).toHaveTextContent('| Feld | Wert |'));
        await selectEditorText(editor, 'Bitte prüfen');
        fireEvent.keyDown(editor, {key: 'b', code: 'KeyB', ctrlKey: true});

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith(markdown.replace('Bitte prüfen', '**Bitte prüfen**')));
    });

    it('offers customers a short toolbar without source mode, headings or links', async () => {
        appInfo.mode = 'customer';
        render(<RichTextInputComponent label="Nachricht" value={PROTECTED_MARKDOWN} onChange={vi.fn()}/>);
        const editor = await screen.findByRole('textbox', {name: 'Nachricht – optional'});

        await waitFor(() => expect(editor).toHaveTextContent('Aktenzeichen 2026-0815'));
        expect(screen.getAllByText('Diesen Abschnitt können Sie hier nicht bearbeiten.')).toHaveLength(3);
        for (const name of ['Fett', 'Kursiv', 'Aufzählung', 'Nummerierte Liste']) {
            expect(screen.getByRole('radio', {name})).toBeInTheDocument();
        }
        expect(screen.queryByRole('radio', {name: 'Markdown-Quelle'})).toBeNull();
        expect(screen.queryByRole('button', {name: 'Link erstellen'})).toBeNull();
        expect(screen.queryByRole('combobox')).toBeNull();
    });

    it('shows stored links to customers as protected parts with their address', async () => {
        appInfo.mode = 'customer';
        render(<RichTextInputComponent label="Nachricht" value="Siehe [Antrag](https://example.org/antrag)." onChange={vi.fn()}/>);
        const editor = await screen.findByRole('textbox', {name: 'Nachricht – optional'});

        await waitFor(() => expect(editor).toHaveTextContent('Siehe [Antrag](https://example.org/antrag).'));
        expect(screen.getByTitle('Diesen Abschnitt können Sie hier nicht bearbeiten.')).toBeInTheDocument();
        expect(editor.querySelector('a')).toBeNull();
    });

    it('pastes links as plain text in the customer app', async () => {
        appInfo.mode = 'customer';
        vi.stubGlobal('DragEvent', class DragEvent extends Event {});
        vi.stubGlobal('ClipboardEvent', class ClipboardEvent extends Event {});
        Range.prototype.getBoundingClientRect = () => new DOMRect();
        onTestFinished(() => {
            delete (Range.prototype as Partial<Range>).getBoundingClientRect;
        });
        const clipboard: Record<string, string> = {
            'text/html': 'Bitte <a href="https://evil.example">hier bestätigen</a>',
            'text/plain': 'Bitte hier bestätigen',
        };
        const onChange = vi.fn();
        render(<RichTextInputComponent label="Nachricht" value="Hinweis" onChange={onChange}/>);
        const editor = await screen.findByRole('textbox', {name: 'Nachricht – optional'});
        await selectEditorText(editor, 'Hinweis');

        fireEvent.paste(editor, {
            clipboardData: {
                types: Object.keys(clipboard),
                files: [],
                getData: (type: string) => clipboard[type] ?? '',
            },
        });

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith('Bitte hier bestätigen'));
    });

    it('offers source mode to staff', async () => {
        render(<RichTextInputComponent label="Nachricht" value="Hinweis" onChange={vi.fn()}/>);
        await screen.findByRole('textbox', {name: 'Nachricht – optional'});

        expect(screen.getByRole('radio', {name: 'Markdown-Quelle'})).toBeInTheDocument();
    });

    it('does not change the stored Markdown when a field is opened', async () => {
        const onChange = vi.fn();
        render(<RichTextInputComponent label="Nachricht" value={'__Fett__ und <u>Tag</u>\nZeile 2'} onChange={onChange} reducedMode/>);
        const editor = await screen.findByRole('textbox', {name: 'Nachricht – optional'});

        await waitFor(() => expect(editor).toHaveTextContent('Fett und <u>Tag</u>'));
        expect(onChange).not.toHaveBeenCalled();
    });

    it('keeps a single line break when the text is edited', async () => {
        const onChange = vi.fn();
        render(<RichTextInputComponent label="Nachricht" value={'Zeile 1\nZeile 2'} onChange={onChange} reducedMode/>);
        const editor = await screen.findByRole('textbox', {name: 'Nachricht – optional'});
        await waitFor(() => expect(editor).toHaveTextContent('Zeile 1'));

        await selectEditorText(editor, 'Zeile 1\nZeile 2');
        fireEvent.keyDown(editor, {key: 'b', code: 'KeyB', ctrlKey: true});

        // Both renderers show a single line break as a line break, so it must not become a paragraph or hard break.
        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith('**Zeile 1\nZeile 2**'));
    });
});
