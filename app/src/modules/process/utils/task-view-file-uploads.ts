import type {AuthoredElementValues, DerivedRuntimeElementData} from '../../../models/element-data';
import type {AnyElement} from '../../../models/elements/any-element';
import {type FileUploadElementItem, isFileUploadElementItem} from '../../../models/elements/form/input/file-upload-element';
import {ElementType} from '../../../data/element-type/element-type';
import {collectTransientFileUploadItems, mapAuthoredElementValues} from '../../../utils/element-data-utils';

/**
 * Collects the files that a task view update sends for the authored values.
 *
 * The values are derived first only when they contain files to send, so updates without new files cost no extra
 * request. The files of hidden upload fields are then left out. If the derivation fails, all files are sent, because
 * the backend discards the files of upload fields it does not take but rejects an update that lacks a needed file.
 */
export async function collectTaskViewFileUploads(
    layout: AnyElement,
    values: AuthoredElementValues,
    derive: (values: AuthoredElementValues) => Promise<DerivedRuntimeElementData>,
): Promise<FileUploadElementItem[]> {
    const allFiles = collectTransientFileUploadItems(layout, values);
    if (allFiles.length === 0) {
        return allFiles;
    }

    let derivedData: DerivedRuntimeElementData;
    try {
        derivedData = await derive(values);
    } catch {
        return allFiles;
    }

    return collectTransientFileUploadItems(layout, values, derivedData);
}

/**
 * Pairs the transient file items of sent values with the stored items that the backend returned for them.
 *
 * The backend replaces the transient items of an upload field in place, so the items pair by field path and position.
 * Upload fields whose transient items the backend discarded have a different item count and pair no items.
 *
 * @returns the stored items by the `blob:` URI of their transient items
 */
export function collectStoredTaskViewFileUploadItems(
    layout: AnyElement,
    sentValues: AuthoredElementValues,
    storedValues: AuthoredElementValues,
): Map<string, FileUploadElementItem> {
    const storedItemsByPath = collectFileUploadItemsByPath(layout, storedValues);
    const storedItemsByUri = new Map<string, FileUploadElementItem>();

    for (const [path, sentItems] of collectFileUploadItemsByPath(layout, sentValues)) {
        const storedItems = storedItemsByPath.get(path);
        if (storedItems == null || storedItems.length !== sentItems.length) {
            continue;
        }

        sentItems.forEach((sentItem, index) => {
            const storedItem = storedItems[index];
            if (isTransientFileUploadItem(sentItem) && isFileUploadElementItem(storedItem) && !isTransientFileUploadItem(storedItem)) {
                storedItemsByUri.set(sentItem.uri, storedItem);
            }
        });
    }

    return storedItemsByUri;
}

/**
 * Replaces transient file items with their stored items, so later updates reference the stored attachments instead of
 * uploading the files again.
 */
export function replaceStoredTaskViewFileUploadItems(
    layout: AnyElement,
    values: AuthoredElementValues,
    storedItemsByUri: Map<string, FileUploadElementItem>,
): AuthoredElementValues {
    if (storedItemsByUri.size === 0) {
        return values;
    }

    return mapAuthoredElementValues(layout, values, (element, value) => (
        element.type === ElementType.FileUpload && Array.isArray(value)
            ? value.map((item) => (isFileUploadElementItem(item) ? storedItemsByUri.get(item.uri) ?? item : item))
            : value
    ));
}

function collectFileUploadItemsByPath(layout: AnyElement, values: AuthoredElementValues): Map<string, unknown[]> {
    const itemsByPath = new Map<string, unknown[]>();

    // Used as a walk only, because it passes the path with the row indices.
    mapAuthoredElementValues(layout, values, (element, value, path) => {
        if (element.type === ElementType.FileUpload && Array.isArray(value)) {
            const pathKey = [...path.map((part) => (typeof part === 'number' ? part : part.id)), element.id].join('/');
            itemsByPath.set(pathKey, value);
        }
        return value;
    });

    return itemsByPath;
}

function isTransientFileUploadItem(item: unknown): item is FileUploadElementItem {
    return isFileUploadElementItem(item) && item.uri.startsWith('blob:');
}
