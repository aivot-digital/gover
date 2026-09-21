import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {AuthService} from '../../../services/auth-service';
import {ElementType} from '../../../data/element-type/element-type';
import {type AnyElement} from '../../../models/elements/any-element';
import {AiChatService, type AiChatMessageData} from './ai-chat-service';

function streamResponse(text = ''): Response {
    const bytes = new TextEncoder().encode(text);
    return new Response(new ReadableStream<Uint8Array>({
        start(controller) {
            // Split both SSE frames and multi-byte characters across network chunks.
            for (const byte of bytes) {
                controller.enqueue(new Uint8Array([byte]));
            }
            controller.close();
        },
    }), {headers: {'Content-Type': 'text/event-stream;charset=UTF-8'}});
}

function readBlob(blob: Blob): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = () => reject(reader.error);
        reader.readAsText(blob);
    });
}

describe('AiChatService.getCurrentElement', () => {
    it.each([undefined, new AbortController().signal])('retrieves the session element with optional cancellation (%j)', async (signal) => {
        const service = new AiChatService();
        const element = {type: ElementType.Text, id: 'field', name: 'Field'} as AnyElement;
        const fetch = vi.spyOn(service, 'fetch').mockResolvedValue(Response.json(element));

        await expect(service.getCurrentElement('session', signal)).resolves.toEqual(element);

        expect(fetch).toHaveBeenCalledWith('GET', '/api/ai/chat/element/', undefined, {
            query: {chatSessionId: 'session'},
            abort: signal,
        });
    });

    it('propagates API errors without replacing a missing element with null', async () => {
        const service = new AiChatService();
        const failure = {status: 404, message: 'Element unavailable'};
        vi.spyOn(service, 'fetch').mockRejectedValue(failure);

        await expect(service.getCurrentElement('session')).rejects.toBe(failure);
    });
});

describe('AiChatService.getMessages', () => {
    it('retrieves persisted messages with optional cancellation', async () => {
        const service = new AiChatService();
        const messages = [
            {role: 'user' as const, content: 'Hallo'},
            {role: 'assistant' as const, content: 'Guten Tag'},
        ];
        const signal = new AbortController().signal;
        const fetch = vi.spyOn(service, 'fetch').mockResolvedValue(Response.json(messages));

        await expect(service.getMessages('session', signal)).resolves.toEqual(messages);

        expect(fetch).toHaveBeenCalledWith('GET', '/api/ai/chat/messages/', undefined, {
            query: {chatSessionId: 'session'},
            abort: signal,
        });
    });
});

describe('AiChatService.downloadTrace', () => {
    it('downloads the JSON trace for a session', async () => {
        const service = new AiChatService();
        const fetch = vi.spyOn(service, 'fetch').mockResolvedValue(new Response('{"schemaVersion":1}', {
            headers: {'Content-Type': 'application/json'},
        }));

        const trace = await service.downloadTrace('session');

        expect(fetch).toHaveBeenCalledWith('GET', '/api/ai/chat/trace/', undefined, {
            query: {chatSessionId: 'session'},
            headers: {Accept: 'application/json'},
        });
        await expect(readBlob(trace)).resolves.toBe('{"schemaVersion":1}');
    });
});

