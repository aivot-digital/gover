import {type AnyElement} from '../models/elements/any-element';
import {generateElementIdForType} from './id-utils';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {type ConditionSet} from '../models/functions/conditions/condition-set';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {generateComponentTitle} from './generate-component-title';
import {ElementType} from '../data/element-type/element-type';
import {isNoCodeExpression, isNoCodeReference, NoCodeOperand} from '../models/functions/no-code-expression';

export function cloneElement<T extends AnyElement>(element: T, skipSuffix?: boolean): T {
    const {
        clone,
        idMap,
    } = deepCloneElement(element, skipSuffix);
    return fixNoCodeReferences(clone, idMap);
}

type IdMap = Record<string, string>;

function deepCloneElement<T extends AnyElement>(element: T, skipSuffix?: boolean): { clone: T, idMap: IdMap } {
    const newId = generateElementIdForType(element.type);

    const idMap = {
        [element.id]: newId,
    };

    const clone: T = JSON.parse(JSON.stringify(element));
    clone.id = newId;
    clone.name = generateComponentTitle(element) + (skipSuffix === true ? '' : ' (Kopie)');

    if (isAnyElementWithChildren(clone)) {
        const clonedChildren = [];

        const isStoreElement = clone.type === ElementType.GroupLayout && clone.storeLink != null;

        for (const child of clone.children ?? []) {
            const res = deepCloneElement(child, skipSuffix || isStoreElement);
            for (const key of Object.keys(res.idMap)) {
                idMap[key] = res.idMap[key];
            }
            clonedChildren.push(res.clone);
        }

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

    if (fixedElement.visibility != null) {
        if (fixedElement.visibility.conditionSet != null) {
            fixedElement = {
                ...fixedElement,
                visibility: {
                    ...fixedElement.visibility,
                    conditionSet: fixConditionSetReferences(fixedElement.visibility.conditionSet, idMap),
                },
            };
        } else if (fixedElement.visibility.noCode != null) {
            fixedElement = {
                ...fixedElement,
                visibility: {
                    ...fixedElement.visibility,
                    noCode: fixNoCodeOperandReferences(fixedElement.visibility.noCode, idMap),
                },
            };
        }
    }


    if (isAnyInputElement(fixedElement)) {
        if (fixedElement.validation != null) {
            if (fixedElement.validation.conditionSet != null) {
                fixedElement = {
                    ...fixedElement,
                    validation: {
                        ...fixedElement.validation,
                        conditionSet: fixConditionSetReferences(fixedElement.validation.conditionSet, idMap),
                    },
                };
            } else if (fixedElement.validation.noCodeList != null) {
                fixedElement = {
                    ...fixedElement,
                    validation: {
                        ...fixedElement.validation,
                        noCodeList: fixedElement
                            .validation
                            .noCodeList
                            .map(i => i.noCode != null ? fixNoCodeOperandReferences(i.noCode, idMap) : null),
                    },
                };
            }
        }
    }

    if (isAnyInputElement(fixedElement)) {
        if (fixedElement.value != null && fixedElement.value.noCode != null) {
            fixedElement = {
                ...fixedElement,
                value: {
                    ...fixedElement.visibility,
                    noCode: fixNoCodeOperandReferences(fixedElement.value.noCode, idMap),
                },
            };
        }
    }

    if (isAnyElementWithChildren(fixedElement)) {
        fixedElement = {
            ...fixedElement,
            children: (fixedElement.children ?? []).map((c) => fixNoCodeReferences(c, idMap)),
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

function fixNoCodeOperandReferences(no: NoCodeOperand, idMap: IdMap): NoCodeOperand {
    if (isNoCodeReference(no)) {
        return {
            ...no,
            elementId: (no.elementId != null && idMap[no.elementId] != null) ? idMap[no.elementId] : no.elementId,
        };
    }

    if (isNoCodeExpression(no)) {
        return {
            ...no,
            operands: no.operands != null
                ? no.operands.map((o) => o != null ? fixNoCodeOperandReferences(o, idMap) : o)
                : undefined,
        };
    }

    return no;
}