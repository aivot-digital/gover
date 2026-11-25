import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const logout = vi.hoisted(() => ({marker: null as string | null | undefined}));
vi.mock('./editor-draft-logout', () => ({
    readEditorDraftLogoutId: () => logout.marker,
    signalEditorDraftLogout: (id: string) => { logout.marker = id; return true; },
}));

// This double is for sequential eligibility decisions. Transaction ordering and rollback are tested in Chrome.
function createIndexedDb(initialDrafts: Record<string, unknown>[] = []) {
    const draftKey = (row: Record<string, unknown>) => JSON.stringify([row.userId, row.key, row.tabId]);
    const records = new Map(initialDrafts.map((row) => [draftKey(row), row]));
    const metadata = new Map<string, unknown>();
    let version = 2;
    const db = {
        objectStoreNames: {contains: (name: string) => name === 'drafts' || version >= 3},
        deleteObjectStore: () => records.clear(),
        createObjectStore: (name: string) => {
            expect(name).toBe('metadata');
            return {put: (value: unknown, key: IDBValidKey) => metadata.set(JSON.stringify(key), value)};
        },
        transaction: () => {
            const transaction = {
                oncomplete: null as (() => void) | null,
                objectStore: (name: string) => name === 'drafts' ? store : {
                    get: (key: IDBValidKey) => request(() => metadata.get(JSON.stringify(key))),
                    put: (value: unknown, key: IDBValidKey) => request(() => metadata.set(JSON.stringify(key), structuredClone(value))),
                },
            };
            let pendingRequests = 0;
            const request = <R>(read: () => R) => {
                const result = {result: undefined as R | undefined, onsuccess: null as (() => void) | null};
                pendingRequests++;
                queueMicrotask(() => {
                    result.result = read();
                    result.onsuccess?.();
                    if (--pendingRequests === 0) transaction.oncomplete?.();
                });
                return result;
            };
            const store = {
                getAll: (range?: {lower: string[]}) => request(() => [...records.values()].filter((row) =>
                    range == null || (row.userId === range.lower[0] && row.key === range.lower[1]))),
                get: (key: string[]) => request(() => structuredClone(records.get(JSON.stringify(key)))),
                put: (row: Record<string, unknown>) => request(() => {
                    records.set(draftKey(row), structuredClone(row));
                }),
                delete: (key: string[]) => request(() => records.delete(JSON.stringify(key))),
                clear: () => request(() => records.clear()),
            };
            return transaction;
        },
    };
    return {
        open: (_name: string, requestedVersion: number) => {
            const request = {result: db, onsuccess: null as (() => void) | null, onupgradeneeded: null as ((e: {oldVersion: number}) => void) | null};
            queueMicrotask(() => {
                if (version < requestedVersion) {
                    request.onupgradeneeded?.({oldVersion: version});
                    version = requestedVersion;
                }
                request.onsuccess?.();
            });
            return request;
        },
    };
}

function createLocks() {
    const held = new Set<string>();
    return {
        held,
        request: async (name: string, options: LockOptions, run: (lock: {name: string} | null) => unknown) => {
            expect(options.ifAvailable).toBe(true);
            if (held.has(name)) return run(null);
            held.add(name);
            try {
                return await run({name});
            } finally {
                held.delete(name);
            }
        },
    };
}

async function openTab() {
    vi.resetModules();
    return (await import('./editor-draft-storage')).EditorDraftStorage;
}

const user = 'user-1';
const key = 'process-node/1/configuration';
const draft = (mine: string) => ({userId: user, key, base: 'Alt', baseLockVersion: 1, mine, savedAt: Date.now()});

