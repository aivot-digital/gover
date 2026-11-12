import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../../models/element-data';
import type {FileUploadElement, FileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {FileUploadView} from './file-upload.view';
import {RemovableFileUploadAttachmentsProvider} from './removable-file-upload-attachments-context';

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

const earlierDocument = {name: 'antrag.pdf', size: 5, uri: 'process-instance-attachment:earlier'};
const ownUpload = {name: 'nachweis.pdf', size: 3, uri: 'process-instance-attachment:own'};
const newUpload = {name: 'foto.png', size: 2, uri: 'blob:new'};

describe('FileUploadView', () => {
    it('offers download and removal for the stored uploads of the task and protects other documents', () => {
        const setValue = vi.fn();
        renderView([earlierDocument, ownUpload, newUpload], setValue, new Set(['own']));

        expect(screen.getByRole('button', {name: 'antrag.pdf herunterladen'})).toBeEnabled();
        expect(screen.queryByRole('button', {name: 'antrag.pdf entfernen'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'nachweis.pdf herunterladen'})).toBeEnabled();
        expect(screen.queryByRole('button', {name: 'foto.png herunterladen'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'foto.png entfernen'})).toBeEnabled();

        fireEvent.click(screen.getByRole('button', {name: 'nachweis.pdf entfernen'}));

        expect(setValue).toHaveBeenCalledWith([earlierDocument, newUpload]);
    });

    it('protects stored uploads outside of task views', () => {
        renderView([ownUpload], vi.fn());

        expect(screen.getByRole('button', {name: 'nachweis.pdf herunterladen'})).toBeEnabled();
        expect(screen.queryByRole('button', {name: 'nachweis.pdf entfernen'})).not.toBeInTheDocument();
    });
});

function renderView(
    value: FileUploadElementItem[],
    setValue: (value: FileUploadElementItem[] | null) => void,
    removableAttachmentKeys?: ReadonlySet<string>,
) {
    const element: FileUploadElement = {
        ...generateElementWithDefaultValues(ElementType.FileUpload) as FileUploadElement,
        label: 'Nachweise',
        isMultifile: true,
    };
    const view = (
        <FileUploadView
            element={element}
            isBusy={false}
            isDeriving={false}
            value={value}
            setValue={setValue}
            onBlur={vi.fn()}
            authoredElementValues={{}}
            onAuthoredElementValuesChange={vi.fn()}
            derivedData={createDerivedRuntimeElementData()}
            onDerive={vi.fn()}
            onEvent={vi.fn()}
            onResetErrors={vi.fn()}
            suppressErrors={false}
            derivationTriggerIdQueue={[]}
        />
    );

    return render(removableAttachmentKeys == null
        ? view
        : <RemovableFileUploadAttachmentsProvider value={removableAttachmentKeys}>{view}</RemovableFileUploadAttachmentsProvider>);
}