describe('AiChatService.sendMessage', () => {
    it.each([undefined, {}, {attachments: []}] satisfies (AiChatMessageData | undefined)[])(
        'sends only required fields when optional data is absent (%j)',
        async (data) => {
            const service = new AiChatService();
            const fetch = vi.spyOn(service, 'fetch').mockResolvedValue(streamResponse());

            await service.sendMessage('session', 'Hello', vi.fn(), undefined, data);

            const [method, path, body, options] = fetch.mock.calls[0];
            expect(method).toBe('POST');
            expect(path).toBe('/api/ai/chat/send/');
            expect(Array.from((body as FormData).entries())).toEqual([
                ['chatSessionId', 'session'],
                ['userInput', 'Hello'],
            ]);
            expect(options?.headers).toEqual({Accept: 'text/event-stream', 'Content-Type': null});
        },
    );

    it('transmits form JSON parts, multiple files and the abort signal', async () => {
        const service = new AiChatService();
        const fetch = vi.spyOn(service, 'fetch').mockResolvedValue(streamResponse());
        const signal = new AbortController().signal;
        const currentState = {type: ElementType.Text, id: 'field', name: 'Straße'} as AnyElement;
        const attachments = [
            new File(['First'], 'first.txt', {type: 'text/plain'}),
            new File(['Second'], 'second.txt', {type: 'text/plain'}),
        ];

        await service.sendMessage('session', 'Edit', vi.fn(), signal, {
            targetRootType: ElementType.FormLayout,
            currentState,
            attachments,
        });

        const body = fetch.mock.calls[0][2] as FormData;
        expect(Array.from(body.keys())).toEqual([
            'chatSessionId', 'userInput', 'targetRootType', 'currentState',
            'attachments', 'attachments',
        ]);
        const rootTypePart = body.get('targetRootType') as Blob;
        const statePart = body.get('currentState') as Blob;
        expect(rootTypePart.type).toBe('application/json');
        expect(JSON.parse(await readBlob(rootTypePart))).toBe(0);
        expect(statePart.type).toBe('application/json');
        expect(JSON.parse(await readBlob(statePart))).toEqual(currentState);
        expect(body.has('processId')).toBe(false);
        expect(body.has('processVersion')).toBe(false);
        expect(body.getAll('attachments')).toEqual(attachments);
        expect(await Promise.all((body.getAll('attachments') as File[]).map(readBlob))).toEqual(['First', 'Second']);
        expect(fetch.mock.calls[0][3]?.abort).toBe(signal);
    });

    it('sends process context without form JSON parts', async () => {
        const service = new AiChatService();
        const fetch = vi.spyOn(service, 'fetch').mockResolvedValue(streamResponse());
        await service.sendMessage('session', 'Prozess bearbeiten', vi.fn(), undefined, {
            processId: 42, processVersion: 3,
        });
        expect(Array.from((fetch.mock.calls[0][2] as FormData).entries())).toEqual([
            ['chatSessionId', 'session'], ['userInput', 'Prozess bearbeiten'],
            ['processId', '42'], ['processVersion', '3'],
        ]);
    });

    it('delivers complete SSE data while preserving Unicode, spaces and newlines', async () => {
        const service = new AiChatService();
        vi.spyOn(service, 'fetch').mockResolvedValue(streamResponse(
            ': heartbeat\r\n\r\ndata: Grüße 😀 \r\n\r\ndata: erste Zeile\ndata: zweite Zeile\n\n',
        ));
        const onStream = vi.fn();

        await service.sendMessage('session', 'Hello', onStream);

        expect(onStream.mock.calls).toEqual([['Grüße 😀 '], ['erste Zeile\nzweite Zeile']]);
    });

    it('propagates a stream failure to the caller', async () => {
        const service = new AiChatService();
        const failure = new Error('Connection interrupted');
        vi.spyOn(service, 'fetch').mockResolvedValue(new Response(new ReadableStream({
            start(controller) {
                controller.error(failure);
            },
        }), {headers: {'Content-Type': 'text/event-stream'}}));

        await expect(service.sendMessage('session', 'Hello', vi.fn())).rejects.toBe(failure);
    });
});

describe('AiChatService streaming through BaseApiService', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.spyOn(AuthService, 'isAccessTokenValid').mockReturnValue(true);
        vi.spyOn(AuthService, 'getCsrfToken').mockReturnValue('test-csrf');
        // Tie AbortSignal.timeout to the fake clock so the shared API deadline is exercised.
        vi.spyOn(AbortSignal, 'timeout').mockImplementation((milliseconds) => {
            const controller = new AbortController();
            setTimeout(() => controller.abort(new DOMException('Timed out', 'TimeoutError')), milliseconds);
            return controller.signal;
        });
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.unstubAllGlobals();
    });

    function mockStreamingFetch() {
        let streamController: ReadableStreamDefaultController<Uint8Array>;
        const fetch = vi.fn<typeof globalThis.fetch>().mockImplementation(async (_input, init) => {
            const stream = new ReadableStream<Uint8Array>({
                start(controller) {
                    streamController = controller;
                    init?.signal?.addEventListener('abort', () => controller.error(init.signal?.reason), {once: true});
                },
            });
            return new Response(stream, {headers: {'Content-Type': 'text/event-stream'}});
        });
        vi.stubGlobal('fetch', fetch);
        return {
            fetch,
            send: (text: string) => streamController.enqueue(new TextEncoder().encode(`data:${text}\n\n`)),
            close: () => streamController.close(),
        };
    }

    it('continues streaming beyond one minute without an explicit abort signal', async () => {
        const stream = mockStreamingFetch();
        const onStream = vi.fn();
        const sending = new AiChatService().sendMessage('session', 'Hello', onStream);
        // Attach a rejection handler before advancing the clock, including on regression failures.
        const completed = sending.then(() => null, error => error);

        await vi.advanceTimersByTimeAsync(0);
        stream.send('First');
        await vi.advanceTimersByTimeAsync(61_000);

        const signal = stream.fetch.mock.calls[0][1]?.signal;
        expect(signal).toBeDefined();
        expect(signal?.aborted).toBe(false);
        stream.send('Later');
        stream.close();

        expect(await completed).toBeNull();
        expect(onStream.mock.calls).toEqual([['First'], ['Later']]);
    });

    it('still cancels an active stream with the caller signal', async () => {
        const stream = mockStreamingFetch();
        const controller = new AbortController();
        const onStream = vi.fn();
        const sending = new AiChatService().sendMessage('session', 'Hello', onStream, controller.signal);
        const completed = sending.then(() => null, error => error);

        await vi.advanceTimersByTimeAsync(0);
        stream.send('First');
        await vi.advanceTimersByTimeAsync(0);
        controller.abort();

        expect(await completed).toBe(controller.signal.reason);
        expect(onStream).toHaveBeenCalledExactlyOnceWith('First');
        expect(stream.fetch.mock.calls[0][1]?.signal).toBe(controller.signal);
    });
});