describe('EditorDraftStorage', () => {
    let locks: ReturnType<typeof createLocks>;
    beforeEach(() => {
        logout.marker = null;
        locks = createLocks();
        vi.stubGlobal('indexedDB', createIndexedDb());
        vi.stubGlobal('IDBKeyRange', {bound: (lower: string[], upper: unknown[]) => ({lower, upper})});
        vi.stubGlobal('navigator', {locks});
    });
    afterEach(() => vi.unstubAllGlobals());

    it('keeps open tabs independent when either one discards its own changes', async () => {
        const first = await openTab();
        const second = await openTab();
        await first.save(draft('Erster Tab'));
        expect(await second.load(user, key)).toBeNull();
        await second.save(draft('Zweiter Tab'));
        await second.remove(user, key);
        expect((await first.load(user, key))?.mine).toBe('Erster Tab');
        expect(await second.load(user, key)).toBeNull();
    });

    it('recovers a closed tab only once and only for the matching account and object', async () => {
        const old = await openTab();
        await old.save(draft('Ungespeichert'));
        locks.held.clear(); // The old document was unloaded.
        const first = await openTab();
        expect(await first.load('someone-else', key)).toBeNull();
        expect(await first.load(user, 'different-object')).toBeNull();
        expect((await first.load(user, key))?.mine).toBe('Ungespeichert');
        await first.remove(user, key);
        expect(await (await openTab()).load(user, key)).toBeNull();
    });

    it('protects a writer whose browser did not grant a lock, even from a healthy reader', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
        vi.stubGlobal('navigator', {locks: {request: () => Promise.reject(new Error('Lock denied'))}});
        const unprotected = await openTab();
        await unprotected.save(draft('Nur lokal'));
        expect((await unprotected.load(user, key))?.mine).toBe('Nur lokal');
        expect(await unprotected.reserve(user, key, 'return')).toBe(false);
        vi.stubGlobal('navigator', {locks});
        expect(await (await openTab()).load(user, key)).toBeNull();
    });

    it('only reads its own edits when Web Locks are unavailable', async () => {
        const other = await openTab();
        await other.save(draft('Anderer Tab'));
        locks.held.clear();
        vi.stubGlobal('navigator', {});
        const local = await openTab();
        expect(await local.load(user, key)).toBeNull();
        await local.save(draft('Eigene Änderung'));
        expect((await local.load(user, key))?.mine).toBe('Eigene Änderung');
        await local.remove(user, key);
        expect(await local.load(user, key)).toBeNull();
    });

    it('gives two overlapping loads in the same document the same adopted draft', async () => {
        const old = await openTab();
        await old.save(draft('Ungespeichert'));
        locks.held.clear();
        const fresh = await openTab();
        const [first, second] = await Promise.all([fresh.load(user, key), fresh.load(user, key)]);
        expect(first?.mine).toBe('Ungespeichert');
        expect(second).toEqual(first);
    });

    it('checks each return ID when two loads overlap in the same document', async () => {
        const old = await openTab();
        await old.save(draft('Zur Anmeldung'));
        await old.reserve(user, key, 'return');
        locks.held.clear();
        const fresh = await openTab();
        const [ordinary, returning] = await Promise.all([fresh.load(user, key), fresh.load(user, key, 'return')]);
        expect(ordinary).toBeNull();
        expect(returning?.mine).toBe('Zur Anmeldung');
    });

    it('clears reserved drafts of all accounts on explicit logout', async () => {
        const old = await openTab();
        await old.save(draft('Vor der Abmeldung'));
        await old.save({...draft('Weiteres Konto'), userId: 'other'});
        expect(await old.reserve(user, key, 'return')).toBe(true);
        await old.clear();
        locks.held.clear();
        const fresh = await openTab();
        expect(await fresh.load(user, key, 'return')).toBeNull();
        expect(await fresh.load('other', key)).toBeNull();
    });

    it('preserves an existing v2 draft with a reservation, pending save and open element', async () => {
        const row = {
            ...draft('Vor dem Update'), tabId: 'older-client', shareable: true,
            reservation: {id: 'return', expiresAt: Date.now() + 60_000},
            context: {base: 'Titel', mine: 'Neuer Titel'}, saveCheckpoint: {phase: 'pending', submitted: 'Gesendet'},
        };
        vi.stubGlobal('indexedDB', createIndexedDb([row]));
        const fresh = await openTab();
        expect(await fresh.load(user, key)).toBeNull();
        expect(await fresh.load(user, key, 'return')).toMatchObject({
            mine: row.mine, context: row.context, saveCheckpoint: row.saveCheckpoint,
        });
    });

    it('stops an old visit from writing, reading, reserving or deleting the next visit’s draft in the same document', async () => {
        const tab = await openTab();
        const old = await tab.openGeneration();
        await tab.save(draft('Vor der Abmeldung'), old);
        logout.marker = 'logout';
        await tab.clear('logout', true);
        const fresh = await tab.openGeneration();
        expect(fresh).not.toBe(old);
        expect(await tab.isCurrentGeneration(old)).toBe(false);
        expect(await tab.isCurrentGeneration(fresh)).toBe(true);
        await tab.save(draft('Nach der Anmeldung'), fresh);
        await tab.save(draft('Verspätete Sicherung'), old);
        expect(await tab.reserve(user, key, 'stale-return', old)).toBe(false);
        expect(await tab.load(user, key, undefined, old)).toBeNull();
        await tab.remove(user, key, old);
        expect((await tab.load(user, key, undefined, fresh))?.mine).toBe('Nach der Anmeldung');
    });

    it('finishes a missed cleanup from the persistent marker before returning or adopting any entries', async () => {
        const previous = await openTab();
        await previous.save(draft('Alter Entwurf'));
        await previous.reserve(user, key, 'return');
        await previous.save({...draft('Anderes Konto'), userId: 'other'});
        locks.held.clear();
        // The logout tab recorded its intent, but could not open the database.
        logout.marker = 'logout-1';
        const fresh = await openTab();
        expect(await fresh.load(user, key, 'return')).toBeNull();
        expect(await fresh.load('other', key)).toBeNull();
        await fresh.save(draft('Neue Sitzung'));
        await previous.clear('logout-1', true);
        expect((await fresh.load(user, key))?.mine).toBe('Neue Sitzung');

        logout.marker = 'logout-2';
        const latest = await openTab();
        await latest.save(draft('Weitere Sitzung'));
        await previous.clear('logout-1');
        expect((await latest.load(user, key))?.mine).toBe('Weitere Sitzung');
    });

    it('refuses to attach an editor to a logout observed while its initial database open was pending', async () => {
        const tab = await openTab();
        const opening = tab.openGeneration();
        logout.marker = 'logout';
        await expect(opening).rejects.toMatchObject({name: 'AbortError'});
        expect(await tab.openGeneration()).toBe('logout');
    });

    it.each([undefined, 'previous-logout'])('fences writes using IndexedDB even if publishing the next marker failed (%s)', async (marker) => {
        logout.marker = marker;
        const oldTab = await openTab();
        const old = await oldTab.openGeneration();
        await oldTab.save(draft('Alter Entwurf'), old);
        const other = await openTab();
        await other.clear('unpublished-logout', false);
        await oldTab.save(draft('Verspätete Sicherung'), old);
        locks.held.clear();
        const fresh = await openTab();
        expect(await fresh.openGeneration()).toBe('unpublished-logout');
        expect(await fresh.load(user, key)).toBeNull();
        await fresh.clear('second-unpublished-logout', false);
        const latest = await fresh.openGeneration();
        await fresh.save(draft('Neue Sitzung'), latest);
        await other.clear('unpublished-logout', false);
        if (marker != null) await other.clear(marker, true);
        expect((await fresh.load(user, key, undefined, latest))?.mine).toBe('Neue Sitzung');
    });

    it('prioritizes the matching return over a newer abandoned draft', async () => {
        const old = await openTab();
        await old.save({...draft('Zur Anmeldung'), savedAt: Date.now() - 60_000});
        await old.reserve(user, key, 'return');
        const later = await openTab();
        await later.save(draft('Neuer, aber anderer Tab'));
        locks.held.clear();
        const returning = await openTab();
        expect((await returning.load(user, key, 'return'))?.mine).toBe('Zur Anmeldung');
        expect((await (await openTab()).load(user, key))?.mine).toBe('Neuer, aber anderer Tab');
    });

    it('returns the reserved draft to the matching login and keeps another tab independent', async () => {
        const old = await openTab();
        const saveCheckpoint = {phase: 'pending' as const, submitted: 'Beim Speichern'};
        await old.save({...draft('Vor der Anmeldung'), context: {title: 'Erste offene Bearbeitung'}, saveCheckpoint});
        expect(await old.reserve(user, key, 'return')).toBe(true);
        locks.held.clear();
        const fresh = await openTab();
        expect(await fresh.load(user, key)).toBeNull();
        expect(await fresh.load(user, key, 'other-login')).toBeNull();
        expect(await fresh.load('other-user', key, 'return')).toBeNull();
        expect(await fresh.load(user, 'other-object', 'return')).toBeNull();
        await fresh.save({...draft('Zweiter Tab'), context: {title: 'Zweite offene Bearbeitung'}});
        const returning = await openTab();
        expect(await returning.load(user, key, 'return')).toMatchObject({
            mine: 'Vor der Anmeldung', context: {title: 'Erste offene Bearbeitung'}, saveCheckpoint,
        });
        // Dismissing a nested edit leaves the restored outer draft and the other tab's nested edits intact.
        await returning.save(draft('Vor der Anmeldung'));
        const restored = await returning.load<string, {title: string}>(user, key);
        expect(restored?.mine).toBe('Vor der Anmeldung');
        expect(restored?.context).toBeUndefined();
        expect(restored?.saveCheckpoint).toBeUndefined();
        expect(await fresh.load(user, key)).toMatchObject({
            mine: 'Zweiter Tab', context: {title: 'Zweite offene Bearbeitung'},
        });
        expect(await (await openTab()).load(user, key, 'return')).toBeNull();
    });

    it('makes an abandoned reservation available after ten minutes and expires old backups', async () => {
        const now = Date.now();
        const clock = vi.spyOn(Date, 'now').mockReturnValue(now);
        const old = await openTab();
        await old.save(draft('Reserviert'));
        await old.reserve(user, key, 'abandoned');
        await old.save({...draft('Abgelaufen'), key: 'expired', savedAt: now - 15 * 86_400_000});
        locks.held.clear();
        const next = await openTab();
        clock.mockReturnValue(now + 599_999);
        expect(await next.load(user, key)).toBeNull();
        clock.mockReturnValue(now + 600_001);
        expect((await next.load(user, key))?.mine).toBe('Reserviert');
        expect(await next.load(user, 'expired')).toBeNull();
    });
});
