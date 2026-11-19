import {
    type MergeOutcome, prepareDependentMerge, prepareMerge, prepareScopedMerge, type PreparedMerge,
    type PreparedMergeRequest,
} from '../hooks/use-merge-conflict-resolution';
import {isApiError, isLockVersionConflictError} from '../models/api-error';
import type {EditorDraft} from '../services/editor-draft-storage';
import {deepEquals} from './equality-utils';
import {type MergeChoices, type MergeSpec} from './three-way-merge';

/** The first snapshot remains the base for edits made after the click, even across 409 retries. */
export interface EditorSaveCheckpoint<T> {
    phase: 'pending' | 'confirmed';
    submitted: T;
    /** Version sent with the PUT, which can differ from the initial draft version on a retry. */
    baseLockVersion?: number | null;
    /** A retry may send a different, explicitly merged value on top of a newer server base. */
    attempt?: {base: T; mine: T};
}

/** A GET at the same version can precede the interrupted PUT's commit. */
export function canPendingSaveStillCommit<T>(
    checkpoint: EditorSaveCheckpoint<T> | undefined,
    draftBaseLockVersion: number | null | undefined,
    storedLockVersion: number | null | undefined,
): boolean {
    if (checkpoint?.phase !== 'pending') return false;
    const attemptedVersion = checkpoint.baseLockVersion === undefined ? draftBaseLockVersion : checkpoint.baseLockVersion;
    return attemptedVersion == null || storedLockVersion == null || storedLockVersion <= attemptedVersion;
}

/** Compare only values this editor can persist, using the same rules as draft recovery. */
export function pendingSaveCanChangeStored<T>(
    checkpoint: EditorSaveCheckpoint<T> | undefined, stored: T, spec: MergeSpec<T>,
): boolean {
    if (checkpoint?.phase !== 'pending') return false;
    const payload = checkpoint.attempt ? checkpoint.attempt.mine : checkpoint.submitted;
    const proposed = prepareMerge({base: stored, mine: payload, theirs: stored, spec}, {});
    return proposed.changed || proposed.conflicts.length > 0;
}

/** The server can commit before a proxy error or an interrupted or malformed success response. */
export function isEditorSaveOutcomeUnknown(error: unknown): boolean {
    if (isApiError(error)) return error.status >= 500 && error.status < 600;
    return error instanceof SyntaxError || error instanceof TypeError ||
        (error instanceof DOMException && (error.name === 'AbortError' || error.name === 'TimeoutError'));
}

/** For a confirmed write the latest typed value wins. Structure and ordering need a decision. */
export function preparePostSaveMerge<T>(
    submitted: T, current: T, confirmed: T, spec: MergeSpec<T>, choices: MergeChoices,
): PreparedMerge<T> {
    return prepareMerge({
        base: submitted, mine: current, theirs: confirmed,
        spec: {
            ...spec,
            defaultChoice: ({base, mine, theirs}) =>
                [base, mine, theirs].every((value) => value == null || typeof value !== 'object') ? 'mine' : undefined,
        },
    }, choices);
}

export function rebaseAfterSave<T>(
    submitted: T, current: T, confirmed: T, spec: MergeSpec<T>,
): {value: T; checkpoint?: EditorSaveCheckpoint<T>} {
    const choices: MergeChoices = {};
    let result = preparePostSaveMerge(submitted, current, confirmed, spec, choices);
    if (result.conflicts.length === 0) return {value: result.value};

    // Display the person's working copy now. Keep the ancestor so that the next explicit save
    // can ask about a deleted or retyped element without silently writing this preview.
    while (result.conflicts.length > 0) {
        for (const {id} of result.conflicts) choices[id] = 'mine';
        result = preparePostSaveMerge(submitted, current, confirmed, spec, choices);
    }
    return {value: result.value, checkpoint: {phase: 'confirmed', submitted}};
}

export function prepareEditorDraftRecovery<T>(
    draft: Pick<EditorDraft<T>, 'base' | 'mine' | 'saveCheckpoint'>, stored: T, spec: MergeSpec<T>, choices: MergeChoices,
): PreparedMerge<T> {
    const checkpoint = draft.saveCheckpoint;
    if (checkpoint == null) {
        return prepareMerge({base: draft.base, mine: draft.mine, theirs: stored, spec}, choices);
    }
    return prepareDependentMerge((decisions) => {
        const confirmed = checkpoint.phase === 'confirmed';
        const {attempt, submitted} = checkpoint;
        const payload = attempt == null ? submitted : attempt.mine;
        let current = draft.mine;
        if (confirmed || attempt != null) {
            const applied = prepareScopedMerge('post-save', decisions, (local) => preparePostSaveMerge(
                submitted, current, confirmed ? draft.base : payload, spec, local,
            ));
            if (applied.conflicts.length > 0) return applied;
            current = applied.value;
        }
        const base = !confirmed && attempt != null ? attempt.base : draft.base;
        const interruptedWrite = confirmed ? undefined : {value: payload};
        return prepareScopedMerge('stored', decisions, (local) =>
            prepareMerge({base, mine: current, theirs: stored, spec}, local, interruptedWrite));
    }, choices);
}

