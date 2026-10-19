import type {AuthoredElementValues, DerivedRuntimeElementData} from '../../../models/element-data';
import type {AnyElement} from '../../../models/elements/any-element';
import type {FileUploadElementItem} from '../../../models/elements/form/input/file-upload-element';
import {collectTransientFileUploadItems} from '../../../utils/element-data-utils';

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
