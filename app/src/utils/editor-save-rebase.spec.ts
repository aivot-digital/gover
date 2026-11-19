import {describe, expect, it} from 'vitest';
import {API_ERROR_REASON_NETWORK_UNREACHABLE, API_ERROR_REASON_TIMEOUT} from '../models/api-error';
import {
    canPendingSaveStillCommit, isEditorSaveOutcomeUnknown, pendingSaveCanChangeStored, prepareEditorDraftRecovery,
    preparePostSaveMerge, rebaseAfterSave,
} from './editor-save-rebase';
import {keyById, type MergeSpec} from './three-way-merge';

const original = {name: 'A', notes: 'Old', lockVersion: 1};
const submitted = {...original, name: 'B'};
const pending = {phase: 'pending', submitted} as const;
const spec: MergeSpec<typeof original> = {serverManaged: ['lockVersion']};

describe('reconciling a save with ongoing editing', () => {
    it('retains a possible write until a read proves its attempted lock version has advanced', () => {
        const retry = {...pending, baseLockVersion: 2};
        expect(canPendingSaveStillCommit(retry, 1, 2)).toBe(true);
        expect(canPendingSaveStillCommit(retry, 1, 3)).toBe(false);
        expect(canPendingSaveStillCommit(retry, 1, null)).toBe(true);
        expect(canPendingSaveStillCommit({...pending, baseLockVersion: null}, 1, 3)).toBe(true);
        expect(canPendingSaveStillCommit(pending, 1, 1)).toBe(true);
        expect(canPendingSaveStillCommit(pending, 1, 2)).toBe(false);
        expect(canPendingSaveStillCommit({...pending, phase: 'confirmed'}, 1, 1)).toBe(false);
        expect(canPendingSaveStillCommit(undefined, 1, 1)).toBe(false);
    });

    it('checks the actual interrupted payload while ignoring normalized and server-managed properties', () => {
        const stored = {...original, notes: 'Current elsewhere', lockVersion: 2};
        const editorSpec = {...spec, normalize: (node: typeof stored) => ({...node, notes: stored.notes})};
        expect(pendingSaveCanChangeStored(pending, stored, editorSpec)).toBe(true);
        expect(pendingSaveCanChangeStored({...pending, attempt: {base: stored, mine: original}}, stored, editorSpec)).toBe(false);
        expect(pendingSaveCanChangeStored({...pending, phase: 'confirmed'}, stored, editorSpec)).toBe(false);
        expect(pendingSaveCanChangeStored(undefined, stored, editorSpec)).toBe(false);
    });

    it('preserves a dispatched write on server, proxy and response-body failures', () => {
        const error = {status: 503, message: 'Transport error', displayableToUser: true, details: {reason: API_ERROR_REASON_NETWORK_UNREACHABLE}};
        expect(isEditorSaveOutcomeUnknown(error)).toBe(true);
        expect(isEditorSaveOutcomeUnknown({...error, status: 504, details: {reason: API_ERROR_REASON_TIMEOUT}})).toBe(true);
        for (const status of [500, 502, 503, 504]) {
            expect(isEditorSaveOutcomeUnknown({...error, status, details: null})).toBe(true);
        }
        expect(isEditorSaveOutcomeUnknown(new SyntaxError('Invalid JSON'))).toBe(true);
        expect(isEditorSaveOutcomeUnknown(new TypeError('Could not read response body'))).toBe(true);
        expect(isEditorSaveOutcomeUnknown(new DOMException('Interrupted', 'AbortError'))).toBe(true);
        expect(isEditorSaveOutcomeUnknown(new DOMException('Timed out', 'TimeoutError'))).toBe(true);
        expect(isEditorSaveOutcomeUnknown({...error, status: 400})).toBe(false);
        expect(isEditorSaveOutcomeUnknown({...error, status: 409})).toBe(false);
        expect(isEditorSaveOutcomeUnknown(new Error('Validation failed'))).toBe(false);
    });
    it('keeps newer scalar edits while adopting unrelated changes and the confirmed lock version', () => {
        const current = {...submitted, name: 'C'};
        const confirmed = {name: 'Server choice', notes: 'Stored note', lockVersion: 3};
        expect(rebaseAfterSave(submitted, current, confirmed, spec)).toEqual({
            value: {...confirmed, name: 'C'},
        });
        expect(rebaseAfterSave(submitted, submitted, confirmed, spec)).toEqual({value: confirmed});
    });

    it('offers a subsequently edited but deleted element on the next save and after login', () => {
        const base = {title: 'Submitted', fields: [{id: 'x', type: 'text', label: 'B'}]};
        const current = {...base, fields: [{...base.fields[0], label: 'C'}]};
        const stored = {title: 'New stored title', fields: []};
        const formSpec: MergeSpec<typeof base> = {arrayKey: keyById};
        const rebased = rebaseAfterSave(base, current, stored, formSpec);
        expect(rebased).toEqual({
            value: {...stored, fields: current.fields},
            checkpoint: {phase: 'confirmed', submitted: base},
        });
        const conflict = preparePostSaveMerge(base, rebased.value, stored, formSpec, {});
        expect(conflict.conflicts.map(({id}) => id)).toEqual(['fields/[x]']);
        expect(preparePostSaveMerge(base, rebased.value, stored, formSpec, {'fields/[x]': 'theirs'}).value).toEqual(stored);
        const draft = {base: stored, mine: rebased.value, saveCheckpoint: rebased.checkpoint};
        expect(prepareEditorDraftRecovery(draft, stored, formSpec, {}).conflicts.map(({id}) => id)).toEqual(['post-save/fields/[x]']);
        expect(prepareEditorDraftRecovery(draft, stored, formSpec, {'post-save/fields/[x]': 'mine'}))
            .toMatchObject({conflicts: [], value: {...stored, fields: current.fields}});
    });

    it('restores a reversal if the interrupted PUT committed and skips it if it failed', () => {
        const draft = {base: original, mine: original, saveCheckpoint: pending};
        const committed = {...submitted, notes: 'Stored elsewhere', lockVersion: 3};
        expect(prepareEditorDraftRecovery(draft, committed, spec, {})).toMatchObject({
            changed: true, conflicts: [], value: {...committed, name: 'A'},
        });
        const failed = {...committed, name: 'A'};
        expect(prepareEditorDraftRecovery(draft, failed, spec, {})).toMatchObject({
            changed: false, conflicts: [], value: failed,
        });
        const other = {...committed, name: 'D'};
        expect(prepareEditorDraftRecovery(draft, other, spec, {}).conflicts).toMatchObject([
            {mine: 'A', theirs: 'D'},
        ]);
    });

    it('retains the submitted edits when a failed request is followed by another change', () => {
        const latest = {...submitted, notes: 'Typed later'};
        const draft = {base: original, mine: latest, saveCheckpoint: pending};
        expect(prepareEditorDraftRecovery(draft, original, spec, {})).toMatchObject({value: latest, changed: true, conflicts: []});
        expect(prepareEditorDraftRecovery(draft, submitted, spec, {})).toMatchObject({value: latest, changed: true, conflicts: []});
    });

    it('respects the selected retry payload without attributing newer inputs to that request', () => {
        const retryBase = {...submitted, name: 'Existing', lockVersion: 2};
        const retryPayload = {...retryBase, name: 'B'};
        const latest = {...submitted, name: 'A', notes: 'Typed later'};
        const draft = {base: original, mine: latest, saveCheckpoint: {...pending, attempt: {base: retryBase, mine: retryPayload}}};
        const confirmed = {...retryPayload, lockVersion: 3};
        expect(prepareEditorDraftRecovery(draft, confirmed, spec, {})).toMatchObject({
            value: {...latest, lockVersion: 3}, changed: true, conflicts: [],
        });
        const selectedStored = {...pending, attempt: {base: retryBase, mine: retryBase}};
        expect(prepareEditorDraftRecovery({...draft, mine: submitted, saveCheckpoint: selectedStored}, retryBase, spec, {})).toMatchObject({
            value: retryBase, changed: false, conflicts: [],
        });
    });

    it('restores reverted element order and field content alongside a newer server property', () => {
        const a = {id: 'a', value: 'A'};
        const b = {id: 'b', value: 'B'};
        const base = {fields: [a, b]};
        const sent = {fields: [b, {...a, value: 'Sent'}]};
        const mine = {fields: [a, b]};
        const stored = {fields: [{...b, value: 'Changed elsewhere'}, {...a, value: 'Sent'}]};
        expect(prepareEditorDraftRecovery({base, mine, saveCheckpoint: {phase: 'pending', submitted: sent}}, stored, {arrayKey: keyById}, {}))
            .toMatchObject({value: {fields: [a, stored.fields[0]]}, conflicts: [], changed: true});
    });
});
