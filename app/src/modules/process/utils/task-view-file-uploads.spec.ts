import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {createDerivedRuntimeElementData, literalAuthoredValue} from '../../../models/element-data';
import type {AnyElement} from '../../../models/elements/any-element';
import {collectTaskViewFileUploads} from './task-view-file-uploads';

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
