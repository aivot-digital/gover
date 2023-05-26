import {AnyElement} from "../models/elements/any-element";
import {Function} from "../models/functions/function";
import {isAnyInputElement} from "../models/elements/form/input/any-input-element";
import {isAnyElementWithChildren} from "../models/elements/any-element-with-children";
import {ConditionSet} from "../models/functions/conditions/condition-set";

export function findNoCodeUsage(target: AnyElement, current: AnyElement): AnyElement[] {
    const foundReferences: AnyElement[] = [];

    let alreadyFound = false;
    if (findNoCodeUsageInFunc(target, current.isVisible)) {
        foundReferences.push(current);
        alreadyFound=true;
    }

    if (!alreadyFound && isAnyInputElement(current)) {
        if (findNoCodeUsageInFunc(target, current.validate)) {
            foundReferences.push(current);
        }
    }

    if (isAnyElementWithChildren(current)) {
        for (const child of current.children) {
            foundReferences.push(...findNoCodeUsage(target, child));
        }
    }

    return foundReferences;
}

function findNoCodeUsageInFunc(target: AnyElement, func?: Function): boolean {
    if (func == null || func.conditionSet == null) {
        return false;
    }

    return findNoCodeUsageInConditionSet(target, func.conditionSet);
}

function findNoCodeUsageInConditionSet(target: AnyElement, conditionSet: ConditionSet): boolean {
    if (conditionSet.conditions != null) {
        for (const cond of conditionSet.conditions) {
            if (cond.reference === target.id || cond.target === target.id) {
                return true;
            }
        }
    }

    if (conditionSet.conditionsSets != null) {
        for (const condSet of conditionSet.conditionsSets) {
            if (findNoCodeUsageInConditionSet(target, condSet)) {
                return true;
            }
        }
    }

    return false;
}