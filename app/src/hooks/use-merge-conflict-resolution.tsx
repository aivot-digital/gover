import React, {type ReactNode, useCallback, useState} from 'react';
import {type MergeChoices, type MergeSpec, pathToId, threeWayMerge} from '../utils/three-way-merge';
import {deepEquals} from '../utils/equality-utils';
import {
    MergeConflictDialog,
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
    /** Whether a newer state was stored since the changes were started, known from the lock version. */
    storedStateChanged: boolean;
}

type DialogAnswer =
    | {status: 'apply'; choices: MergeChoices}
    | {status: 'discard'}
    | {status: 'cancel'};

interface DialogState {
    mode: MergeConflictDialogMode;
    storedStateChanged: boolean;
    conflicts: MergeConflictDialogConflict[];
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

    const resolve = useCallback(async <T, >(request: MergeRequest<T>): Promise<MergeOutcome<T>> => {
        const {base, mine, theirs, spec, mode, storedStateChanged} = request;
        let choices: MergeChoices = {};
        let asked = false;

        for (;;) {
            const result = threeWayMerge(base, mine, theirs, spec, choices);

            if (mode === 'restore' && !asked && result.conflicts.length === 0 && deepEquals(result.merged, threeWayMerge(theirs, theirs, theirs, spec).merged)) {
                return {status: 'unchanged'};
            }

            if (result.conflicts.length === 0 && (mode === 'save' || asked)) {
                return {status: 'apply', value: result.merged};
            }

            const answer = await new Promise<DialogAnswer>((answer) => {
                setDialogState({
                    mode,
                    storedStateChanged,
                    conflicts: result.conflicts.map((conflict) => ({
                        id: conflict.id,
                        label: spec.label?.(conflict.path, {base, mine, theirs}) ?? (pathToId(conflict.path) || 'Gesamter Inhalt'),
                        mine: conflict.mine,
                        theirs: conflict.theirs,
                    })),
                    answer,
                });
            });
            setDialogState(null);

            if (answer.status !== 'apply') {
                return answer;
            }

            // The choices can make the merged object invalid, which turns it into a single conflict to decide again.
            choices = {...choices, ...answer.choices};
            asked = true;
        }
    }, []);

    const dialog: ReactNode = dialogState == null ? null : (
        <MergeConflictDialog
            open={true}
            mode={dialogState.mode}
            storedStateChanged={dialogState.storedStateChanged}
            conflicts={dialogState.conflicts}
            onApply={(choices) => dialogState.answer({status: 'apply', choices})}
            onDiscard={() => dialogState.answer({status: 'discard'})}
            onCancel={dialogState.mode === 'save' ? () => dialogState.answer({status: 'cancel'}) : undefined}
        />
    );

    return {resolve, dialog};
}
