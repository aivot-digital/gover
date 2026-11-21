import assert from 'node:assert/strict';
import {spawn} from 'node:child_process';
import {mkdtemp, rm, writeFile} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import path from 'node:path';
import {setTimeout as delay} from 'node:timers/promises';
import {fileURLToPath, pathToFileURL} from 'node:url';
import {test} from 'node:test';
import {build} from 'esbuild';

// Chrome's debugging pipe sends NUL-delimited CDP messages over file descriptors 3 and 4.
// A local fixture gives every document the same origin without starting a web server.
function connect(chrome) {
    let nextId = 0;
    const pending = new Map();
    let buffer = '';
    chrome.stdio[4].setEncoding('utf8').on('data', (chunk) => {
        buffer += chunk;
        let end;
        while ((end = buffer.indexOf('\0')) !== -1) {
            const message = JSON.parse(buffer.slice(0, end));
            buffer = buffer.slice(end + 1);
            const request = pending.get(message.id);
            if (request) {
                pending.delete(message.id);
                if (message.error) {
                    request.reject(new Error(message.error.message));
                } else {
                    request.resolve(message.result);
                }
            }
        }
    });
    let closed = false;
    chrome.once('close', () => {
        closed = true;
        for (const request of pending.values()) request.reject(new Error('Chrome closed'));
        pending.clear();
    });
    return (method, params, sessionId) => new Promise((resolve, reject) => {
        if (closed) return reject(new Error('Chrome closed'));
        const id = ++nextId;
        pending.set(id, {resolve, reject});
        chrome.stdio[3].write(JSON.stringify({id, method, params, sessionId}) + '\0');
    });
}

async function until(probe) {
    const deadline = Date.now() + 5_000;
    while (!await probe()) {
        if (Date.now() > deadline) throw new Error(`Timed out waiting for ${probe}`);
        await delay(10);
    }
}

const user = 'user';
const draft = (key, mine) => ({key, mine, userId: user, savedAt: Date.now(), base: 'original', baseLockVersion: 1});

// Pause exactly between the candidate lookup and lock acquisition, without replacing either real mechanism.
function pauseLock(name) {
    const acquire = navigator.locks.request.bind(navigator.locks);
    const gate = new Promise(resolve => { window.allowLock = resolve; });
    navigator.locks.request = async (...args) => {
        if (!name || name === args[0]) {
            window.waitingForLock = true;
            await gate;
        }
        return acquire(...args);
    };
}

function readStoredDrafts() {
    return new Promise((resolve, reject) => {
        const opening = indexedDB.open('prosuna_editor_drafts');
        opening.onerror = () => reject(opening.error);
        opening.onsuccess = () => {
            const db = opening.result;
            const tx = db.transaction('drafts');
            const rows = tx.objectStore('drafts').getAll();
            tx.oncomplete = () => { db.close(); resolve(rows.result); };
            tx.onabort = () => { db.close(); reject(tx.error); };
        };
    });
}

function seedV2Drafts(rows, keepConnectionOpen = false) {
    return new Promise((resolve, reject) => {
        const opening = indexedDB.open('prosuna_editor_drafts', 2);
        opening.onupgradeneeded = () => opening.result.createObjectStore('drafts', {keyPath: ['userId', 'key', 'tabId']});
        opening.onerror = () => reject(opening.error);
        opening.onsuccess = () => {
            const db = opening.result;
            const tx = db.transaction('drafts', 'readwrite');
            for (const row of rows) tx.objectStore('drafts').put(row);
            tx.oncomplete = () => {
                if (keepConnectionOpen) window.legacyConnection = db;
                else db.close();
                resolve();
            };
            tx.onabort = () => reject(tx.error);
        };
    });
}

const chromeExecutable = process.env.CHROME_BIN ?? ({
    darwin: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    win32: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
}[process.platform] ?? 'google-chrome');

