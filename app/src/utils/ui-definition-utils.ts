import {type AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {deepEquals} from './equality-utils';
import {flattenElements} from './flatten-elements';
import {generateElementWithDefaultValues} from './generate-element-with-default-values';
import {ElementType} from '../data/element-type/element-type';

interface ReferencingFunction {
    referencedIds: string[] | null | undefined;
}

function normalizeForDefaultComparison(value: unknown): unknown {
    if (Array.isArray(value)) {
        return value.map(normalizeForDefaultComparison);
    }

    if (value == null || typeof value !== 'object') {
        return value;
    }

    const source = value as Record<string, unknown>;
    const isElement =
        typeof source.id === 'string' &&
        typeof source.type === 'number' &&
        ElementType[source.type] != null;

    return Object.entries(value)
        .filter(([key, item]) => item !== undefined && (key !== 'id' || !isElement))
        .reduce<Record<string, unknown>>((obj, [key, item]) => {
            obj[key] = normalizeForDefaultComparison(item);
            return obj;
        }, {});
}

function isEmptyGeneratedDefault(value: AnyElement): boolean {
    const defaultValue = generateElementWithDefaultValues(value.type);

    return defaultValue != null &&
        deepEquals(normalizeForDefaultComparison(value), normalizeForDefaultComparison(defaultValue));
}

export function normalizeUiDefinitionForStorage<T extends AnyElement>(
    value: T | null | undefined
): T | null {
    if (value == null) {
        return null;
    }

    if (
        isAnyElementWithChildren(value) &&
        (value.children?.length ?? 0) === 0 &&
        isEmptyGeneratedDefault(value)
    ) {
        return null;
    }

    return value;
}

/**
 * Applies the referenced IDs recalculated by the backend to the authored draft.
 *
 * The backend returns a re-serialized copy of the tree with additional null properties. Adopting that copy would
 * change the authored definition, for example an untouched default would no longer normalize to null. Only the
 * functions recalculated in the backend's BaseElement and BaseInputElement are updated, and no keys are added.
 */
export function mergeRecalculatedReferencedIds<T extends AnyElement>(draft: T, recalculated: AnyElement): T {
    const recalculatedElements = new Map(flattenElements(recalculated).map((element) => [element.id, element]));
    return mergeElementReferencedIds(draft, recalculatedElements);
}

function mergeElementReferencedIds<T extends AnyElement>(element: T, recalculatedElements: Map<string, AnyElement>): T {
    const merged: T = {...element};
    const recalculated = recalculatedElements.get(element.id);

    if (recalculated != null) {
        if (merged.visibility != null) {
            merged.visibility = withReferencedIdsOf(merged.visibility, recalculated.visibility);
        }
        if (merged.override != null) {
            merged.override = withReferencedIdsOf(merged.override, recalculated.override);
        }
        if (isAnyInputElement(merged) && isAnyInputElement(recalculated)) {
            if (merged.value != null) {
                merged.value = withReferencedIdsOf(merged.value, recalculated.value);
            }
            if (merged.validation != null) {
                merged.validation = withReferencedIdsOf(merged.validation, recalculated.validation);
            }
        }
    }

    if (isAnyElementWithChildren(merged) && merged.children != null) {
        merged.children = merged.children.map((child) => mergeElementReferencedIds(child, recalculatedElements));
    }

    return merged;
}

function withReferencedIdsOf<T extends ReferencingFunction>(authored: T, recalculated: ReferencingFunction | null | undefined): T {
    return recalculated == null ? authored : {...authored, referencedIds: recalculated.referencedIds};
}
