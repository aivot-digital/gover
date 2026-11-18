import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {createDerivedRuntimeElementData, literalAuthoredValue} from '../../../models/element-data';
import type {AnyElement} from '../../../models/elements/any-element';
import {
    collectStoredTaskViewFileUploadItems,
    collectTaskViewFileUploads,
    haveTaskViewFileUploadItemsChanged,
    replaceStoredTaskViewFileUploadItems,
} from './task-view-file-uploads';

describe('collectTaskViewFileUploads', () => {
    const layout = {
        id: 'root',
        type: ElementType.GroupLayout,
        children: [
            {id: 'visibleUpload', type: ElementType.FileUpload},
            {id: 'hiddenUpload', type: ElementType.FileUpload},
        ],
    } as AnyElement;
    const visibleFile = {name: 'visible.pdf', size: 1, uri: 'blob:visible'};
    const hiddenFile = {name: 'hidden.pdf', size: 2, uri: 'blob:hidden'};
    const values = {
        visibleUpload: literalAuthoredValue([visibleFile]),
        hiddenUpload: literalAuthoredValue([hiddenFile]),
    };

    it('should not derive values without files to send', async () => {
        const derive = vi.fn();

        const files = await collectTaskViewFileUploads(layout, {
            visibleUpload: literalAuthoredValue([{name: 'stored.pdf', size: 3, uri: 'process-instance-attachment:stored'}]),
        }, derive);

        expect(files).toEqual([]);
        expect(derive).not.toHaveBeenCalled();
    });

    it('should derive the sent values and leave out the files of hidden upload fields', async () => {
        const derive = vi.fn().mockResolvedValue(createDerivedRuntimeElementData({
            elementStates: {
                visibleUpload: {visible: true},
                hiddenUpload: {visible: false},
            },
        }));

        const files = await collectTaskViewFileUploads(layout, values, derive);

        expect(derive).toHaveBeenCalledWith(values);
        expect(files).toEqual([visibleFile]);
    });

    it('should send all files when the derivation fails', async () => {
        const derive = vi.fn().mockRejectedValue(new Error('network'));

        const files = await collectTaskViewFileUploads(layout, values, derive);

        expect(files).toEqual([visibleFile, hiddenFile]);
    });
});

describe('stored task view file upload items', () => {
    const layout = {
        id: 'root',
        type: ElementType.GroupLayout,
        children: [
            {id: 'upload', type: ElementType.FileUpload},
            {
                id: 'rows',
                type: ElementType.ReplicatingContainer,
                children: [{id: 'rowUpload', type: ElementType.FileUpload}],
            },
        ],
    } as AnyElement;
    const keptFile = {name: 'kept.pdf', size: 1, uri: 'process-instance-attachment:kept'};
    const firstRowFile = {name: 'first.pdf', size: 2, uri: 'blob:first'};
    const secondRowFile = {name: 'second.pdf', size: 3, uri: 'blob:second'};
    const storedFirstRowFile = {name: 'first.pdf', size: 2, uri: 'process-instance-attachment:first'};
    const storedSecondRowFile = {name: 'second.pdf', size: 3, uri: 'process-instance-attachment:second'};
    const sentValues = {
        upload: literalAuthoredValue([keptFile]),
        rows: literalAuthoredValue([
            {id: 'row-1', values: {rowUpload: literalAuthoredValue([firstRowFile])}},
            {id: 'row-2', values: {rowUpload: literalAuthoredValue([secondRowFile])}},
        ]),
    };

    it('should pair the transient items of each row with the stored items at the same position', () => {
        const storedItems = collectStoredTaskViewFileUploadItems(layout, sentValues, {
            upload: literalAuthoredValue([keptFile]),
            rows: literalAuthoredValue([
                {id: 'row-1', values: {rowUpload: literalAuthoredValue([storedFirstRowFile])}},
                {id: 'row-2', values: {rowUpload: literalAuthoredValue([storedSecondRowFile])}},
            ]),
        });

        expect(storedItems).toEqual(new Map([
            [firstRowFile.uri, storedFirstRowFile],
            [secondRowFile.uri, storedSecondRowFile],
        ]));
    });

    it('should pair no items of an upload field whose transient items the backend discarded', () => {
        const storedItems = collectStoredTaskViewFileUploadItems(layout, sentValues, {
            upload: literalAuthoredValue([keptFile]),
            rows: literalAuthoredValue([
                {id: 'row-1', values: {}},
                {id: 'row-2', values: {rowUpload: literalAuthoredValue([storedSecondRowFile])}},
            ]),
        });

        expect(storedItems).toEqual(new Map([[secondRowFile.uri, storedSecondRowFile]]));
    });

    it('should replace transient items by URI and keep rows changed during the save', () => {
        const values = replaceStoredTaskViewFileUploadItems(layout, {
            upload: literalAuthoredValue([keptFile]),
            rows: literalAuthoredValue([
                {id: 'row-2', values: {rowUpload: literalAuthoredValue([secondRowFile])}},
                {id: 'row-3', values: {rowUpload: literalAuthoredValue([firstRowFile])}},
            ]),
        }, new Map([[secondRowFile.uri, storedSecondRowFile]]));

        expect(values).toEqual({
            upload: literalAuthoredValue([keptFile]),
            rows: literalAuthoredValue([
                {id: 'row-2', values: {rowUpload: literalAuthoredValue([storedSecondRowFile])}},
                {id: 'row-3', values: {rowUpload: literalAuthoredValue([firstRowFile])}},
            ]),
        });
    });

    it('should detect stored, removed and reordered file items but ignore other values', () => {
        const values = {
            upload: literalAuthoredValue([keptFile, storedFirstRowFile]),
            subject: literalAuthoredValue('Betreff'),
        };

        expect(haveTaskViewFileUploadItemsChanged(layout, values, {...values, subject: literalAuthoredValue('Neu')})).toBe(false);
        expect(haveTaskViewFileUploadItemsChanged(layout, values, {...values, upload: literalAuthoredValue([keptFile])})).toBe(true);
        expect(haveTaskViewFileUploadItemsChanged(layout, values, {
            ...values,
            upload: literalAuthoredValue([storedFirstRowFile, keptFile]),
        })).toBe(true);
        expect(haveTaskViewFileUploadItemsChanged(layout, {
            upload: literalAuthoredValue([keptFile, firstRowFile]),
        }, values)).toBe(true);
    });
});