test('editor drafts in independent Chrome documents', async (t) => {
    const bundle = await build({
        stdin: {
            contents: `
                export * from './editor-draft-storage';
                export * from './editor-draft-login';
                export {broadcastCommittedEditorDraftLogout} from './editor-draft-logout';
                export {AuthService} from './auth-service';`,
            resolveDir: fileURLToPath(new URL('../src/services/', import.meta.url)),
        },
        format: 'iife', globalName: 'draftModule', bundle: true, write: false,
    });
    const profile = await mkdtemp(path.join(tmpdir(), 'prosuna-editor-test-'));
    const fixture = path.join(profile, 'editor.html');
    const loginPage = path.join(profile, 'login.html');
    await writeFile(loginPage, '<title>Login</title>');
    await writeFile(fixture, `<script>if (location.search === '?pause') {
        (${pauseLock})();
        window.addEventListener('storage', event => event.stopImmediatePropagation());
    }</script>
        <script>if (location.search !== '?legacy') { ${bundle.outputFiles[0].text}
draftModule.initializeEditorDraftLoginReturn(); }</script>`);
    const chrome = spawn(chromeExecutable, [
        '--headless', '--disable-background-networking', '--no-first-run', '--no-default-browser-check',
        '--remote-debugging-pipe', `--user-data-dir=${profile}`, 'about:blank',
    ], {stdio: ['ignore', 'ignore', 'ignore', 'pipe', 'pipe']});
    const exited = new Promise(resolve => { chrome.once('close', resolve); });
    chrome.once('error', error => t.diagnostic(error.message));
    const kill = setTimeout(() => chrome.kill('SIGKILL'), 30_000);
    t.after(async () => {
        chrome.kill();
        await exited;
        clearTimeout(kill);
        await rm(profile, {recursive: true, force: true, maxRetries: 3});
    });
    const send = connect(chrome);
    async function openTab({pauseRegistration = false, browserContextId, withoutEditor = false} = {}) {
        const url = pathToFileURL(fixture);
        if (withoutEditor) url.search = 'legacy';
        if (pauseRegistration) url.search = 'pause';
        const {targetId} = await send('Target.createTarget', {url: url.href, browserContextId});
        const {sessionId} = await send('Target.attachToTarget', {targetId, flatten: true});
        const evaluate = async (fn, ...args) => {
            const expression = `(${fn})(...${JSON.stringify(args)})`;
            const {result, exceptionDetails} = await send('Runtime.evaluate', {expression, returnByValue: true, awaitPromise: true}, sessionId);
            if (exceptionDetails) throw new Error(exceptionDetails.exception?.description ?? exceptionDetails.text);
            return result.value;
        };
        const ready = withoutEditor
            ? () => location.search === '?legacy' && document.readyState === 'complete'
            : () => typeof draftModule !== 'undefined';
        await until(() => evaluate(ready));
        return {
            evaluate,
            call: (method, ...args) => evaluate((method, args) => draftModule.EditorDraftStorage[method](...args), method, args),
            close: () => send('Target.closeTarget', {targetId}),
        };
    }

    const first = await openTab();
    const second = await openTab();
    async function writeAndGetLock(owner, data) {
        await owner.call('save', data);
        const {tabId} = await owner.call('load', user, data.key);
        return `prosuna_editor_draft_tab/${tabId}`;
    }
    function waitForRelease(name) {
        return until(() => first.evaluate(async name => !(await navigator.locks.query()).held.some(lock => lock.name === name), name));
    }

    await t.test('a blocked v1 upgrade fails promptly and can finish after the old connection closes', async () => {
        await first.evaluate(() => new Promise((resolve, reject) => {
            const req = indexedDB.open('prosuna_editor_drafts', 1);
            req.onupgradeneeded = () => req.result.createObjectStore('drafts', {keyPath: ['userId', 'key']});
            req.onsuccess = () => { window.oldConnection = req.result; resolve(); };
            req.onerror = () => reject(req.error);
        }));
        await Promise.all([
            assert.rejects(second.call('load', user, 'a'), /blocked/i),
            assert.rejects(second.call('load', user, 'b'), /blocked/i),
        ]);
        await assert.rejects(second.call('save', draft('upgrade', 'pending')), /blocked/i);
        await first.evaluate(() => window.oldConnection.close());
        await until(async () => {
            try {
                return await second.call('load', user, 'upgrade') === null;
            } catch (error) {
                if (/blocked/i.test(error.message)) return false;
                throw error;
            }
        });
    });

    await t.test('three independent documents keep login return reservations separate', async () => {
        const key = 'login';
        const a = await openTab();
        const b = await openTab();
        const lockA = await writeAndGetLock(a, draft(key, 'first edit'));
        const panelB = {base: {id: 'field', label: 'Title'}, mine: {id: 'field', label: 'Tab B'}};
        const lockB = await writeAndGetLock(b, {...draft(key, 'second edit'), context: panelB});
        assert.equal(await a.call('reserve', user, key, 'return-a'), true);
        assert.equal(await b.call('reserve', user, key, 'return-b'), true);
        // Autosave may flush again while the redirect is already being prepared.
        const panelA = {...panelB, mine: {id: 'field', label: 'Tab A'}};
        await a.call('save', {...draft(key, 'last edit in A'), context: panelA});
        await Promise.all([a.close(), b.close()]);
        await Promise.all([waitForRelease(lockA), waitForRelease(lockB)]);
        const fresh = await openTab();
        assert.equal(await fresh.call('load', user, key), null);
        assert.equal(await fresh.call('load', user, key, 'wrong-id'), null);
        await fresh.call('save', draft(key, 'third edit'));
        const returnedB = await second.call('load', user, key, 'return-b');
        const returnedA = await first.call('load', user, key, 'return-a');
        const third = await fresh.call('load', user, key);
        assert.equal(returnedB.mine, 'second edit');
        assert.deepEqual(returnedB.context, panelB);
        assert.equal(returnedA.mine, 'last edit in A');
        assert.deepEqual(returnedA.context, panelA);
        assert.equal(third.mine, 'third edit');
        assert.equal(third.context, undefined);
        await fresh.close();
    });

    await t.test('a redirect in the original tab returns its own draft through the login coordinator', async () => {
        const key = 'login-navigation';
        const owner = await openTab();
        const lock = await writeAndGetLock(owner, draft(key, 'before login'));
        const callback = await owner.evaluate(async (user, key) => {
            history.replaceState(null, '', location.pathname + '?section=fields#selected');
            draftModule.registerEditorDraftLoginHandler(id => draftModule.EditorDraftStorage.reserve(user, key, id));
            const id = await draftModule.prepareEditorDraftLogin();
            const login = new URL(draftModule.AuthService.getLoginUrl({pathname: '/editor', search: location.search}, id), location.href);
            // The simulated identity provider returns to the app_uri produced by the real login code.
            const appUri = new URL(login.searchParams.get('app_uri'), location.href);
            const callback = new URL(location.href);
            callback.search = appUri.search;
            callback.hash = appUri.hash;
            return callback.href;
        }, user, key);
        assert.ok(new URL(callback).searchParams.get('editor_draft_return'));
        await owner.evaluate(url => location.assign(url), pathToFileURL(loginPage).href);
        await until(() => owner.evaluate(() => document.title === 'Login'));
        await waitForRelease(lock);
        assert.equal(await first.call('load', user, key), null);
        const fresh = await openTab();
        await fresh.evaluate(url => location.assign(url), callback);
        await until(() => fresh.evaluate(() => location.hash === '#selected' && typeof draftModule !== 'undefined'));
        assert.equal(await fresh.evaluate((user, key) => draftModule.EditorDraftStorage.load(user, key, draftModule.getEditorDraftReturnId()), user, key), null);
        await owner.evaluate(url => location.assign(url), callback);
        await until(() => owner.evaluate(() => typeof draftModule !== 'undefined'));
        const returned = await owner.evaluate(async (user, key) => ({
            row: await draftModule.EditorDraftStorage.load(user, key, draftModule.getEditorDraftReturnId()),
            search: location.search,
            hash: location.hash,
        }), user, key);
        assert.equal(returned.row.mine, 'before login');
        assert.equal(returned.search, '?section=fields');
        assert.equal(returned.hash, '#selected');
        await Promise.all([owner.close(), fresh.close()]);
    });

    await t.test('the v2 upgrade preserves existing drafts and login reservations until explicit logout', async () => {
        const {browserContextId} = await send('Target.createBrowserContext', {disposeOnDetach: true});
        const legacy = await openTab({withoutEditor: true, browserContextId});
        const row = {
            ...draft('legacy', 'retained'), tabId: 'older-client',
            context: {base: {id: 'field', label: 'Title'}, mine: {id: 'field', label: 'Open edit'}},
            saveCheckpoint: {phase: 'pending', submitted: 'submitted'},
        };
        const reserved = {...row, key: 'reserved-legacy', reservation: {id: 'old-return', expiresAt: Date.now() + 60_000}};
        await legacy.evaluate(seedV2Drafts, [row, reserved]);
        await legacy.close();
        const existing = await openTab({browserContextId});
        const returning = await openTab({browserContextId});
        const recovered = await existing.call('load', user, row.key);
        assert.equal(recovered.mine, row.mine);
        assert.deepEqual(recovered.context, row.context);
        assert.deepEqual(recovered.saveCheckpoint, row.saveCheckpoint);
        assert.equal(await returning.call('load', user, row.key), null);
        assert.equal(await existing.call('load', user, reserved.key), null);
        assert.equal((await returning.call('load', user, reserved.key, 'old-return')).mine, reserved.mine);
        await returning.evaluate(() => draftModule.beginExplicitEditorDraftLogout());
        assert.deepEqual(await existing.evaluate(readStoredDrafts), []);
        await send('Target.disposeBrowserContext', {browserContextId});
    });

    await t.test('persistent writes wait for owner registration', async () => {
        const owner = await openTab({pauseRegistration: true});
        const write = owner.call('save', draft('delayed', 'live draft'));
        await until(() => owner.evaluate(() => window.waitingForLock));
        assert.equal(await first.call('load', user, 'delayed'), null);
        await owner.evaluate(() => window.allowLock());
        await write;
        assert.equal(await first.call('load', user, 'delayed'), null);
        await owner.close();
    });

    await t.test('at most one of two tabs can adopt the same closed draft', async () => {
        const owner = await openTab();
        const lock = await writeAndGetLock(owner, draft('closed', 'left behind'));
        await Promise.all([first, second].map(tab => tab.evaluate(pauseLock, lock)));
        const reads = Promise.all([first, second].map(tab => tab.call('load', user, 'closed')));
        await Promise.all([first, second].map(tab => until(() => tab.evaluate(() => window.waitingForLock))));
        await owner.close();
        await waitForRelease(lock);
        await Promise.all([first, second].map(tab => tab.evaluate(() => window.allowLock())));
        const results = await reads;
        assert.deepEqual(results.filter(Boolean).map(row => row.mine), ['left behind']);
        assert.ok(results.includes(null));
    });

    for (const operation of ['remove', 'save']) {
        await t.test(`a ${operation} after the candidate lookup is respected during adoption`, async () => {
            const key = `race-${operation}`;
            const reader = await openTab();
            const owner = await openTab();
            const lock = await writeAndGetLock(owner, draft(key, 'stale'));
            await reader.evaluate(pauseLock, lock);
            const read = reader.call('load', user, key);
            await until(() => reader.evaluate(() => window.waitingForLock));
            const latest = {...draft(key, 'revised'), base: 'new base', baseLockVersion: 2};
            if (operation === 'remove') {
                await owner.call('remove', user, key);
            } else {
                await owner.call('save', latest);
            }
            await owner.close();
            await waitForRelease(lock);
            await reader.evaluate(() => window.allowLock());
            const recovered = await read;
            if (operation === 'remove') {
                assert.equal(recovered, null);
            } else {
                for (const [field, value] of Object.entries(latest)) {
                    assert.equal(recovered[field], value, field);
                }
            }
            await reader.close();
        });
    }

    await t.test('a late transaction abort rolls back the complete move', async () => {
        const owner = await openTab();
        const reader = await openTab();
        const lock = await writeAndGetLock(owner, draft('aborted', 'survives abort'));
        await owner.close();
        await waitForRelease(lock);
        await reader.evaluate(() => {
            const remove = IDBObjectStore.prototype.delete;
            IDBObjectStore.prototype.delete = function (key) {
                const result = remove.call(this, key);
                result.addEventListener('success', () => this.transaction.abort());
                return result;
            };
        });
        await assert.rejects(reader.call('load', user, 'aborted'), /abort/i);
        // The failed claimant still holds its own lock, so a partial move couldn't be recovered here.
        assert.equal((await first.call('load', user, 'aborted')).mine, 'survives abort');
        assert.equal(await reader.call('load', user, 'aborted'), null);
        await reader.close();
    });

    await t.test('a write waiting for its owner lock cannot recreate a draft after logout in another tab', async () => {
        const key = 'logout-delayed';
        const delayed = await openTab({pauseRegistration: true});
        const generation = await delayed.call('openGeneration');
        await first.call('save', draft(key, 'before logout'), generation);
        await first.call('save', {...draft(key, 'other account'), userId: 'other'}, generation);
        assert.equal(await first.call('reserve', user, key, 'before-logout', generation), true);
        await first.evaluate(() => {
            window.stopListening = draftModule.registerEditorDraftLoginHandler(async () => false, async id => { window.explicitLogout = id; });
        });
        await delayed.evaluate(() => {
            // This background document starts with storage event delivery paused.
            draftModule.registerEditorDraftLoginHandler(async () => false, async () => { window.stoppedOldSession = true; });
        });

        const lateSave = delayed.call('save', draft(key, 'delayed write'), generation);
        await until(() => delayed.evaluate(() => window.waitingForLock));
        await second.evaluate(() => draftModule.beginExplicitEditorDraftLogout());
        await until(() => first.evaluate(() => typeof window.explicitLogout === 'string'));
        const logoutId = await first.evaluate(() => { window.stopListening(); return window.explicitLogout; });
        await delayed.evaluate(() => window.allowLock());
        await lateSave;
        assert.deepEqual(await first.evaluate(readStoredDrafts), []);
        assert.notEqual(await delayed.evaluate(() => window.stoppedOldSession), true);
        await delayed.close();

        const fresh = await openTab();
        const current = await fresh.call('openGeneration');
        assert.notEqual(current, generation);
        assert.equal(await fresh.call('load', user, key), null);
        assert.equal(await fresh.call('load', user, key, 'before-logout'), null);
        await fresh.call('save', draft(key, 'new edit after signing in'), current);
        await second.call('clear', logoutId, true);
        // Use the same tab and key here so its tab ID alone cannot protect this new edit.
        await fresh.call('save', draft(key, 'obsolete update'), generation);
        await fresh.call('remove', user, key, generation);
        assert.equal(await fresh.call('reserve', user, key, 'late-reservation', generation), false);
        assert.equal(await fresh.call('load', user, key, null, generation), null);
        assert.equal((await fresh.call('load', user, key, null, current)).mine, 'new edit after signing in');
        await fresh.close();
    });

    await t.test('logout clears a write whose IDB transaction began before the logout transaction', async () => {
        const writer = await openTab();
        const loggingOut = await openTab();
        const generation = await writer.call('openGeneration');
        await writer.evaluate(() => {
            const put = IDBObjectStore.prototype.put;
            IDBObjectStore.prototype.put = function (row, ...args) {
                const request = put.call(this, row, ...args);
                if (this.name === 'drafts' && row.key === 'logout-in-flight') {
                    window.writing = true;
                    // Keep this real write transaction active until the other tab requests its transaction.
                    const keepActive = () => {
                        if (!window.finishWrite) this.count().onsuccess = keepActive;
                    };
                    keepActive();
                }
                return request;
            };
        });
        await loggingOut.call('openGeneration');
        await loggingOut.evaluate(() => {
            const transaction = IDBDatabase.prototype.transaction;
            IDBDatabase.prototype.transaction = function (stores, mode, ...args) {
                const tx = transaction.call(this, stores, mode, ...args);
                if (mode === 'readwrite' && tx.objectStoreNames.contains('drafts')) window.requestedCleanup = true;
                return tx;
            };
        });
        const writing = writer.call('save', draft('logout-in-flight', 'in progress'), generation);
        await until(() => writer.evaluate(() => window.writing));
        const clearing = loggingOut.evaluate(() => draftModule.beginExplicitEditorDraftLogout());
        try {
            await until(() => loggingOut.evaluate(() => window.requestedCleanup));
        } finally {
            await writer.evaluate(() => { window.finishWrite = true; });
        }
        await Promise.all([writing, clearing]);
        assert.deepEqual(await writer.evaluate(readStoredDrafts), []);
        await Promise.all([writer.close(), loggingOut.close()]);
    });

    await t.test('a committed logout crosses tabs without localStorage and a delayed older broadcast is ignored', async () => {
        const {browserContextId} = await send('Target.createBrowserContext', {disposeOnDetach: true});
        const editor = await openTab({browserContextId});
        const sender = await openTab({browserContextId});
        await sender.evaluate(() => {
            const setItem = Storage.prototype.setItem;
            Storage.prototype.setItem = function (...args) {
                if (this === localStorage) throw new DOMException('Storage full', 'QuotaExceededError');
                return setItem.apply(this, args);
            };
        });
        await editor.evaluate(() => {
            window.logouts = [];
            window.rejectedBroadcasts = [];
            const storage = draftModule.EditorDraftStorage;
            const check = storage.isCurrentGeneration.bind(storage);
            storage.isCurrentGeneration = async id => {
                const current = await check(id);
                if (!current) window.rejectedBroadcasts.push(id);
                return current;
            };
            draftModule.registerEditorDraftLoginHandler(async () => false, async id => { window.logouts.push(id); });
        });
        const key = 'broadcast-logout';
        await editor.call('save', draft(key, 'old edit'));
        await sender.evaluate(() => draftModule.beginExplicitEditorDraftLogout());
        await until(() => editor.evaluate(() => window.logouts.length === 1));
        const firstLogout = await editor.call('openGeneration');
        assert.deepEqual(await editor.evaluate(readStoredDrafts), []);
        await sender.evaluate(() => draftModule.beginExplicitEditorDraftLogout());
        await until(() => editor.evaluate(() => window.logouts.length === 2));
        const secondLogout = await editor.call('openGeneration');
        assert.notEqual(firstLogout, secondLogout);
        await editor.call('save', draft(key, 'edit after sign-in'), secondLogout);
        await sender.evaluate(id => draftModule.broadcastCommittedEditorDraftLogout(id), firstLogout);
        await until(() => editor.evaluate(id => window.rejectedBroadcasts.includes(id), firstLogout));
        assert.deepEqual(await editor.evaluate(() => window.logouts), [firstLogout, secondLogout]);
        assert.equal((await editor.call('load', user, key, null, secondLogout)).mine, 'edit after sign-in');
        await send('Target.disposeBrowserContext', {browserContextId});
    });

    await t.test('the next document completes a logout when IndexedDB was blocked until the logging-out tab closed', async () => {
        const {browserContextId} = await send('Target.createBrowserContext', {disposeOnDetach: true});
        const legacy = await openTab({withoutEditor: true, browserContextId});
        const key = 'missed-cleanup';
        await legacy.evaluate(seedV2Drafts, [{
            ...draft(key, 'old edit'), tabId: 'older-client', reservation: {id: 'old-return', expiresAt: Date.now() + 60_000},
        }], true);
        const interrupted = await openTab({browserContextId});
        await interrupted.evaluate(() => {
            const open = indexedDB.open.bind(indexedDB);
            indexedDB.open = (...args) => {
                const request = open(...args);
                request.addEventListener('blocked', () => { window.blockedCleanup = true; });
                return request;
            };
            // The user closes the tab once the real clear is blocked by the old connection.
            draftModule.beginExplicitEditorDraftLogout().catch(() => {});
        });
        await until(() => interrupted.evaluate(() => window.blockedCleanup));
        await interrupted.close();
        await legacy.close();
        const fresh = await openTab({browserContextId});
        assert.equal(await fresh.call('load', user, key, 'old-return'), null);
        assert.deepEqual(await fresh.evaluate(readStoredDrafts), []);
        const current = await fresh.call('openGeneration');
        await fresh.call('save', draft(key, 'newly signed in'), current);
        assert.equal((await fresh.call('load', user, key)).mine, 'newly signed in');
        await send('Target.disposeBrowserContext', {browserContextId});
    });
});
