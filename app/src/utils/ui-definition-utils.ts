import {type AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {deepEquals} from './equality-utils';
import {generateElementWithDefaultValues} from './generate-element-with-default-values';

function stripGeneratedElementIds(value: unknown): unknown {
    if (Array.isArray(value)) {
        return value.map(stripGeneratedElementIds);
    }

    if (value == null || typeof value !== 'object') {
        return value;
    }

    const source = value as Record<string, unknown>;
    const isElement = 'type' in source;

    return Object.entries(value)
        .filter(([key]) => key !== 'id' || !isElement)
        .reduce<Record<string, unknown>>((obj, [key, item]) => {
            obj[key] = stripGeneratedElementIds(item);
            return obj;
        }, {});
}

function isEmptyGeneratedDefault(value: AnyElement): boolean {
    const defaultValue = generateElementWithDefaultValues(value.type);

    return defaultValue != null &&
        deepEquals(stripGeneratedElementIds(value), stripGeneratedElementIds(defaultValue));
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
