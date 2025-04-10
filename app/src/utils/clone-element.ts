import {type AnyElement} from '../models/elements/any-element';
import {generateElementIdForType} from './id-utils';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {type ConditionSet} from '../models/functions/conditions/condition-set';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {generateComponentTitle} from './generate-component-title';
import {ElementType} from '../data/element-type/element-type';

export function cloneElement<T extends AnyElement>(element: T, skipSuffix?: boolean): T {
    const {
        clone,
        idMap,
    } = deepCloneElement(element, skipSuffix);
    return fixNoCodeReferences(clone, idMap);
}

type IdMap = Record<string, string>;

function deepCloneElement<T extends AnyElement>(element: T, skipSuffix?: boolean): {clone: T, idMap: IdMap} {
    const newId = generateElementIdForType(element.type);

    const idMap = {
        [element.id]: newId,
    };

    const clone: T = JSON.parse(JSON.stringify(element));
    clone.id = newId;
    clone.name = generateComponentTitle(element) + (skipSuffix === true ? '' : ' (Kopie)');

    if (isAnyElementWithChildren(clone)) {
        const clonedChildren = [];

        const isStoreElement = clone.type === ElementType.Container && clone.storeLink != null;

        for (const child of clone.children) {
            const res = deepCloneElement(child, skipSuffix || isStoreElement);
            for (const key of Object.keys(res.idMap)) {
                idMap[key] = res.idMap[key];
            }
            clonedChildren.push(res.clone);
        }

        // @ts-expect-error
        clone.children = clonedChildren;
    }

    return {
        clone,
        idMap,
    };
}

function fixNoCodeReferences<T extends AnyElement>(element: T, idMap: IdMap): T {
    let fixedElement = {
        ...element,
    };

    if (fixedElement.isVisible != null && fixedElement.isVisible.conditionSet != null) {
        fixedElement = {
            ...fixedElement,
            isVisible: {
                ...fixedElement.isVisible,
                conditionSet: fixConditionSetReferences(fixedElement.isVisible.conditionSet, idMap),
            },
        };
    }

    if (isAnyInputElement(fixedElement)) {
        if (fixedElement.validate != null && fixedElement.validate.conditionSet != null) {
            fixedElement = {
                ...fixedElement,
                validate: {
                    ...fixedElement.validate,
                    conditionSet: fixConditionSetReferences(fixedElement.validate.conditionSet, idMap),
                },
            };
        }
    }

    if (isAnyElementWithChildren(fixedElement)) {
        fixedElement = {
            ...fixedElement,
            children: fixedElement.children.map((c) => fixNoCodeReferences(c, idMap)),
        };
    }

    return fixedElement;
}

function fixConditionSetReferences(cs: ConditionSet, idMap: IdMap): ConditionSet {
    return {
        ...cs,
        conditions: cs.conditions != null ?
            cs.conditions.map((c) => ({
                ...c,
                reference: (c.reference != null && idMap[c.reference] != null) ? idMap[c.reference] : c.reference,
                target: (c.target != null && idMap[c.target]) ? idMap[c.target] : c.target,
            })) :
            undefined,
        conditionsSets: cs.conditionsSets != null ? cs.conditionsSets.map((c) => fixConditionSetReferences(c, idMap)) : undefined,
    };
}
