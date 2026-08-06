import {useCallback, useEffect, useMemo, useRef} from 'react';

const CHANNEL_NAME = 'gover_cross_tab_invalidation';
const MAX_SEEN_MESSAGE_IDS = 1000;

type CrossTabInvalidationScope = string | number | null | undefined;

interface CrossTabInvalidationMessage {
    type: 'invalidate';
    key: string;
    scope: string | null;
    sourceTabId: string;
    messageId: string;
    timestamp: number;
}

export interface BroadcastCrossTabInvalidationOptions {
    key: string;
    scope?: CrossTabInvalidationScope;
}

export interface UseCrossTabInvalidationOptions {
    key: string;
    scope?: CrossTabInvalidationScope;
    enabled?: boolean;
    deferWhileHidden?: boolean;
    onInvalidate: () => void | Promise<void>;
    onError?: (error: unknown) => void;
}

const tabId = createId();

export function broadcastCrossTabInvalidation(options: BroadcastCrossTabInvalidationOptions): void {
    if (typeof window === 'undefined' || typeof BroadcastChannel === 'undefined') {
        return;
    }

    const channel = new BroadcastChannel(CHANNEL_NAME);
    channel.postMessage({
        type: 'invalidate',
        key: options.key,
        scope: normalizeScope(options.scope),
        sourceTabId: tabId,
        messageId: createId(),
        timestamp: Date.now(),
    } satisfies CrossTabInvalidationMessage);
    channel.close();
}

export function useCrossTabInvalidation(options: UseCrossTabInvalidationOptions): void {
    const {
        key,
        enabled = true,
        deferWhileHidden = true,
        onInvalidate,
        onError,
    } = options;
    const scope = useMemo(() => normalizeScope(options.scope), [options.scope]);
    const seenMessageIdsRef = useRef<Set<string>>(new Set());
    const staleWhileHiddenRef = useRef(false);
    const invalidationInProgressRef = useRef(false);
    const invalidationQueuedRef = useRef(false);

    const runInvalidation = useCallback(() => {
        if (invalidationInProgressRef.current) {
            invalidationQueuedRef.current = true;
            return;
        }

        invalidationInProgressRef.current = true;
        void Promise
            .resolve(onInvalidate())
            .catch((error) => {
                onError?.(error);
            })
            .finally(() => {
                invalidationInProgressRef.current = false;

                if (invalidationQueuedRef.current) {
                    invalidationQueuedRef.current = false;
                    runInvalidation();
                }
            });
    }, [onError, onInvalidate]);

    const handleInvalidation = useCallback(() => {
        if (
            deferWhileHidden &&
            typeof document !== 'undefined' &&
            document.visibilityState === 'hidden'
        ) {
            staleWhileHiddenRef.current = true;
            return;
        }

        runInvalidation();
    }, [deferWhileHidden, runInvalidation]);

    useEffect(() => {
        seenMessageIdsRef.current.clear();
        staleWhileHiddenRef.current = false;
        invalidationQueuedRef.current = false;
    }, [enabled, key, scope]);

    useEffect(() => {
        if (!enabled || typeof window === 'undefined' || typeof BroadcastChannel === 'undefined') {
            return;
        }

        const channel = new BroadcastChannel(CHANNEL_NAME);

        channel.onmessage = (event: MessageEvent<unknown>) => {
            const message = event.data;
            if (!isCrossTabInvalidationMessage(message)) {
                return;
            }

            if (
                message.sourceTabId === tabId ||
                message.key !== key ||
                message.scope !== scope ||
                !rememberMessageId(seenMessageIdsRef.current, message.messageId)
            ) {
                return;
            }

            handleInvalidation();
        };

        return () => {
            channel.close();
        };
    }, [enabled, handleInvalidation, key, scope]);

    useEffect(() => {
        if (!enabled || !deferWhileHidden || typeof document === 'undefined') {
            return;
        }

        const handleVisibilityChange = () => {
            if (document.visibilityState !== 'visible' || !staleWhileHiddenRef.current) {
                return;
            }

            staleWhileHiddenRef.current = false;
            runInvalidation();
        };

        document.addEventListener('visibilitychange', handleVisibilityChange);

        return () => {
            document.removeEventListener('visibilitychange', handleVisibilityChange);
        };
    }, [deferWhileHidden, enabled, runInvalidation]);
}

function normalizeScope(scope: CrossTabInvalidationScope): string | null {
    return scope == null ? null : String(scope);
}

function rememberMessageId(seenMessageIds: Set<string>, messageId: string): boolean {
    if (seenMessageIds.has(messageId)) {
        return false;
    }

    seenMessageIds.add(messageId);
    if (seenMessageIds.size > MAX_SEEN_MESSAGE_IDS) {
        const oldestMessageId = seenMessageIds.values().next().value;
        if (oldestMessageId != null) {
            seenMessageIds.delete(oldestMessageId);
        }
    }

    return true;
}

function createId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }

    return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function isCrossTabInvalidationMessage(value: unknown): value is CrossTabInvalidationMessage {
    if (value == null || typeof value !== 'object') {
        return false;
    }

    const message = value as Record<string, unknown>;
    return (
        message.type === 'invalidate' &&
        typeof message.key === 'string' &&
        (message.scope === null || typeof message.scope === 'string') &&
        typeof message.sourceTabId === 'string' &&
        typeof message.messageId === 'string' &&
        typeof message.timestamp === 'number'
    );
}
