import {act, fireEvent, screen, waitFor} from '@testing-library/react';
import {useEffect} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AiChatWindow} from './ai-chat-window';
import {AiChatService} from '../../services/ai-chat-service';
import {useProcessAiChat, type ProcessChatEditor} from '../../../process/pages/details/hooks/use-process-ai-chat';
import {renderAiChatTestUi as render} from './ai-chat-test-utils';

const mocks = vi.hoisted(() => ({download: vi.fn()}));
vi.mock('../../../../utils/download-utils', () => ({downloadBlobFile: mocks.download}));

function deferred<T = void>() {
    let resolve!: (value: T) => void;
    let reject!: (error: Error) => void;
    const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; });
    return {promise, resolve, reject};
}

describe('process chat lifecycle', () => {
    const save = vi.fn<() => Promise<void>>();
    const refresh = vi.fn<() => Promise<void>>();
    const reload = vi.fn<(editor: ProcessChatEditor | null, isCurrent: () => boolean) => Promise<void>>();
    const editor = {nodeId: 7, save, refresh};

    beforeEach(() => {
        localStorage.clear();
        save.mockReset().mockResolvedValue();
        refresh.mockReset().mockResolvedValue();
        reload.mockReset().mockImplementation(async selected => { await selected?.refresh(); });
        vi.spyOn(AiChatService.prototype, 'startChatSession').mockResolvedValue({sessionId: 'process-session'});
        vi.spyOn(AiChatService.prototype, 'sendMessage').mockImplementation(
            async (_id, _text, chunk, _signal, _data, onAccepted) => {
                onAccepted?.();
                chunk('Prozess geändert.');
            },
        );
        vi.spyOn(AiChatService.prototype, 'getMessages').mockResolvedValue([]);
        vi.spyOn(AiChatService.prototype, 'getCurrentElement').mockResolvedValue({} as never);
        vi.spyOn(AiChatService.prototype, 'downloadTrace').mockResolvedValue(new Blob(['{}']));
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    function Editor({userId = 'user', processId = 42, version = 2}: {userId?: string; processId?: number; version?: number}) {
        const chat = useProcessAiChat(`${userId}:${processId}:${version}`, reload);
        useEffect(() => chat.registerEditor(editor), [chat.registerEditor]);
        return <>
            <button disabled={chat.locked}>Manuell bearbeiten</button>
            <AiChatWindow mode="process" userId={userId} processId={processId} processVersion={version}
                          onThinking={chat.onThinking} beforeSend={chat.beforeSend} afterTurn={chat.afterTurn}
                          disabled={chat.reloadFailed || chat.busy}
                          reloadFailed={chat.reloadFailed}
                          onRetry={() => void chat.retry()}
                          isRetrying={chat.busy}
                          unavailable={false}/>
        </>;
    }

    function send() {
        fireEvent.change(screen.getByRole('textbox', {name: 'Nachricht'}), {target: {value: 'Prozess modellieren'}});
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));
    }

    it('awaits node saving, prevents duplicates and keeps editing locked until node refresh completes', async () => {
        const saving = deferred();
        const reloading = deferred();
        save.mockReturnValueOnce(saving.promise);
        refresh.mockReturnValueOnce(reloading.promise);
        render(<Editor/>);
        send();
        expect(screen.queryByRole('button', {name: 'Absenden'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Anfrage abbrechen'})).toBeEnabled();
        expect(save).toHaveBeenCalledTimes(1);
        expect(AiChatService.prototype.sendMessage).not.toHaveBeenCalled();
        expect(screen.getByRole('button', {name: 'Manuell bearbeiten'})).toBeDisabled();
        await act(async () => saving.resolve());
        await waitFor(() => expect(refresh).toHaveBeenCalledTimes(1));
        expect(screen.getByRole('button', {name: 'Manuell bearbeiten'})).toBeDisabled();
        expect(AiChatService.prototype.sendMessage).toHaveBeenCalledExactlyOnceWith(
            'process-session', 'Prozess modellieren', expect.any(Function), expect.any(AbortSignal),
            {processId: 42, processVersion: 2, attachment: undefined}, expect.any(Function),
        );
        expect(AiChatService.prototype.getCurrentElement).not.toHaveBeenCalled();
        await act(async () => reloading.resolve());
        await waitFor(() => expect(screen.getByRole('button', {name: 'Manuell bearbeiten'})).toBeEnabled());
        expect(localStorage.getItem('aiChatSessionId:process:user:42:2')).toBe('process-session');
        expect(localStorage.getItem('aiChatSessionId')).toBeNull();
    });

    it('renders the shared chat window for process editing', () => {
        render(<Editor/>);

        expect(screen.getByRole('heading', {name: 'KI-Chat'})).toBeVisible();
        expect(screen.getByText('Änderungen durch die KI werden direkt gespeichert.')).toBeVisible();
        expect(screen.queryByText('Änderungen durch die KI werden in den Formularentwurf übernommen.'))
            .not.toBeInTheDocument();
    });

    it('keeps the unsent message on save failure and does not start a session or tool turn', async () => {
        save.mockRejectedValueOnce(new Error('Invalid configuration'));
        render(<Editor/>);
        send();
        expect(await screen.findByText(/Die offenen Änderungen konnten nicht gespeichert werden/)).toBeVisible();
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toHaveValue('Prozess modellieren');
        expect(AiChatService.prototype.startChatSession).not.toHaveBeenCalled();
        expect(AiChatService.prototype.sendMessage).not.toHaveBeenCalled();
        expect(reload).not.toHaveBeenCalled();
        expect(screen.getByRole('button', {name: 'Manuell bearbeiten'})).toBeEnabled();
    });

    it('cancels while saving without starting a tool turn or reloading the process', async () => {
        const saving = deferred();
        save.mockReturnValueOnce(saving.promise);
        render(<Editor/>);
        send();

        fireEvent.click(screen.getByRole('button', {name: 'Anfrage abbrechen'}));
        expect(screen.getByText('Abbruch wird abgeschlossen …')).toBeVisible();
        await act(async () => saving.resolve());

        expect(await screen.findByText(
            'Die Anfrage wurde abgebrochen. Bereits ausgeführte Änderungen können erhalten bleiben.',
        )).toBeVisible();
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toHaveValue('Prozess modellieren');
        expect(AiChatService.prototype.startChatSession).not.toHaveBeenCalled();
        expect(AiChatService.prototype.sendMessage).not.toHaveBeenCalled();
        expect(reload).not.toHaveBeenCalled();
        expect(screen.getByRole('button', {name: 'Manuell bearbeiten'})).toBeEnabled();
    });

    it.each(['error', 'empty'])('reloads committed changes after an %s response', async outcome => {
        if (outcome === 'error') vi.mocked(AiChatService.prototype.sendMessage).mockRejectedValueOnce(new Error('Stream failed'));
        else vi.mocked(AiChatService.prototype.sendMessage).mockResolvedValueOnce();
        render(<Editor/>);
        send();
        expect(await screen.findByText(/Bereits ausgeführte Änderungen bleiben gespeichert/)).toBeVisible();
        await waitFor(() => expect(refresh).toHaveBeenCalledTimes(1));
        expect(screen.getByRole('button', {name: 'Manuell bearbeiten'})).toBeEnabled();
    });

    it('cancels an active turn and keeps editing locked until committed changes are reloaded', async () => {
        const reloading = deferred();
        refresh.mockReturnValueOnce(reloading.promise);
        vi.mocked(AiChatService.prototype.sendMessage).mockImplementationOnce(
            async (_id, _text, chunk, signal, _data, onAccepted) => {
                onAccepted?.();
                chunk('Teilweise geändert.');
                await new Promise<void>((_resolve, reject) => signal?.addEventListener(
                    'abort', () => reject(signal.reason), {once: true},
                ));
            },
        );
        render(<Editor/>);
        send();
        expect(await screen.findByText('Teilweise geändert.')).toBeVisible();

        fireEvent.click(screen.getByRole('button', {name: 'Anfrage abbrechen'}));

        expect(await screen.findByText('Abbruch wird abgeschlossen …')).toBeVisible();
        await waitFor(() => expect(refresh).toHaveBeenCalledTimes(1));
        expect(screen.getByRole('button', {name: 'Manuell bearbeiten'})).toBeDisabled();
        expect(screen.queryByText(/Die Anfrage wurde abgebrochen/)).not.toBeInTheDocument();
        await act(async () => reloading.resolve());
        expect(await screen.findByText(
            'Die Anfrage wurde abgebrochen. Bereits ausgeführte Änderungen können erhalten bleiben.',
        )).toBeVisible();
        expect(screen.getByRole('button', {name: 'Manuell bearbeiten'})).toBeEnabled();
        expect(screen.getByText('Teilweise geändert.')).toBeVisible();
        expect(screen.queryByText(/Die KI-Anfrage wurde mit einem Fehler beendet/)).not.toBeInTheDocument();
    });

    it('blocks more edits and messages until a failed reload is successfully retried, without replaying tools', async () => {
        reload.mockRejectedValueOnce(new Error('Reload failed'));
        render(<Editor/>);
        send();
        const retry = await screen.findByRole('button', {name: 'Erneut laden'});
        await waitFor(() => expect(retry).toBeEnabled());
        expect(screen.getByRole('button', {name: 'Manuell bearbeiten'})).toBeDisabled();
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toBeDisabled();
        fireEvent.click(retry);
        await waitFor(() => expect(screen.getByRole('button', {name: 'Manuell bearbeiten'})).toBeEnabled());
        expect(reload).toHaveBeenCalledTimes(2);
        expect(AiChatService.prototype.sendMessage).toHaveBeenCalledTimes(1);
        expect(save).toHaveBeenCalledTimes(1);
    });

    it('restores history and downloads the trace from the process session', async () => {
        localStorage.setItem('aiChatSessionId', 'form-session');
        localStorage.setItem('aiChatSessionId:process:user:42:2', 'existing-process-session');
        vi.mocked(AiChatService.prototype.getMessages).mockResolvedValue([{role: 'user', content: 'Vorheriger Auftrag'}]);
        render(<Editor/>);
        expect(await screen.findByText('Vorheriger Auftrag')).toBeVisible();
        fireEvent.click(screen.getByRole('button', {name: 'KI-Diagnose herunterladen'}));
        await waitFor(() => expect(mocks.download).toHaveBeenCalledWith('prosuna-ai-chat-trace-existing-process-session.json', expect.any(Blob)));
        expect(AiChatService.prototype.getMessages).toHaveBeenCalledWith('existing-process-session', expect.any(AbortSignal));
    });

    it('drops an expired process session without clearing the form session', async () => {
        localStorage.setItem('aiChatSessionId', 'form-session');
        localStorage.setItem('aiChatSessionId:process:user:42:2', 'expired');
        vi.mocked(AiChatService.prototype.getMessages).mockRejectedValue({status: 404, message: 'Not found', details: null, displayableToUser: false});
        render(<Editor/>);
        await screen.findByText(/Der bisherige Chat ist nicht mehr verfügbar/);
        expect(localStorage.getItem('aiChatSessionId:process:user:42:2')).toBeNull();
        expect(localStorage.getItem('aiChatSessionId')).toBe('form-session');
        send();
        await waitFor(() => expect(refresh).toHaveBeenCalledTimes(1));
    });

    it.each([{userId: 'other'}, {processId: 43}, {version: 3}])('aborts and isolates old requests on context change %j', async next => {
        const response = deferred();
        let oldChunk!: (text: string) => void;
        let signal!: AbortSignal;
        vi.mocked(AiChatService.prototype.sendMessage).mockImplementationOnce(async (
            _id, _text, chunk, abort, _data, onAccepted,
        ) => {
            signal = abort!;
            oldChunk = chunk;
            onAccepted?.();
            await response.promise;
        });
        const view = render(<Editor/>);
        send();
        await waitFor(() => expect(AiChatService.prototype.sendMessage).toHaveBeenCalledTimes(1));
        view.rerender(<Editor {...next}/>);
        expect(signal.aborted).toBe(true);
        await act(async () => { oldChunk('Veraltete Antwort'); response.resolve(); });
        expect(screen.queryByText('Veraltete Antwort')).not.toBeInTheDocument();
        expect(screen.queryByText('Prozess modellieren')).not.toBeInTheDocument();
        expect(reload).not.toHaveBeenCalled();
        expect(screen.getByRole('button', {name: 'Manuell bearbeiten'})).toBeEnabled();
    });

    it('aborts a pending stream when leaving the editor', async () => {
        const response = deferred();
        vi.mocked(AiChatService.prototype.sendMessage).mockReturnValueOnce(response.promise);
        const view = render(<Editor/>);
        send();
        await waitFor(() => expect(AiChatService.prototype.sendMessage).toHaveBeenCalled());
        const signal = vi.mocked(AiChatService.prototype.sendMessage).mock.calls[0][3];
        view.unmount();
        expect(signal?.aborted).toBe(true);
        await act(async () => response.resolve());
        expect(reload).not.toHaveBeenCalled();
    });
});
