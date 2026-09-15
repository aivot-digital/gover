import {useCallback, useEffect, useRef, useState} from 'react';

const CHANNEL_NAME = 'duplicate_page_warning';
const HEARTBEAT_INTERVAL_MS = 3000;
const STALE_TAB_TIMEOUT_MS = HEARTBEAT_INTERVAL_MS * 3;

type DuplicatePageMessageType = 'query' | 'presence' | 'leave';

interface DuplicatePageMessage {
    type: DuplicatePageMessageType;
    tabId: string;
    pageKey: string;
    timestamp: number;
}

export function useDuplicatePageWarning(pageKey: string | null): boolean {
    const enabled = pageKey != null;
    const tabIdRef = useRef<string>(createTabId());
    const duplicateTabIdsRef = useRef<Map<string, number>>(new Map());
    const [hasDuplicatePageOpen, setHasDuplicatePageOpen] = useState(false);

    const syncDuplicateState = useCallback(() => {
        setHasDuplicatePageOpen(duplicateTabIdsRef.current.size > 0);
    }, []);

    useEffect(() => {
        duplicateTabIdsRef.current.clear();
        syncDuplicateState();

        if (!enabled || pageKey == null) {
            return;
        }

        if (typeof window === 'undefined' || typeof BroadcastChannel === 'undefined') {
            return;
        }

        const channel = new BroadcastChannel(CHANNEL_NAME);

        const publish = (type: DuplicatePageMessageType, targetPageKey = pageKey) => {
            channel.postMessage({
                type,
                tabId: tabIdRef.current,
                pageKey: targetPageKey,
                timestamp: Date.now(),
            } satisfies DuplicatePageMessage);
        };

        const removeDuplicateTab = (tabId: string) => {
            const deleted = duplicateTabIdsRef.current.delete(tabId);
            if (deleted) {
                syncDuplicateState();
            }
        };

        const registerDuplicateTab = (tabId: string, timestamp: number) => {
            duplicateTabIdsRef.current.set(tabId, timestamp);
            syncDuplicateState();
        };

        channel.onmessage = (event: MessageEvent<DuplicatePageMessage>) => {
            const message = event.data;
            if (!isDuplicatePageMessage(message) || message.tabId === tabIdRef.current) {
                return;
            }

            if (message.type === 'leave') {
                removeDuplicateTab(message.tabId);
                return;
            }

            if (message.pageKey !== pageKey) {
                removeDuplicateTab(message.tabId);
                return;
            }

            registerDuplicateTab(message.tabId, message.timestamp);

            if (message.type === 'query') {
                publish('presence');
            }
        };

        const heartbeatInterval = window.setInterval(() => {
            publish('presence');
        }, HEARTBEAT_INTERVAL_MS);

        const staleTabSweepInterval = window.setInterval(() => {
            const staleBefore = Date.now() - STALE_TAB_TIMEOUT_MS;
            let changed = false;

            duplicateTabIdsRef.current.forEach((lastSeen, tabId) => {
                if (lastSeen < staleBefore) {
                    duplicateTabIdsRef.current.delete(tabId);
                    changed = true;
                }
            });

            if (changed) {
                syncDuplicateState();
            }
        }, HEARTBEAT_INTERVAL_MS);

        const handleBeforeUnload = () => {
            publish('leave');
        };

        window.addEventListener('beforeunload', handleBeforeUnload);

        publish('query');
        publish('presence');

        return () => {
            publish('leave');
            window.removeEventListener('beforeunload', handleBeforeUnload);
            window.clearInterval(heartbeatInterval);
            window.clearInterval(staleTabSweepInterval);
            channel.close();
        };
    }, [enabled, pageKey, syncDuplicateState]);

    return enabled && hasDuplicatePageOpen;
}

function createTabId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }

    return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function isDuplicatePageMessage(value: unknown): value is DuplicatePageMessage {
    if (value == null || typeof value !== 'object') {
        return false;
    }

    const message = value as Record<string, unknown>;
    return (
        (message.type === 'query' || message.type === 'presence' || message.type === 'leave') &&
        typeof message.tabId === 'string' &&
        typeof message.pageKey === 'string' &&
        typeof message.timestamp === 'number'
    );
}
