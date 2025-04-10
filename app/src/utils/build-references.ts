import {RootElement} from '../models/elements/root-element';
import {FunctionType, hasElementFunctionType} from './function-status-utils';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {flattenElementsWithParents} from './flatten-elements';
import {StepElement} from '../models/elements/steps/step-element';
import {AnyElement} from '../models/elements/any-element';
import {ElementType} from '../data/element-type/element-type';

export interface Reference {
    source: AnyElement;
    target: AnyElement;
    sourceStep: StepElement;
    targetStep: StepElement;
    functionType: FunctionType;
    isSameStep: boolean;
    sourceIsStep: boolean;
}

export function collectReferences(root: RootElement): Reference[] {
    const allElements = flattenElementsWithParents(root, [], false);
    const references: Reference[] = [];

    const addReference = (sourceElement: AnyElement, parents: AnyElement[], targetId: string, type: FunctionType) => {
        const target = allElements.find((element) => element.element.id === targetId);
        if (target == null) {
            return;
        }
        const targetElement = target.element;
        const targetParents = target.parents;

        let sourceStep = parents.find((element) => element.type === ElementType.Step) as StepElement | undefined;
        let targetStep = targetParents.find((element) => element.type === ElementType.Step) as StepElement | undefined;

        if (sourceStep == null && sourceElement.type === ElementType.Step) {
            sourceStep = sourceElement as StepElement;
        }

        if (targetStep == null && targetElement.type === ElementType.Step) {
            targetStep = targetElement as StepElement;
        }

        if (sourceStep == null || targetStep == null) {
            console.warn('Could not find step for reference', sourceElement, targetElement);
            return;
        }

        references.push({
            source: sourceElement,
            target: targetElement,
            sourceStep: sourceStep,
            targetStep: targetStep,
            functionType: type,
            isSameStep: sourceStep === targetStep,
            sourceIsStep: sourceElement.type === ElementType.Step,
        });
    };

    for (const {element, parents} of allElements) {
        if (hasElementFunctionType(element, FunctionType.VISIBILITY)) {
            for (const id of (element.visibilityReferencedIds ?? [])) {
                addReference(element, parents, id, FunctionType.VISIBILITY);
            }
        }

        if (hasElementFunctionType(element, FunctionType.OVERRIDE)) {
            for (const id of (element.overrideReferencedIds ?? [])) {
                addReference(element, parents, id, FunctionType.OVERRIDE);
            }
        }

        if (isAnyInputElement(element)) {
            if (hasElementFunctionType(element, FunctionType.VALUE)) {
                for (const id of (element.valueReferencedIds ?? [])) {
                    addReference(element, parents, id, FunctionType.VALUE);
                }
            }

            if (hasElementFunctionType(element, FunctionType.VALIDATION)) {
                for (const id of (element.validationReferencedIds ?? [])) {
                    addReference(element, parents, id, FunctionType.VALIDATION);
                }
            }
        }
    }

    const uniqueReferences: Reference[] = [];
    for (const reference of references) {
        if (!uniqueReferences.some((r) => r.source === reference.source && r.target === reference.target && r.functionType === reference.functionType)) {
            uniqueReferences.push(reference);
        }
    }

    return uniqueReferences
        .sort((a, b) => a.source.id.localeCompare(b.source.id));
}
