import {isNoCodeNodeDataReference} from '../models/functions/no-code-expression';
import {InputMode, isAuthoredInputValue} from '../models/input-mode';
import {deepEquals} from './equality-utils';

function variableReferenceComparison(value: unknown): object | undefined {
    if (isAuthoredInputValue(value) && value.type === InputMode.Variable) {
        const {reference} = value;
        if (reference == null || typeof reference !== 'object') return value;
        // Older drafts and newly picked variables may omit the defaults returned by the backend.
        return {
            ...value,
            reference: {...reference, nodeDataKey: reference.nodeDataKey ?? null, markdown: reference.markdown ?? false},
        };
    }
    if (isNoCodeNodeDataReference(value)) {
        return {...value, nodeDataKey: value.nodeDataKey ?? null, path: value.path ?? null};
    }
}

export const variableReferenceMergeRules = {atomicValue: variableReferenceComparison};

export function editorValuesEqual(left: unknown, right: unknown): boolean {
    return deepEquals(left, right, variableReferenceComparison);
}
