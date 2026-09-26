import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Dialog, DialogContent, DialogTitle} from '@mui/material';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {RichTextInputComponent} from './rich-text-input-component';

beforeEach(() => {
    vi.stubGlobal('ResizeObserver', class {
        observe() {}
        unobserve() {}
        disconnect() {}
    });
    const createRange = document.createRange.bind(document);
    vi.spyOn(document, 'createRange').mockImplementation(() => {
        const range = createRange();
        range.getBoundingClientRect = () => new DOMRect(450, 300, 100, 20);
        range.getClientRects = () => [range.getBoundingClientRect()] as unknown as DOMRectList;
        return range;
    });
});

afterEach(() => vi.unstubAllGlobals());

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

function renderEditor(value = 'Linktext') {
    const onChange = vi.fn();
    render(<>
        <section aria-label="Scrollable editor">
            <RichTextInputComponent label="Bearbeitungshinweis" value={value} onChange={onChange} reducedMode />
        </section>
        <input aria-label="Outside input" />
        <div>Outside text</div>
    </>);
    return {onChange};
}

async function openNewLink(user: ReturnType<typeof userEvent.setup>) {
    const editor = await screen.findByRole('textbox', {name: 'Bearbeitungshinweis – optional'});
    await selectEditorText(editor, 'Linktext');
    await user.click(screen.getByRole('button', {name: 'Link erstellen'}));
    return screen.findByRole('textbox', {name: ''});
}

