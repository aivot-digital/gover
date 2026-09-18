import {type AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {deepEquals} from './equality-utils';
import {generateElementWithDefaultValues} from './generate-element-with-default-values';
import {ElementType} from '../data/element-type/element-type';

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
