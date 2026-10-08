import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {BaseApiService} from '../../../services/base-api-service';
import {ProcessExecutionSummary} from './process-execution-summary';

const {dispatch} = vi.hoisted(() => ({dispatch: vi.fn()}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => dispatch}));

const attachmentMarkdown = '[Bescheid.pdf](/api/process-instance-attachments/original-attachment/file/?download=false)';

describe('ProcessExecutionSummary', () => {
    beforeEach(() => {
        dispatch.mockClear();
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.unstubAllGlobals();
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
            payload: expect.objectContaining({message: 'Der Anhang konnte nicht angezeigt werden.'}),
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
