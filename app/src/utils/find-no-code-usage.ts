import {type AnyElement} from '../models/elements/any-element';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {type AnyElementWithChildren, isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {ElementVisibilityFunction} from '../models/elements/element-visibility-function';
import {ElementValidationFunction} from '../models/elements/element-validation-function';
import {ElementOverrideFunction} from '../models/elements/element-override-function';
import {ElementValueFunction} from '../models/elements/element-value-function';

export function findUsageOfChild(target: AnyElementWithChildren, current: AnyElement): [AnyElement, AnyElement[]][] {
    const found: [AnyElement, AnyElement[]][] = [];

    for (const child of target.children ?? []) {
        const usages = findNoCodeUsage(child, current);
        if (usages.length > 0) {
            found.push([
                child,
                usages,
            ]);
        }

        if (isAnyElementWithChildren(child)) {
            found.push(...findUsageOfChild(child, current));
        }
    }

    return found;
}

export function findNoCodeUsage(elementToFindUsageFor: AnyElement, currentlyInspectedElement: AnyElement): AnyElement[] {
    const foundReferences: AnyElement[] = [];

    const referencedInVisibilityFunc = testTargetIsReferencedInFunc(elementToFindUsageFor, currentlyInspectedElement.visibility);
    if (referencedInVisibilityFunc) {
        foundReferences.push(currentlyInspectedElement);
    } else if (isAnyInputElement(currentlyInspectedElement)) {
        if (testTargetIsReferencedInFunc(elementToFindUsageFor, currentlyInspectedElement.validation)) {
            foundReferences.push(currentlyInspectedElement);
        }
    }

    if (isAnyElementWithChildren(currentlyInspectedElement)) {
        for (const child of currentlyInspectedElement.children ?? []) {
            foundReferences.push(...findNoCodeUsage(elementToFindUsageFor, child));
        }
    }

    return foundReferences;
}


// region Utils

function testTargetIsReferencedInFunc(target: AnyElement, func?: ElementVisibilityFunction | ElementValidationFunction | ElementOverrideFunction | ElementValueFunction | undefined | null): boolean {
    if (func == null || func.referencedIds == null) {
        return false;
    }

    return func.referencedIds.includes(target.id);
}

// endregion
