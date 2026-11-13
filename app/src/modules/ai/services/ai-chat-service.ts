import {BaseApiService} from "../../../services/base-api-service";
import {EventSourceParserStream} from 'eventsource-parser/stream';
import {type ElementType} from '../../../data/element-type/element-type';
import {type AnyElement} from '../../../models/elements/any-element';

export interface AiChatMessageData {
    targetRootType?: ElementType;
    currentState?: AnyElement;
    processId?: number;
    processVersion?: number;
    attachment?: File;
}

export interface AiChatAttachmentMetadata {
    name: string;
    size: number;
    contentType: string | null;
}

export interface AiChatMessage {
    role: 'user' | 'assistant';
    content: string;
    attachments?: AiChatAttachmentMetadata[];
}

export class AiChatService extends BaseApiService {
    public async startChatSession(signal?: AbortSignal): Promise<{sessionId: string}> {
        return await this.post('/api/ai/chat/start/', {}, {abort: signal});
    }

    public async getCurrentElement(sessionId: string, signal?: AbortSignal): Promise<AnyElement> {
        return await this.get<AnyElement>('/api/ai/chat/element/', {
            query: {chatSessionId: sessionId},
            abort: signal,
        });
    }

    public async getMessages(sessionId: string, signal?: AbortSignal): Promise<AiChatMessage[]> {
        return await this.get<AiChatMessage[]>('/api/ai/chat/messages/', {
            query: {chatSessionId: sessionId},
            abort: signal,
        });
    }

    public async downloadTrace(sessionId: string): Promise<Blob> {
        const response = await this.fetch('GET', '/api/ai/chat/trace/', undefined, {
            query: {chatSessionId: sessionId},
            headers: {Accept: 'application/json'},
        });
        return await response.blob();
    }

    public async sendMessage(
        sessionId: string,
        message: string,
        onStream: (chunk: string) => void,
        signal?: AbortSignal,
        data?: AiChatMessageData,
        onAccepted?: () => void,
    ): Promise<void> {
        const body = new FormData();
        body.append('chatSessionId', sessionId);
        body.append('userInput', message);

        if (data?.targetRootType != null) {
            body.append('targetRootType', new Blob([JSON.stringify(data.targetRootType)], {type: 'application/json'}));
        }
        if (data?.currentState != null) {
            body.append('currentState', new Blob([JSON.stringify(data.currentState)], {type: 'application/json'}));
        }
        if (data?.processId != null) {
            body.append('processId', String(data.processId));
        }
        if (data?.processVersion != null) {
            body.append('processVersion', String(data.processVersion));
        }
        if (data?.attachment != null) {
            body.append('attachments', data.attachment);
        }

        const response = await this.fetch('POST', '/api/ai/chat/send/', body, {
            // The backend bounds model requests; avoid the shared API's shorter browser deadline.
            abort: signal ?? new AbortController().signal,
            headers: {
                Accept: 'text/event-stream',
                'Content-Type': null,
            },
        });

        const contentType = response
            .headers
            .get('Content-Type')
            ?.split(';')[0]
            .trim()
            .toLowerCase();

        if (contentType !== 'text/event-stream' || response.body == null) {
            throw new Error('Expected an SSE response with a body');
        }
        onAccepted?.();

        const reader = response
            .body
            .pipeThrough(new TextDecoderStream())
            .pipeThrough(new EventSourceParserStream())
            .getReader();

        try {
            while (true) {
                const {value, done} = await reader.read();

                if (done) {
                    break;
                }

                onStream(value.data);
            }
        } finally {
            try {
                await reader.cancel();
            } finally {
                reader.releaseLock();
            }
        }
    }
}