interface EditorSaveOptions<T, S> {
    original: S;
    submitted: T;
    checkpoint?: EditorSaveCheckpoint<T>;
    read: () => Promise<S>;
    write: (base: S, value: T, reserveVersion: boolean) => Promise<S>;
    valueOf: (stored: S) => T;
    specFor: (stored: S) => MergeSpec<T>;
    setPending: (checkpoint: EditorSaveCheckpoint<T>) => void;
    resolvePrepared: (request: PreparedMergeRequest<T>) => Promise<MergeOutcome<T>>;
    onRead?: (stored: S) => void;
    signal: AbortSignal;
    /** A resumed editor needs confirmation from the current server session before it may back up new edits. */
    requireServerConfirmation?: boolean;
}

export const signInToSaveEditorMessage = 'Melden Sie sich zum Speichern wieder mit dem Konto an, mit dem Sie diesen Editor geöffnet haben.';

type EditorSaveResult<S> =
    | {status: 'saved'; stored: S; merged: boolean}
    | {status: 'discarded'; stored: S}
    | {status: 'cancel'}
    | {status: 'unknown'; error: unknown};

const MAX_SAVE_ATTEMPTS = 3;

/** Persist one click's snapshot, keeping later edits out of every retry. */
export async function persistEditorSave<T, S extends {lockVersion?: number | null}>({
    original, submitted, checkpoint, read, write, valueOf, specFor, setPending, resolvePrepared, onRead, signal, requireServerConfirmation,
}: EditorSaveOptions<T, S>): Promise<EditorSaveResult<S>> {
    const readStored = async () => {
        const stored = await read();
        signal.throwIfAborted();
        onRead?.(stored);
        return stored;
    };
    const decide = async (merge: PreparedMergeRequest<T>['merge']) => {
        const outcome = await resolvePrepared({mode: 'save', storedStateChanged: true, signal, merge});
        signal.throwIfAborted();
        return outcome;
    };
    const recover = (stored: S, choices: MergeChoices) => prepareEditorDraftRecovery({
        base: valueOf(original), mine: submitted, saveCheckpoint: checkpoint,
    }, valueOf(stored), specFor(stored), choices);
    const matchesStored = (value: T, stored: S) => {
        const {normalize = (input: T) => input, atomicValue} = specFor(stored);
        return deepEquals(normalize(value), normalize(valueOf(stored)), atomicValue);
    };

    signal.throwIfAborted();
    let base = original;
    let payload = submitted;
    let previousPutMayCommit = false;
    let merged = false;
    if (checkpoint) {
        if (checkpoint.phase === 'pending') {
            base = await readStored();
            previousPutMayCommit = canPendingSaveStillCommit(checkpoint, original.lockVersion, base.lockVersion) &&
                pendingSaveCanChangeStored(checkpoint, valueOf(base), specFor(base));
        }
        const outcome = await decide((choices) => checkpoint.phase === 'pending'
            ? recover(base, choices)
            : preparePostSaveMerge(checkpoint.submitted, submitted, valueOf(base), specFor(base), choices));
        if (outcome.status === 'discard') return {status: 'discarded', stored: base};
        if (outcome.status !== 'apply') return {status: 'cancel'};
        payload = outcome.value;
        if (!previousPutMayCommit && matchesStored(payload, base) && (checkpoint.phase === 'pending' || !requireServerConfirmation)) {
            return {status: 'saved', stored: base, merged};
        }
    }

    for (let attempt = 1; ; attempt++) {
        signal.throwIfAborted();
        const attemptBase = valueOf(base);
        // An interrupted previous PUT and this one can race for the same version. When sending
        // a third possible value, first advance the version with its unchanged contents. A
        // second interruption can then always be recovered with the single persisted checkpoint.
        const reserveVersion = previousPutMayCommit && !matchesStored(payload, base) &&
            pendingSaveCanChangeStored(checkpoint, payload, specFor(base));
        if (!previousPutMayCommit) {
            setPending({
                phase: 'pending', submitted, baseLockVersion: base.lockVersion,
                attempt: base === original && payload === submitted ? undefined : {base: attemptBase, mine: payload},
            });
        }
        let stored: S;
        let reservationConfirmed = false;
        try {
            stored = await write(base, reserveVersion ? attemptBase : payload, reserveVersion);
            signal.throwIfAborted();
            if (!reserveVersion) return {status: 'saved', stored, merged};
            reservationConfirmed = true;
        } catch (error) {
            signal.throwIfAborted();
            if (isEditorSaveOutcomeUnknown(error)) return {status: 'unknown', error};
            if (!isLockVersionConflictError(error) || attempt >= MAX_SAVE_ATTEMPTS) throw error;
            if (!reserveVersion) merged = true;
            stored = await readStored();
        }

        const outcome = await decide((choices) => {
            const latest = valueOf(stored);
            const spec = specFor(stored);
            if (reservationConfirmed) return preparePostSaveMerge(attemptBase, payload, latest, spec, choices);
            if (previousPutMayCommit) return recover(stored, choices);
            return prepareMerge({base: attemptBase, mine: payload, theirs: latest, spec}, choices);
        });
        if (outcome.status === 'discard') return {status: 'discarded', stored};
        if (outcome.status !== 'apply') return {status: 'cancel'};
        payload = outcome.value;
        if (reservationConfirmed && matchesStored(payload, stored)) return {status: 'saved', stored, merged};
        base = stored;
        previousPutMayCommit = false;
    }
}
