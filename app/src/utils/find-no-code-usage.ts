import {type AnyElement} from '../models/elements/any-element';
import {type Function as AppFunction} from '../models/functions/function';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {type AnyElementWithChildren, isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {type ConditionSet} from '../models/functions/conditions/condition-set';

export function findNoCodeUsageOfChildren(target: AnyElementWithChildren, current: AnyElement): [AnyElement, AnyElement[]][] {
    const found: [AnyElement, AnyElement[]][] = [];
    for (const child of target.children) {
        const usages = findNoCodeUsage(child, current);
        if (usages.length > 0) {
            found.push([
                child,
                usages,
            ]);
        }

        if (isAnyElementWithChildren(child)) {
            found.push(...findNoCodeUsageOfChildren(child, current));
        }
    }

    return found;
}

export function findNoCodeUsage(elementToFindUsageFor: AnyElement, currentlyInspectedElement: AnyElement): AnyElement[] {
    const foundReferences: AnyElement[] = [];

    const referencedInVisibilityFunc = testTargetIsReferencedInFunc(elementToFindUsageFor, currentlyInspectedElement.isVisible);
    if (referencedInVisibilityFunc) {
        foundReferences.push(currentlyInspectedElement);
    } else if (isAnyInputElement(currentlyInspectedElement)) {
        if (testTargetIsReferencedInFunc(elementToFindUsageFor, currentlyInspectedElement.validate)) {
            foundReferences.push(currentlyInspectedElement);
        }
    }

    if (isAnyElementWithChildren(currentlyInspectedElement)) {
        for (const child of currentlyInspectedElement.children) {
            foundReferences.push(...findNoCodeUsage(elementToFindUsageFor, child));
        }
    }

    return foundReferences;
}


// region Utils

function testTargetIsReferencedInFunc(target: AnyElement, func?: AppFunction): boolean {
    if (func == null || func.conditionSet == null) {
        return false;
    }

    return testTargetIsReferencedInConditionSet(target, func.conditionSet);
}

function testTargetIsReferencedInConditionSet(target: AnyElement, conditionSet: ConditionSet): boolean {
    if (conditionSet.conditions != null) {
        for (const cond of conditionSet.conditions) {
            if (cond.reference === target.id || cond.target === target.id) {
                return true;
            }
        }
    }

    if (conditionSet.conditionsSets != null) {
        for (const condSet of conditionSet.conditionsSets) {
            if (testTargetIsReferencedInConditionSet(target, condSet)) {
                return true;
            }
        }
    }

    return false;
}

// endregion
