import React, {type ReactNode, useCallback, useState} from 'react';
import {type MergeChoices, type MergeSpec, pathToId, threeWayMerge} from '../utils/three-way-merge';
import {deepEquals} from '../utils/equality-utils';
import {
    MergeConflictDialog,
    type DependentMergeConflicts,
    type MergeConflictDialogConflict,
    type MergeConflictDialogMode,
} from '../dialogs/merge-conflict-dialog/merge-conflict-dialog';

export type MergeOutcome<T> =
    | {status: 'apply'; value: T}
    | {status: 'discard'}
    | {status: 'cancel'}
    /** Only in the `restore` mode: the draft adds nothing to the stored state, so there is nothing to offer. */
    | {status: 'unchanged'};

interface MergeRequest<T> {
    /** The stored state the person's changes started from. */
    base: T;
    mine: T;
    /** The current stored state. */
    theirs: T;
    spec: MergeSpec<T>;
    mode: MergeConflictDialogMode;
    /** Dismisses a pending decision when its save or draft recovery is no longer current. */
    signal?: AbortSignal;
    /** Whether a newer state was stored since the changes were started, known from the lock version. */
    storedStateChanged: boolean;
}

export interface PreparedMerge<T> {
    value: T;
    changed: boolean;
    conflicts: MergeConflictDialogConflict[];
    /** Some choices determine the base for additional fields in the same dialog. */
    dependentConflicts?: DependentMergeConflicts;
}

/** Namespace decisions within a composite merge. Its outer prepareDependentMerge previews all layers. */
export function prepareScopedMerge<T>(
    layer: string, choices: MergeChoices, prepare: (choices: MergeChoices) => PreparedMerge<T>,
): PreparedMerge<T> {
    const prefix = layer + '/';
    const local = Object.fromEntries(Object.entries(choices)
        .filter(([id]) => id.startsWith(prefix))
        .map(([id, choice]) => [id.slice(prefix.length), choice]));
    const {value, changed, conflicts} = prepare(local);
    return {value, changed, conflicts: conflicts.map((conflict) => ({...conflict, id: prefix + conflict.id}))};
}

/** Preview every decision reachable through the current selection before the dialog is confirmed. */
export function prepareDependentMerge<T>(merge: (choices: MergeChoices) => PreparedMerge<T>, choices: MergeChoices): PreparedMerge<T> {
    const result = merge(choices);
    if (result.conflicts.length === 0) return result;
    return {
        ...result,
        dependentConflicts: (selected, defaultChoice = 'mine') => {
            const decisions = {...choices};
            const additional: MergeConflictDialogConflict[] = [];
            let pending = result.conflicts;
            while (pending.length > 0) {
                for (const {id} of pending) decisions[id] = selected[id] ?? defaultChoice;
                pending = merge(decisions).conflicts.filter(({id}) => !Object.hasOwn(decisions, id));
                additional.push(...pending);
            }
            return additional;
        },
    };
}

/** Presents the result of a three-way merge, including the spec's domain-specific labels. */
export function prepareMerge<T>(
    {base, mine, theirs, spec}: Pick<MergeRequest<T>, 'base' | 'mine' | 'theirs' | 'spec'>,
    choices: MergeChoices,
    interruptedWrite?: {value: T},
): PreparedMerge<T> {
    const {merged, conflicts} = threeWayMerge(base, mine, theirs, spec, choices, interruptedWrite);
    return {
        value: merged,
        changed: !deepEquals(merged, spec.normalize ? spec.normalize(theirs) : theirs, spec.atomicValue),
        conflicts: conflicts.map((conflict) => {
            const presentation = spec.describeConflict?.(conflict.path, {base, mine, theirs});
            return {
                id: conflict.id,
                label: spec.label?.(conflict.path, {base, mine, theirs}) ?? (pathToId(conflict.path) || 'Gesamter Inhalt'),
                group: presentation?.group,
                propertyLabel: conflict.elementPlacement ? 'Position im Formular' : presentation?.propertyLabel,
                elementPlacement: conflict.elementPlacement,
                mine: conflict.mine,
                theirs: conflict.theirs,
            };
        }),
    };
}

export interface PreparedMergeRequest<T> {
    mode: MergeConflictDialogMode;
    storedStateChanged: boolean;
    signal?: AbortSignal;
    merge: (choices: MergeChoices) => PreparedMerge<T>;
}

type DialogAnswer =
    | {status: 'apply'; choices: MergeChoices}
    | {status: 'discard'}
    | {status: 'cancel'};

interface DialogState {
    mode: MergeConflictDialogMode;
    storedStateChanged: boolean;
    conflicts: MergeConflictDialogConflict[];
    dependentConflicts?: PreparedMerge<unknown>['dependentConflicts'];
    answer: (answer: DialogAnswer) => void;
}

/**
 * Merges the person's changes with a newer stored state and asks for a decision where both changed the same value.
 * Used when an editor offers a draft from an earlier visit (`restore`) and when a save was rejected with a lock
 * version conflict (`save`). In the `save` mode a merge without conflicts is applied without asking, because the
 * person has already decided to save.
 */
export function useMergeConflictResolution() {
    const [dialogState, setDialogState] = useState<DialogState | null>(null);

    const resolvePrepared = useCallback(async <T, >({merge, mode, storedStateChanged, signal}: PreparedMergeRequest<T>): Promise<MergeOutcome<T>> => {
        let choices: MergeChoices = {};
        let asked = false;

        for (;;) {
            const result = merge(choices);

            if (mode === 'restore' && !asked && result.conflicts.length === 0 && !result.changed) {
                return {status: 'unchanged'};
            }

            if (result.conflicts.length === 0 && (mode === 'save' || asked)) {
                return {status: 'apply', value: result.value};
            }

            const answer = await new Promise<DialogAnswer>((finish) => {
                const cancel = () => answerDialog({status: 'cancel'});
                const answerDialog = (value: DialogAnswer) => {
                    signal?.removeEventListener('abort', cancel);
                    setDialogState((current) => current?.answer === answerDialog ? null : current);
                    finish(value);
                };
                if (signal?.aborted) {
                    cancel();
                    return;
                }
                signal?.addEventListener('abort', cancel);
                setDialogState({
                    mode,
                    storedStateChanged,
                    conflicts: result.conflicts,
                    dependentConflicts: result.dependentConflicts,
                    answer: answerDialog,
                });
            });

            if (answer.status !== 'apply') {
                return answer;
            }

            // The choices can make the merged object invalid, which turns it into a single conflict to decide again.
            choices = {...choices, ...answer.choices};
            asked = true;
        }
    }, []);

    const resolve = useCallback(<T, >(request: MergeRequest<T>): Promise<MergeOutcome<T>> =>
        resolvePrepared({...request, merge: (choices) => prepareMerge(request, choices)}), [resolvePrepared]);

    const dialog: ReactNode = dialogState == null ? null : (
        <MergeConflictDialog
            open={true}
            mode={dialogState.mode}
            storedStateChanged={dialogState.storedStateChanged}
            conflicts={dialogState.conflicts}
            dependentConflicts={dialogState.dependentConflicts}
            onApply={(choices) => dialogState.answer({status: 'apply', choices})}
            onDiscard={() => dialogState.answer({status: 'discard'})}
            onCancel={dialogState.mode === 'save' ? () => dialogState.answer({status: 'cancel'}) : undefined}
        />
    );

    return {resolve, resolvePrepared, dialog};
}
