import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

/** An in-memory IndexedDB with the few operations the storage uses, shared by all simulated tabs. */
function createIndexedDb() {
    const records = new Map<string, unknown>();
    let keyPath: string[] = [];
    const request = <R>(result: () => R) => {
        const pending = {result: undefined as R | undefined, error: null, onsuccess: null as (() => void) | null, onerror: null};
        setTimeout(() => {
            pending.result = result();
            pending.onsuccess?.();
        });
        return pending;
    };
    const keyOf = (value: unknown) => JSON.stringify(Array.isArray(value) ? value : keyPath.map((field) => (value as Record<string, unknown>)[field]));
    const store = {
        getAll: () => request(() => [...records.values()]),
        put: (value: unknown) => request(() => void records.set(keyOf(value), structuredClone(value))),
        delete: (key: unknown) => request(() => void records.delete(keyOf(key))),
        clear: () => request(() => records.clear()),
    };
    const db = {
        objectStoreNames: {contains: () => keyPath.length > 0},
        deleteObjectStore: () => records.clear(),
        createObjectStore: (_name: string, options: {keyPath: string[]}) => {
            keyPath = options.keyPath;
        },
        transaction: () => ({objectStore: () => store}),
    };

    return {
        open: () => {
            const opening = {result: db, error: null, onupgradeneeded: null as (() => void) | null, onsuccess: null as (() => void) | null, onerror: null};
            setTimeout(() => {
                // Like on the first open in a browser, the store is created before the database is handed out.
                if (keyPath.length === 0) {
                    opening.onupgradeneeded?.();
                }
                opening.onsuccess?.();
            });
            return opening;
        },
    };
}

/** Holds a lock per open tab like navigator.locks; a tab is closed by releasing its lock. */
function createLockManager() {
    const held: string[] = [];
    return {
        held,
        request: (name: string) => {
            held.push(name);
            return new Promise(() => undefined);
        },
        query: async () => ({held: held.map((name) => ({name}))}),
    };
}

/** Each import is a new page with its own tab ID, like a second tab. */
async function openTab() {
    vi.resetModules();
    return (await import('./editor-draft-storage')).EditorDraftStorage;
}

const draft = (mine: string) => ({userId: 'user-1', key: 'process-node/1/configuration', base: 'Alt', baseLockVersion: 1, mine, savedAt: Date.now()});

describe('EditorDraftStorage', () => {
    let locks: ReturnType<typeof createLockManager>;

    beforeEach(() => {
        locks = createLockManager();
        vi.stubGlobal('indexedDB', createIndexedDb());
        vi.stubGlobal('navigator', {locks});
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('keeps the drafts of two open tabs editing the same object apart', async () => {
        const first = await openTab();
        const second = await openTab();

        await first.save(draft('Erster Tab'));
        await second.save(draft('Zweiter Tab'));
        await second.remove('user-1', 'process-node/1/configuration');

        expect((await first.load<string>('user-1', 'process-node/1/configuration'))?.mine).toEqual('Erster Tab');
        expect(await second.load('user-1', 'process-node/1/configuration')).toBeNull();
    });

    it('takes over the draft of a closed tab, e.g. after the session expired and the page was reloaded', async () => {
        const closed = await openTab();
        await closed.save(draft('Ungespeichert'));
        locks.held.splice(0, 1);

        const reloaded = await openTab();
        const recovered = await reloaded.load<string>('user-1', 'process-node/1/configuration');

        expect(recovered?.mine).toEqual('Ungespeichert');
        // The draft now belongs to the new tab, so saving there removes it and it is not offered again.
        await reloaded.remove('user-1', 'process-node/1/configuration');
        expect(await (await openTab()).load('user-1', 'process-node/1/configuration')).toBeNull();
    });
});
