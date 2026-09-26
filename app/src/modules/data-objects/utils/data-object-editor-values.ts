import type {AnyElement} from '../../../models/elements/any-element';
import {isAnyElementWithChildren} from '../../../models/elements/any-element-with-children';
import {isReplicatingContainerLayout} from '../../../models/elements/form/layout/replicating-container-layout';
import {type AuthoredElementValues, toLiteralAuthoredElementValues, toLiteralElementValues} from '../../../models/element-data';
import type {DataObjectItem} from '../models/data-object-item';

export type DataObjectItemDraft = Omit<DataObjectItem, 'data'> & {data: AuthoredElementValues};

export function toDataObjectItemDraft(item: DataObjectItem, schema: AnyElement): DataObjectItemDraft {
    return {...item, data: wrapValues([schema], item.data)};
}

export function fromDataObjectItemDraft(item: DataObjectItemDraft, schema: AnyElement): DataObjectItem {
    return {...item, data: unwrapValues([schema], item.data)};
}

function wrapValues(elements: AnyElement[], values: Record<string, unknown>): AuthoredElementValues {
    return toLiteralAuthoredElementValues(mapReplicatingRows(elements, values, wrapValues));
}

function unwrapValues(elements: AnyElement[], values: AuthoredElementValues): Record<string, unknown> {
    // Reject dynamic modes rather than silently sending null or a stale frontend-derived result.
    return mapReplicatingRows(elements, toLiteralElementValues(values), (children, rowValues) => (
        unwrapValues(children, rowValues as AuthoredElementValues)
    ));
}

/**
 * Only schema-declared containers contain element-value maps. Ordinary object/list payloads must remain opaque,
 * even when they happen to contain properties named "type", "value", "id", or "values".
 */
function mapReplicatingRows(
    elements: AnyElement[],
    values: Record<string, unknown>,
    mapValues: (elements: AnyElement[], values: Record<string, unknown>) => Record<string, unknown>,
): Record<string, unknown> {
    const result = {...values};
    for (const element of elements) {
        if (isReplicatingContainerLayout(element)) {
            const rows = values[element.id];
            if (Array.isArray(rows)) {
                result[element.id] = rows.map((row: unknown) => {
                    if (row == null) {
                        return row;
                    }
                    if (typeof row !== 'object' || Array.isArray(row)) {
                        throw new Error('A data-object container row must be an object.');
                    }
                    const record = row as Record<string, unknown>;
                    const rowValues = Object.hasOwn(record, 'values') ? record.values : record;
                    if (rowValues != null && (typeof rowValues !== 'object' || Array.isArray(rowValues))) {
                        throw new Error('Data-object container values must be an object.');
                    }
                    return {
                        ...(Object.hasOwn(record, 'values') ? record : {}),
                        values: mapValues(element.children ?? [], (rowValues ?? {}) as Record<string, unknown>),
                    };
                });
            }
        } else if (isAnyElementWithChildren(element)) {
            Object.assign(result, mapReplicatingRows(element.children ?? [], result, mapValues));
        }
    }
    return result;
}
