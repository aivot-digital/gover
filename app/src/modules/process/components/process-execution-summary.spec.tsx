import fs from 'fs';
import path from 'path';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {BaseApiService} from '../../../services/base-api-service';
import {ProcessExecutionSummary} from './process-execution-summary';

const {dispatch} = vi.hoisted(() => ({dispatch: vi.fn()}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => dispatch}));

const attachmentMarkdown = 'Dokument\n:   [Bescheid.pdf](/api/process-instance-attachments/original-attachment/file/?download=false)';

interface SharedSummaryCase {
    name: string;
    expectedMarkdown: string;
    terms: string[];
    contains: string[];
}
const sharedCases: SharedSummaryCase[] = JSON.parse(fs.readFileSync(path.resolve(__dirname,
    '../../../../../backend/src/test/resources/markdown/process-execution-summary-cases.json'), 'utf-8')).cases;

describe('ProcessExecutionSummary', () => {
    beforeEach(() => {
        dispatch.mockClear();
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.unstubAllGlobals();
    });

    it.each(sharedCases)('renders the shared backend format: $name', example => {
        render(<ProcessExecutionSummary markdown={example.expectedMarkdown}/>);
        expect(screen.queryAllByRole('term').map(term => term.textContent)).toEqual(example.terms);
        const valueText = screen.queryAllByRole('definition').map(value => value.textContent).join(' ');
        for (const text of example.contains) expect(valueText).toContain(text);
    });

    it('renders generated details as terms and definitions while preserving rich values', () => {
        render(<ProcessExecutionSummary markdown={
            'Die Aufgabe wurde abgeschlossen.\n\nVermerk\n:   Erster Absatz.\n    \n    - **Freigegeben**\n    - [Details](https://example.org)\n    \n    ```json\n    {"ok": true}\n    ```\n\nBetreff\n:   Wichtig\n\nAbschließender Hinweis.'
        }/>);
        expect(screen.getAllByRole('term').map(term => term.textContent)).toEqual(['Vermerk', 'Betreff']);
        const values = screen.getAllByRole('definition');
        const remark = within(values[0]);
        expect(remark.getByText('Erster Absatz.')).toBeVisible();
        expect(remark.getAllByRole('listitem')).toHaveLength(2);
        expect(remark.getByText('Freigegeben').tagName).toBe('STRONG');
        expect(remark.getByRole('link', {name: 'Details'})).toHaveAttribute('rel', 'noopener noreferrer');
        expect(remark.getByText('{"ok": true}').tagName).toBe('CODE');
        expect(values[1]).toHaveTextContent('Wichtig');
        expect(screen.getByText('Abschließender Hinweis.').closest('dd')).toBeNull();
        expect(screen.getByText('Die Aufgabe wurde abgeschlossen.').closest('dl')).toBeNull();
    });

    it('keeps existing summaries and authored bold paragraphs unchanged', () => {
        render(<ProcessExecutionSummary markdown={'Die Aufgabe wurde abgeschlossen.\n\n**Vermerk**\n\nDas war wichtig!'}/>);
        expect(screen.queryByRole('term')).not.toBeInTheDocument();
        expect(screen.getByText('Vermerk').tagName).toBe('STRONG');
        expect(screen.getByText('Das war wichtig!')).toBeVisible();
    });

    it.each([
        '```markdown\nVermerk\n:   Beispiel\n```',
        '~~~markdown\nVermerk\n:   Beispiel\n~~~',
        '    Vermerk\n    :   Beispiel',
        '> Vermerk\n> :   Beispiel',
        '- Vermerk\n  :   Beispiel',
    ])('does not interpret definition-list examples inside code, quotes or lists: %s', markdown => {
        render(<ProcessExecutionSummary markdown={markdown}/>);
        expect(screen.queryByRole('term')).not.toBeInTheDocument();
        expect(screen.getByText(/Beispiel/)).toBeVisible();
    });

    it('preserves indented code in a definition value', () => {
        render(<ProcessExecutionSummary markdown={'Beispiel\n:       code\n    \n        more code'}/>);
        const value = screen.getByRole('definition');
        expect(within(value).getByText(/more code/).tagName).toBe('CODE');
    });

    it('keeps HTML literal and unsafe links inactive inside definitions', () => {
        const {container} = render(<ProcessExecutionSummary markdown={
            'Vermerk\n:   <script>alert(1)</script>\n    \n    [Unsicher](javascript:alert(1))\n    \n    ![Bild](https://example.org/private.png)'
        }/>);
        const value = screen.getByRole('definition');
        expect(value).toHaveTextContent('<script>alert(1)</script>');
        expect(container.querySelector('script')).toBeNull();
        expect(container.querySelector('img')).toBeNull();
        expect(within(value).getByText('Unsicher')).not.toHaveAttribute('href', expect.stringContaining('javascript:'));
        expect(value).toHaveTextContent('Bild');
    });

    it('loads a document through the authenticated API and opens a temporary blob preview', async () => {
        const blob = new Blob(['PDF'], {type: 'application/pdf'});
        const getBlob = vi.spyOn(BaseApiService.prototype, 'getBlob').mockResolvedValue(blob);
        const replace = vi.fn();
        const preview = {opener: {}, document: {title: '', body: {textContent: ''}}, location: {replace}, close: vi.fn()};
        vi.spyOn(window, 'open').mockReturnValue(preview as unknown as Window);
        const createObjectURL = vi.fn(() => 'blob:preview');
        const revokeObjectURL = vi.fn();
        vi.stubGlobal('URL', class extends URL {
            static createObjectURL = createObjectURL;
            static revokeObjectURL = revokeObjectURL;
        });
        const originalTimeout = window.setTimeout.bind(window);
        let revoke: (() => void) | undefined;
        vi.spyOn(window, 'setTimeout').mockImplementation(((callback: () => void, delay?: number) => {
            if (delay === 60_000) {
                revoke = callback;
                return 0;
            }
            return originalTimeout(callback, delay);
        }) as typeof window.setTimeout);

        render(<ProcessExecutionSummary markdown={attachmentMarkdown}/>);
        fireEvent.click(screen.getByRole('link', {name: 'Bescheid.pdf'}));

        await waitFor(() => expect(replace).toHaveBeenCalledWith('blob:preview'));
        expect(getBlob).toHaveBeenCalledWith('/api/process-instance-attachments/original-attachment/file/?download=false');
        expect(createObjectURL).toHaveBeenCalledWith(blob);
        expect(preview.opener).toBeNull();
        expect(preview.document.title).toBe('Dokumentvorschau');
        expect(revoke).toBeDefined();
        revoke?.();
        expect(revokeObjectURL).toHaveBeenCalledWith('blob:preview');
    });

    it('explains blocked pop-ups without loading the document', () => {
        const getBlob = vi.spyOn(BaseApiService.prototype, 'getBlob');
        vi.spyOn(window, 'open').mockReturnValue(null);
        render(<ProcessExecutionSummary markdown={attachmentMarkdown}/>);
        fireEvent.click(screen.getByRole('link', {name: 'Bescheid.pdf'}));
        expect(getBlob).not.toHaveBeenCalled();
        expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: expect.stringContaining('Bitte erlauben Sie Pop-ups')}),
        }));
    });

    it('closes the preview and reports an API failure', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        vi.spyOn(BaseApiService.prototype, 'getBlob').mockRejectedValue(new Error('Denied'));
        const close = vi.fn();
        vi.spyOn(window, 'open').mockReturnValue({document: {body: {}}, close} as unknown as Window);
        render(<ProcessExecutionSummary markdown={attachmentMarkdown}/>);
        fireEvent.click(screen.getByRole('link', {name: 'Bescheid.pdf'}));
        await waitFor(() => expect(close).toHaveBeenCalled());
        expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: 'Die Anlage konnte nicht angezeigt werden.'}),
        }));
    });

    it('reports malformed attachment keys without throwing or requesting an invalid URL', async () => {
        const getBlob = vi.spyOn(BaseApiService.prototype, 'getBlob');
        const close = vi.fn();
        vi.spyOn(window, 'open').mockReturnValue({document: {body: {}}, close} as unknown as Window);
        render(<ProcessExecutionSummary markdown={'[Anlage](/api/process-instance-attachments/%FF/file/?download=false)'}/>);
        fireEvent.click(screen.getByRole('link', {name: 'Anlage'}));
        await waitFor(() => expect(close).toHaveBeenCalled());
        expect(getBlob).not.toHaveBeenCalled();
        expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: 'Die Anlage konnte nicht angezeigt werden.'}),
        }));
    });

    it('preserves task and user references and protects external links', () => {
        render(<ProcessExecutionSummary markdown={'[Aufgabe](/staff/tasks/1/2) [Person](/staff/users/ada) [Extern](https://example.org) [Unsicher](javascript:alert(1))'}/>);
        expect(screen.getByRole('link', {name: 'Aufgabe'})).toHaveAttribute('href', '/staff/tasks/1/2');
        expect(screen.getByRole('link', {name: 'Person'})).toHaveAttribute('href', '/staff/users/ada');
        expect(screen.getByRole('link', {name: 'Extern'})).toHaveAttribute('rel', 'noopener noreferrer');
        expect(screen.getByRole('link', {name: 'Extern'})).toHaveAttribute('target', '_blank');
        expect(screen.getByText('Unsicher')).not.toHaveAttribute('href', expect.stringContaining('javascript:'));
    });
});
