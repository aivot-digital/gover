import {act, fireEvent, screen, waitFor} from '@testing-library/react';
import {useState} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {AiChatWindow} from './ai-chat-window';
import {AiChatService, type AiChatMessage} from '../../services/ai-chat-service';
import {ElementType} from '../../../../data/element-type/element-type';
import {type AnyElement} from '../../../../models/elements/any-element';
import {renderAiChatTestUi as render} from './ai-chat-test-utils';

const mocks = vi.hoisted(() => ({
    downloadBlobFile: vi.fn(),
}));

vi.mock('../../../../utils/download-utils', () => ({
    downloadBlobFile: mocks.downloadBlobFile,
}));

describe('AiChatWindow', () => {
    const root = {id: 'root', type: ElementType.FormLayout, children: []} as unknown as AnyElement;
    const updated = {...root, name: 'Updated'} as AnyElement;
    const onElementChange = vi.fn();
    const onThinking = vi.fn();

    beforeEach(() => {
        localStorage.clear();
        vi.clearAllMocks();
        vi.spyOn(AiChatService.prototype, 'startChatSession').mockResolvedValue({sessionId: 'new-session'});
        vi.spyOn(AiChatService.prototype, 'sendMessage').mockImplementation(async (
            _id, _message, onStream, _signal, _data, onAccepted,
        ) => {
            onAccepted?.();
            onStream('Abschnitt ');
            onStream('erstellt.');
        });
        vi.spyOn(AiChatService.prototype, 'getCurrentElement').mockResolvedValue(updated);
        vi.spyOn(AiChatService.prototype, 'getMessages').mockResolvedValue([]);
        vi.spyOn(AiChatService.prototype, 'downloadTrace').mockResolvedValue(
            new Blob(['{"schemaVersion":1}'], {type: 'application/json'}),
        );
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
        localStorage.clear();
    });

    function mount() {
        render(<AiChatWindow rootElement={root} targetRootType={ElementType.FormLayout}
                             onElementChange={onElementChange} onThinking={onThinking}/>);
        fireEvent.change(screen.getByRole('textbox', {name: 'Nachricht'}), {target: {value: 'Neuen Abschnitt erstellen'}});
    }

    it('renders the shared chat window for form editing', () => {
        mount();

        expect(screen.getByRole('heading', {name: 'KI-Chat'})).toBeVisible();
        expect(screen.getByText('Änderungen durch die KI werden in den Formularentwurf übernommen.')).toBeVisible();
        expect(screen.queryByRole('button', {name: 'KI-Chat schließen'})).not.toBeInTheDocument();
    });

    it('uses the new session for the first message and updated element', async () => {
        mount();
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));

        await waitFor(() => expect(onElementChange).toHaveBeenCalledWith(updated));
        expect(AiChatService.prototype.sendMessage).toHaveBeenCalledWith('new-session', 'Neuen Abschnitt erstellen',
            expect.any(Function), expect.any(AbortSignal),
            {currentState: root, targetRootType: ElementType.FormLayout, attachment: undefined}, expect.any(Function));
        expect(AiChatService.prototype.getCurrentElement).toHaveBeenCalledWith('new-session');
        expect(localStorage.getItem('aiChatSessionId')).toBe('new-session');
        expect(screen.getByText('Abschnitt erstellt.')).toBeInTheDocument();
        await waitFor(() => expect(screen.getByRole('textbox', {name: 'Nachricht'})).toHaveValue(''));
        expect(onThinking).toHaveBeenLastCalledWith(false);
    });

    it('reuses a stored session', async () => {
        localStorage.setItem('aiChatSessionId', 'existing-session');
        mount();
        await waitFor(() => expect(screen.getByRole('textbox', {name: 'Nachricht'})).toBeEnabled());
        fireEvent.change(screen.getByRole('textbox', {name: 'Nachricht'}), {
            target: {value: 'Neuen Abschnitt erstellen'},
        });
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));
        await waitFor(() => expect(onElementChange).toHaveBeenCalled());
        expect(AiChatService.prototype.startChatSession).not.toHaveBeenCalled();
        expect(AiChatService.prototype.sendMessage).toHaveBeenCalledWith('existing-session', expect.any(String),
            expect.any(Function), expect.any(AbortSignal), expect.any(Object), expect.any(Function));
        expect(AiChatService.prototype.getCurrentElement).toHaveBeenCalledWith('existing-session');
    });

    it('restores persisted messages before enabling the composer', async () => {
        localStorage.setItem('aiChatSessionId', 'existing-session');
        let resolveHistory!: (messages: AiChatMessage[]) => void;
        vi.mocked(AiChatService.prototype.getMessages).mockReturnValue(new Promise(resolve => {
            resolveHistory = resolve;
        }));

        mount();

        expect(screen.getByText('Chatverlauf wird geladen …')).toBeVisible();
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toBeDisabled();
        await act(async () => resolveHistory([
            {role: 'user', content: 'Mein Hund heißt Bello.', attachments: [{
                name: 'formular.pdf', size: 1234, contentType: 'application/pdf',
            }]},
            {role: 'assistant', content: 'Ich habe den Namen übernommen.'},
        ]));

        expect(await screen.findByText('Mein Hund heißt Bello.')).toBeVisible();
        expect(screen.getByText('Ich habe den Namen übernommen.')).toBeVisible();
        expect(screen.getByText(/formular\.pdf/)).toBeVisible();
        expect(screen.queryByText('Chatverlauf wird geladen …')).not.toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toBeEnabled();
        expect(AiChatService.prototype.getMessages).toHaveBeenCalledWith(
            'existing-session', expect.any(AbortSignal),
        );
    });

    it('sends one supported file and clears it only after the request is accepted', async () => {
        let acceptRequest!: () => void;
        vi.mocked(AiChatService.prototype.sendMessage).mockImplementationOnce(
            async (_id, _text, _onStream, _signal, _data, onAccepted) => {
                await new Promise<void>(resolve => {
                    acceptRequest = () => {
                        onAccepted?.();
                        resolve();
                    };
                });
            },
        );
        mount();
        const file = new File(['Formularinhalt'], 'formular.pdf', {type: 'application/pdf'});
        const input = document.querySelector<HTMLInputElement>('input[type="file"]');
        expect(input).not.toBeNull();
        fireEvent.change(input!, {target: {files: [file]}});

        expect(screen.getByText('formular.pdf')).toBeVisible();
        expect(screen.getByRole('button', {name: 'Datei entfernen'})).toBeEnabled();
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));
        await waitFor(() => expect(AiChatService.prototype.sendMessage).toHaveBeenCalled());
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toHaveValue('Neuen Abschnitt erstellen');
        expect(screen.getByText('formular.pdf')).toBeVisible();
        expect(vi.mocked(AiChatService.prototype.sendMessage).mock.calls[0][4]?.attachment).toBe(file);

        await act(async () => acceptRequest());

        await waitFor(() => expect(screen.getByRole('textbox', {name: 'Nachricht'})).toHaveValue(''));
        expect(screen.queryByRole('button', {name: 'Datei entfernen'})).not.toBeInTheDocument();
        expect(screen.getByText(/formular\.pdf/)).toBeVisible();
    });

    it('rejects unsupported and oversized files before sending', () => {
        mount();
        const input = document.querySelector<HTMLInputElement>('input[type="file"]');
        fireEvent.change(input!, {target: {files: [new File(['image'], 'scan.png', {type: 'image/png'})]}});
        expect(screen.getByRole('alert')).toHaveTextContent('Dieser Dateityp wird nicht unterstützt.');
        expect(screen.queryByRole('button', {name: 'Datei entfernen'})).not.toBeInTheDocument();

        const oversized = new File(['x'], 'large.pdf', {type: 'application/pdf'});
        Object.defineProperty(oversized, 'size', {value: AppConfig.aiChatAttachments.maxFileSizeBytes + 1});
        fireEvent.change(input!, {target: {files: [oversized]}});
        expect(screen.getByRole('alert')).toHaveTextContent('Die Datei darf höchstens');
        expect(AiChatService.prototype.sendMessage).not.toHaveBeenCalled();
    });

    it('keeps the message and file when the backend rejects the upload', async () => {
        vi.mocked(AiChatService.prototype.sendMessage).mockRejectedValueOnce({
            status: 400,
            message: 'Aus der angehängten Datei konnte kein Text gelesen werden.',
            details: null,
            displayableToUser: true,
        });
        mount();
        const file = new File(['content'], 'scan.pdf', {type: 'application/pdf'});
        fireEvent.change(document.querySelector<HTMLInputElement>('input[type="file"]')!, {
            target: {files: [file]},
        });
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));

        expect(await screen.findByText('Aus der angehängten Datei konnte kein Text gelesen werden.')).toBeVisible();
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toHaveValue('Neuen Abschnitt erstellen');
        expect(screen.getByRole('button', {name: 'Datei entfernen'})).toBeVisible();
    });

    it('discards a stale session and starts a new one with the next message', async () => {
        localStorage.setItem('aiChatSessionId', 'missing-session');
        vi.mocked(AiChatService.prototype.getMessages).mockRejectedValue({
            status: 404,
            message: 'Not found',
            details: null,
            displayableToUser: false,
        });

        mount();

        expect(await screen.findByText(
            'Der bisherige Chat ist nicht mehr verfügbar. Mit Ihrer nächsten Nachricht wird ein neuer Chat gestartet.',
        )).toBeVisible();
        expect(localStorage.getItem('aiChatSessionId')).toBeNull();
        fireEvent.change(screen.getByRole('textbox', {name: 'Nachricht'}), {target: {value: 'Neu beginnen'}});
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));

        await waitFor(() => expect(AiChatService.prototype.sendMessage).toHaveBeenCalledWith(
            'new-session', 'Neu beginnen', expect.any(Function), expect.any(AbortSignal), expect.any(Object),
            expect.any(Function),
        ));
        expect(AiChatService.prototype.startChatSession).toHaveBeenCalledTimes(1);
    });

    it('keeps the stored session when loading its history fails temporarily', async () => {
        localStorage.setItem('aiChatSessionId', 'existing-session');
        vi.mocked(AiChatService.prototype.getMessages).mockRejectedValue(new Error('Network failure'));

        mount();

        expect(await screen.findByText(
            'Der Chatverlauf konnte nicht geladen werden. Öffnen Sie den Chat erneut, um es noch einmal zu versuchen.',
        )).toBeVisible();
        expect(localStorage.getItem('aiChatSessionId')).toBe('existing-session');
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toBeEnabled();
    });

    it('cancels history loading when the chat is closed', () => {
        localStorage.setItem('aiChatSessionId', 'existing-session');
        let signal: AbortSignal | undefined;
        vi.mocked(AiChatService.prototype.getMessages).mockImplementation((_sessionId, requestSignal) => {
            signal = requestSignal;
            return new Promise(() => {});
        });

        const view = render(<AiChatWindow rootElement={root} targetRootType={ElementType.FormLayout}
                                          onElementChange={onElementChange} onThinking={onThinking}/>);
        view.unmount();

        expect(signal?.aborted).toBe(true);
    });

    it('sends the updated draft with subsequent local edits on the next message', async () => {
        function Editor() {
            const [draft, setDraft] = useState(root);
            return <>
                <button onClick={() => setDraft({...draft, name: 'Lokale Änderung'})}>Lokal bearbeiten</button>
                <AiChatWindow rootElement={draft} targetRootType={ElementType.FormLayout}
                              onElementChange={setDraft} onThinking={onThinking}/>
                <output aria-label="Entwurf">{draft.name}</output>
            </>;
        }
        render(<Editor/>);
        fireEvent.change(screen.getByRole('textbox', {name: 'Nachricht'}), {target: {value: 'Ändern'}});
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));
        await waitFor(() => expect(screen.getByRole('status', {name: 'Entwurf'})).toHaveTextContent('Updated'));
        fireEvent.click(screen.getByRole('button', {name: 'Lokal bearbeiten'}));
        fireEvent.change(screen.getByRole('textbox', {name: 'Nachricht'}), {target: {value: 'Weiter ändern'}});
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));

        await waitFor(() => expect(AiChatService.prototype.sendMessage).toHaveBeenNthCalledWith(2,
            'new-session', 'Weiter ändern', expect.any(Function), expect.any(AbortSignal),
            {currentState: {...updated, name: 'Lokale Änderung'}, targetRootType: ElementType.FormLayout,
                attachment: undefined}, expect.any(Function)));
    });

    it('blocks duplicate sends while starting the session', async () => {
        let resolveSession!: (session: {sessionId: string}) => void;
        vi.mocked(AiChatService.prototype.startChatSession).mockReturnValue(new Promise(resolve => {
            resolveSession = resolve;
        }));
        mount();
        const send = screen.getByRole('button', {name: 'Absenden'});
        fireEvent.click(send);
        expect(screen.queryByRole('button', {name: 'Absenden'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Anfrage abbrechen'})).toBeEnabled();
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toBeDisabled();
        expect(AiChatService.prototype.startChatSession).toHaveBeenCalledTimes(1);
        expect(AiChatService.prototype.sendMessage).not.toHaveBeenCalled();
        await act(async () => resolveSession({sessionId: 'new-session'}));
        await waitFor(() => expect(onElementChange).toHaveBeenCalled());
        expect(AiChatService.prototype.sendMessage).toHaveBeenCalledTimes(1);
    });

    it('cancels session creation without consuming the message', async () => {
        vi.mocked(AiChatService.prototype.startChatSession).mockImplementationOnce(async signal => {
            await new Promise<void>((_resolve, reject) => signal?.addEventListener(
                'abort', () => reject(signal.reason), {once: true},
            ));
            return {sessionId: 'unreachable'};
        });
        mount();
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));

        const cancel = await screen.findByRole('button', {name: 'Anfrage abbrechen'});
        fireEvent.click(cancel);

        expect(await screen.findByText(
            'Die Anfrage wurde abgebrochen. Bereits ausgeführte Änderungen können erhalten bleiben.',
        )).toBeVisible();
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toHaveValue('Neuen Abschnitt erstellen');
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toBeEnabled();
        expect(AiChatService.prototype.startChatSession).toHaveBeenCalledWith(expect.any(AbortSignal));
        expect(AiChatService.prototype.sendMessage).not.toHaveBeenCalled();
        expect(AiChatService.prototype.getCurrentElement).not.toHaveBeenCalled();
    });

    it('cancels an active turn, keeps partial content and reuses the session', async () => {
        vi.mocked(AiChatService.prototype.sendMessage).mockImplementationOnce(
            async (_id, _text, onStream, signal, _data, onAccepted) => {
                onAccepted?.();
                onStream('Teilantwort');
                await new Promise<void>((_resolve, reject) => signal?.addEventListener(
                    'abort', () => reject(signal.reason), {once: true},
                ));
            },
        );
        mount();
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));
        expect(await screen.findByText('Teilantwort')).toBeVisible();

        fireEvent.click(screen.getByRole('button', {name: 'Anfrage abbrechen'}));

        expect(await screen.findByText(
            'Die Anfrage wurde abgebrochen. Bereits ausgeführte Änderungen können erhalten bleiben.',
        )).toBeVisible();
        expect(screen.getByText('Teilantwort')).toBeVisible();
        expect(onElementChange).toHaveBeenCalledWith(updated);
        expect(screen.queryByText(/Die KI-Anfrage wurde mit einem Fehler beendet/)).not.toBeInTheDocument();
        expect(screen.queryByText(/Die KI hat die Bearbeitung ohne Antwort beendet/)).not.toBeInTheDocument();

        fireEvent.change(screen.getByRole('textbox', {name: 'Nachricht'}), {target: {value: 'Weiterarbeiten'}});
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));
        await waitFor(() => expect(AiChatService.prototype.sendMessage).toHaveBeenCalledTimes(2));
        expect(AiChatService.prototype.startChatSession).toHaveBeenCalledTimes(1);
        expect(AiChatService.prototype.sendMessage).toHaveBeenLastCalledWith(
            'new-session', 'Weiterarbeiten', expect.any(Function), expect.any(AbortSignal), expect.any(Object),
            expect.any(Function),
        );
    });

    it('shows a start error and keeps the unsent message', async () => {
        vi.mocked(AiChatService.prototype.startChatSession).mockRejectedValueOnce(new Error('Failure'));
        mount();
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));

        expect(await screen.findByText('Die KI-Anfrage konnte nicht gestartet werden. Versuchen Sie es später erneut.')).toBeVisible();
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toBeEnabled();
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toHaveValue('Neuen Abschnitt erstellen');
        expect(onThinking).toHaveBeenLastCalledWith(false);
        expect(onElementChange).not.toHaveBeenCalled();
        expect(AiChatService.prototype.sendMessage).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));
        await waitFor(() => expect(onElementChange).toHaveBeenCalledWith(updated));
    });

    it('shows a stream error and still reloads changes made by tools', async () => {
        vi.mocked(AiChatService.prototype.sendMessage).mockRejectedValueOnce(new Error('Failure'));
        mount();
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));

        expect(await screen.findByText(
            'Die KI-Anfrage wurde mit einem Fehler beendet. Prüfen Sie den aktuellen Formularentwurf, bevor Sie die Anfrage erneut senden.',
        )).toBeVisible();
        await waitFor(() => expect(onElementChange).toHaveBeenCalledWith(updated));
        expect(AiChatService.prototype.getCurrentElement).toHaveBeenCalledWith('new-session');
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toBeEnabled();
        expect(onThinking).toHaveBeenLastCalledWith(false);
    });

    it('shows an error when the current draft cannot be reloaded', async () => {
        vi.mocked(AiChatService.prototype.getCurrentElement).mockRejectedValueOnce(new Error('Failure'));
        mount();
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));

        expect(await screen.findByText(
            'Der aktuelle Formularentwurf konnte nach der KI-Anfrage nicht geladen werden.',
        )).toBeVisible();
        expect(onElementChange).not.toHaveBeenCalled();
        expect(screen.getByRole('textbox', {name: 'Nachricht'})).toBeEnabled();
        expect(onThinking).toHaveBeenLastCalledWith(false);
    });

    it('makes a completed stream without assistant content visible and reloads the draft', async () => {
        vi.mocked(AiChatService.prototype.sendMessage).mockResolvedValueOnce();
        mount();
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));

        expect(await screen.findByText(
            'Die KI hat die Bearbeitung ohne Antwort beendet. Prüfen Sie den aktuellen Formularentwurf und laden Sie bei Bedarf die KI-Diagnose herunter.',
        )).toBeVisible();
        await waitFor(() => expect(onElementChange).toHaveBeenCalledWith(updated));
    });

    it('downloads the trace for the active session', async () => {
        localStorage.setItem('aiChatSessionId', 'existing-session');
        mount();

        const download = screen.getByRole('button', {name: 'KI-Diagnose herunterladen'});
        await waitFor(() => expect(download).toBeEnabled());
        fireEvent.click(download);

        await waitFor(() => expect(AiChatService.prototype.downloadTrace).toHaveBeenCalledWith('existing-session'));
        expect(mocks.downloadBlobFile).toHaveBeenCalledWith(
            'prosuna-ai-chat-trace-existing-session.json',
            expect.any(Blob),
        );
    });
});