describe('Rich text link dialogs', () => {
    it.each([
        {markdown: '[Linktext](https://example.org)', selectedText: 'Linktext'},
        {markdown: '[Link **formatted** text](https://example.org)', selectedText: 'formatted'},
    ])('centers preview and edit on the entire link: $markdown', async ({markdown, selectedText}) => {
        const user = userEvent.setup();
        vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (this: HTMLElement) {
            if (this.matches('[class*="_linkDialogAnchor_"]')) {
                return new DOMRect(parseFloat(this.style.left), parseFloat(this.style.top),
                    parseFloat(this.style.width), parseFloat(this.style.height));
            }
            if (this.tagName === 'A') {
                return new DOMRect(450, 300, 240, 40);
            }
            if (this.closest('a') != null) {
                return new DOMRect(600, 320, 90, 20);
            }
            return new DOMRect();
        });
        renderEditor(markdown);
        const editor = await screen.findByRole('textbox', {name: 'Bearbeitungshinweis – optional'});
        await selectEditorText(editor, selectedText, true);
        const editButton = await screen.findByRole('button', {name: 'Link-URL bearbeiten'});
        const getAnchor = () => screen.getByRole('region', {name: 'Scrollable editor'})
            .querySelector<HTMLElement>('[class*="_linkDialogAnchor_"]')!;

        expect(getAnchor().getBoundingClientRect()).toMatchObject({left: 450, top: 300, width: 240, height: 40});
        await user.click(editButton);
        await screen.findByRole('button', {name: 'URL übernehmen'});
        expect(getAnchor().getBoundingClientRect()).toMatchObject({left: 450, top: 300, width: 240, height: 40});
    });

    it('keeps the selected text bounds when creating a new link', async () => {
        const user = userEvent.setup();
        renderEditor();
        await openNewLink(user);
        const anchor = screen.getByRole('region', {name: 'Scrollable editor'})
            .querySelector<HTMLElement>('[class*="_linkDialogAnchor_"]')!;

        expect(anchor.style.left).toBe('450px');
        expect(anchor.style.width).toBe('100px');
    });

    it('discards a new link draft on outside click without moving focus back', async () => {
        const user = userEvent.setup();
        const {onChange} = renderEditor();
        const url = await openNewLink(user);
        await user.type(url, 'https://example.org');
        await user.click(screen.getByRole('textbox', {name: 'Outside input'}));

        await waitFor(() => expect(screen.queryByRole('button', {name: 'URL übernehmen'})).not.toBeInTheDocument());
        expect(screen.getByRole('textbox', {name: 'Outside input'})).toHaveFocus();
        expect(onChange.mock.calls.every(([markdown]) => !markdown.includes('https://example.org'))).toBe(true);
    });

    it('keeps the link form open while tabbing between fields and saves the selected text', async () => {
        const user = userEvent.setup();
        const {onChange} = renderEditor();
        const url = await openNewLink(user);
        await user.type(url, 'https://example.org');
        await user.tab();
        expect(screen.getByRole('textbox', {name: 'Linktitel'})).toHaveFocus();
        await user.keyboard('Example');
        await user.click(screen.getByRole('button', {name: 'URL übernehmen'}));

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith('[Linktext](https://example.org "Example")'));
        expect(screen.getByRole('link', {name: 'Linktext'})).toHaveAttribute('href', 'https://example.org');
    });

    it('closes a new link form with Cancel without changing the text', async () => {
        const user = userEvent.setup();
        const {onChange} = renderEditor();
        await user.type(await openNewLink(user), 'https://example.org');
        await user.click(screen.getByRole('button', {name: 'Änderung verwerfen'}));

        await waitFor(() => expect(screen.queryByRole('button', {name: 'URL übernehmen'})).not.toBeInTheDocument());
        expect(screen.queryByRole('link', {name: 'Linktext'})).not.toBeInTheDocument();
        expect(onChange.mock.calls.every(([markdown]) => !markdown.includes('https://example.org'))).toBe(true);
    });

    it('follows nested page scrolling without discarding an unsaved link draft', async () => {
        const user = userEvent.setup();
        renderEditor();
        const editor = await screen.findByRole('textbox', {name: 'Bearbeitungshinweis – optional'});
        let editorTop = 250;
        vi.spyOn(editor, 'getBoundingClientRect').mockImplementation(() => new DOMRect(430, editorTop, 600, 180));
        const url = await openNewLink(user);
        await user.type(url, 'https://example.org');
        const scrollContainer = screen.getByRole('region', {name: 'Scrollable editor'});
        const anchor = scrollContainer.querySelector<HTMLElement>('[class*="_linkDialogAnchor_"]')!;
        const initialTop = parseFloat(anchor.style.top);
        editorTop -= 80;
        fireEvent.scroll(scrollContainer);

        await waitFor(() => expect(parseFloat(anchor.style.top)).toBe(initialTop - 80));
        editor.scrollTop = 40;
        fireEvent.scroll(editor);
        await waitFor(() => expect(parseFloat(anchor.style.top)).toBe(initialTop - 120));
        expect(url).toHaveValue('https://example.org');
        expect(url).toHaveFocus();
        expect(screen.getByRole('button', {name: 'URL übernehmen'})).toBeInTheDocument();
    });

    it('saves an existing link and discards a subsequent draft when tabbing out', async () => {
        const user = userEvent.setup();
        const {onChange} = renderEditor('[Linktext](https://example.org)');
        const editor = await screen.findByRole('textbox', {name: 'Bearbeitungshinweis – optional'});
        await selectEditorText(editor, 'Linktext', true);
        await user.click(await screen.findByRole('button', {name: 'Link-URL bearbeiten'}));
        const url = await screen.findByRole('textbox', {name: ''});
        await user.clear(url);
        await user.type(url, 'https://changed.example.org');
        await user.click(screen.getByRole('button', {name: 'URL übernehmen'}));
        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith('[Linktext](https://changed.example.org)'));

        await user.click(await screen.findByRole('button', {name: 'Link-URL bearbeiten'}));
        const editedUrl = await screen.findByRole('textbox', {name: ''});
        await user.clear(editedUrl);
        await user.type(editedUrl, 'https://discarded.example.org');
        const cancel = screen.getByRole('button', {name: 'Änderung verwerfen'});
        act(() => cancel.focus());
        await user.tab();

        await waitFor(() => expect(screen.queryByRole('button', {name: 'URL übernehmen'})).not.toBeInTheDocument());
        expect(screen.queryByRole('button', {name: 'Link-URL bearbeiten'})).not.toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Outside input'})).toHaveFocus();
        expect(screen.getByRole('link', {name: 'Linktext'})).toHaveAttribute('href', 'https://changed.example.org');
        expect(onChange.mock.calls.every(([markdown]) => !markdown.includes('https://discarded.example.org'))).toBe(true);
    });

    it('closes a link preview on a non-focusable outside click and keeps it closed on scroll', async () => {
        const user = userEvent.setup();
        renderEditor('[Linktext](https://example.org)');
        const editor = await screen.findByRole('textbox', {name: 'Bearbeitungshinweis – optional'});
        await selectEditorText(editor, 'Linktext', true);
        expect(await screen.findByRole('button', {name: 'Link-URL bearbeiten'})).toBeInTheDocument();
        await user.click(screen.getByText('Outside text'));

        await waitFor(() => expect(screen.queryByRole('button', {name: 'Link-URL bearbeiten'})).not.toBeInTheDocument());
        fireEvent.scroll(window);
        expect(screen.queryByRole('button', {name: 'Link-URL bearbeiten'})).not.toBeInTheDocument();
    });

    it('closes a preview when focus moves to another editor', async () => {
        render(<>
            <RichTextInputComponent label="First editor" value="[Linktext](https://example.org)" onChange={vi.fn()} reducedMode />
            <RichTextInputComponent label="Second editor" value="Other text" onChange={vi.fn()} reducedMode />
        </>);
        const editor = await screen.findByRole('textbox', {name: 'First editor – optional'});
        await selectEditorText(editor, 'Linktext', true);
        expect(await screen.findByRole('button', {name: 'Link-URL bearbeiten'})).toBeInTheDocument();
        const otherEditor = screen.getByRole('textbox', {name: 'Second editor – optional'});
        act(() => otherEditor.focus());

        await waitFor(() => expect(screen.queryByRole('button', {name: 'Link-URL bearbeiten'})).not.toBeInTheDocument());
        expect(otherEditor).toHaveFocus();
    });

    it('allows link editing inside a modal and dismisses when focus leaves the editor', async () => {
        const user = userEvent.setup();
        render(
            <Dialog open>
                <DialogTitle>Editor dialog</DialogTitle>
                <DialogContent>
                    <RichTextInputComponent label="Bearbeitungshinweis" value="Linktext" onChange={vi.fn()} reducedMode />
                    <input aria-label="Other dialog input" />
                </DialogContent>
            </Dialog>,
        );
        const url = await openNewLink(user);
        await user.type(url, 'https://example.org');
        await user.click(screen.getByRole('textbox', {name: 'Linktitel'}));
        expect(screen.getByRole('button', {name: 'URL übernehmen'})).toBeInTheDocument();
        await user.click(screen.getByRole('textbox', {name: 'Other dialog input'}));

        await waitFor(() => expect(screen.queryByRole('button', {name: 'URL übernehmen'})).not.toBeInTheDocument());
        expect(screen.getByRole('dialog', {name: 'Editor dialog'})).toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Other dialog input'})).toHaveFocus();
    });
});
