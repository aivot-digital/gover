import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {ProcessAttachmentDisplayView} from './process-attachment-display-view';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import {ProcessTaskViewAttachmentProvider} from '../modules/process/pages/details/process-task-view-attachment-context';
import type {BaseViewProps} from './base-view';
import type {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';
import type {ProcessInstanceAttachmentEntity} from '../modules/process/entities/process-instance-attachment-entity';
import type {ProcessInstanceAttachmentSetEntity} from '../modules/process/entities/process-instance-attachment-set-entity';

describe('ProcessAttachmentDisplayView', () => {
    it('should render a configuration prompt in the editor', () => {
        render(
            <ProcessAttachmentDisplayView
                {...createBaseProps()}
            />,
        );

        expect(screen.getByText('Konfigurieren Sie einen Anlagensatz-Schlüssel, um Vorgangsanhänge anzuzeigen.')).toBeInTheDocument();
    });

    it('should render an editor placeholder without task attachment context', () => {
        render(
            <ProcessAttachmentDisplayView
                {...createBaseProps({
                    attachmentSetKey: 'case_documents',
                    label: 'Fallunterlagen',
                    hint: 'Bitte prüfen Sie den Anhang sorgfältig.',
                })}
            />,
        );

        expect(screen.getByText('Fallunterlagen')).toBeInTheDocument();
        expect(screen.getByText('Bitte prüfen Sie den Anhang sorgfältig.')).toBeInTheDocument();
        expect(screen.getByText('Dies ist eine Vorschau. Anhänge können im Modellierungsmodus nicht angesehen oder heruntergeladen werden.')).toBeInTheDocument();
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('should render attachments from all matching attachment sets', () => {
        renderWithAttachmentContext(
            <ProcessAttachmentDisplayView
                {...createBaseProps({
                    attachmentSetKey: 'case_documents',
                })}
            />,
            {
                attachmentSets: [
                    createAttachmentSet(1, 'case_documents'),
                    createAttachmentSet(2, 'other_documents'),
                    createAttachmentSet(3, 'case_documents'),
                ],
                attachments: [
                    createAttachment('1', 'evidence.pdf', 1),
                    createAttachment('2', 'other.pdf', 2),
                    createAttachment('3', 'invoice.pdf', 3),
                ],
            },
        );

        expect(screen.getByText('evidence.pdf')).toBeInTheDocument();
        expect(screen.getByText('invoice.pdf')).toBeInTheDocument();
        expect(screen.queryByText('other.pdf')).not.toBeInTheDocument();
    });

    it('should trigger the provided download handler for matching attachments', () => {
        const downloadAttachment = jest.fn().mockResolvedValue(undefined);

        renderWithAttachmentContext(
            <ProcessAttachmentDisplayView
                {...createBaseProps({
                    attachmentSetKey: 'case_documents',
                })}
            />,
            {
                downloadAttachment,
                attachmentSets: [createAttachmentSet(1, 'case_documents')],
                attachments: [createAttachment('1', 'evidence.pdf', 1)],
            },
        );

        fireEvent.click(screen.getByRole('button', {name: 'evidence.pdf herunterladen'}));

        expect(downloadAttachment).toHaveBeenCalledWith(expect.objectContaining({
            key: '1',
            fileName: 'evidence.pdf',
        }));
    });

    it('should trigger the provided view handler as the primary action', () => {
        const viewAttachment = jest.fn().mockResolvedValue(undefined);
        const downloadAttachment = jest.fn().mockResolvedValue(undefined);

        renderWithAttachmentContext(
            <ProcessAttachmentDisplayView
                {...createBaseProps({
                    attachmentSetKey: 'case_documents',
                })}
            />,
            {
                viewAttachment,
                downloadAttachment,
                attachmentSets: [createAttachmentSet(1, 'case_documents')],
                attachments: [createAttachment('1', 'evidence.pdf', 1)],
            },
        );

        fireEvent.click(screen.getByRole('button', {name: 'evidence.pdf ansehen'}));

        expect(viewAttachment).toHaveBeenCalledWith(expect.objectContaining({
            key: '1',
            fileName: 'evidence.pdf',
        }));
        expect(downloadAttachment).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', {name: 'evidence.pdf in neuem Tab ansehen'}));

        expect(viewAttachment).toHaveBeenCalledTimes(2);
        expect(downloadAttachment).not.toHaveBeenCalled();
    });

    it('should render an explicit empty state when no attachment set matches', () => {
        renderWithAttachmentContext(
            <ProcessAttachmentDisplayView
                {...createBaseProps({
                    attachmentSetKey: 'case_documents',
                })}
            />,
            {
                attachmentSets: [createAttachmentSet(2, 'other_documents')],
                attachments: [createAttachment('1', 'other.pdf', 2)],
            },
        );

        expect(screen.getByText('Für den konfigurierten Anlagensatz-Schlüssel wurde kein Anlagensatz gefunden.')).toBeInTheDocument();
    });

    it('should render an explicit empty state when the attachment set has no attachments', () => {
        renderWithAttachmentContext(
            <ProcessAttachmentDisplayView
                {...createBaseProps({
                    attachmentSetKey: 'case_documents',
                })}
            />,
            {
                attachmentSets: [createAttachmentSet(1, 'case_documents')],
                attachments: [],
            },
        );

        expect(screen.getByText('Der konfigurierte Anlagensatz enthält keine Anhänge.')).toBeInTheDocument();
    });
});

function renderWithAttachmentContext(
    children: React.ReactElement,
    overrides?: Partial<{
        attachments: ProcessInstanceAttachmentEntity[];
        attachmentSets: ProcessInstanceAttachmentSetEntity[];
        isLoadingAttachments: boolean;
        viewAttachment: jest.Mock;
        downloadAttachment: jest.Mock;
    }>,
) {
    return render(
        <ProcessTaskViewAttachmentProvider
            value={{
                attachments: overrides?.attachments ?? [],
                attachmentSets: overrides?.attachmentSets ?? [],
                isLoadingAttachments: overrides?.isLoadingAttachments ?? false,
                viewAttachment: overrides?.viewAttachment ?? jest.fn(),
                downloadAttachment: overrides?.downloadAttachment ?? jest.fn(),
            }}
        >
            {children}
        </ProcessTaskViewAttachmentProvider>,
    );
}

function createBaseProps(
    overrides?: Partial<ProcessAttachmentDisplayElement>,
): BaseViewProps<ProcessAttachmentDisplayElement, void> {
    return {
        element: {
            id: 'pa_test',
            type: ElementType.ProcessAttachmentDisplay,
            weight: 12,
            attachmentSetKey: undefined,
            label: undefined,
            hint: undefined,
            metadata: undefined,
            name: undefined,
            override: undefined,
            testProtocolSet: undefined,
            visibility: undefined,
            ...overrides,
        },
        isBusy: false,
        isDeriving: false,
        value: undefined,
        setValue: jest.fn(),
        onBlur: jest.fn(),
        errors: undefined,
        errorDetails: undefined,
        authoredElementValues: {},
        onAuthoredElementValuesChange: jest.fn(),
        onElementBlur: jest.fn(),
        derivedData: createDerivedRuntimeElementData(),
        onDerive: jest.fn(),
        onEvent: jest.fn(),
        onResetErrors: jest.fn(),
        suppressErrors: false,
        derivationTriggerIdQueue: [],
    };
}

function createAttachment(key: string, fileName: string, attachmentSetId: number): ProcessInstanceAttachmentEntity {
    return {
        key,
        fileName,
        attachmentSetId,
        processInstanceId: 42,
        processInstanceTaskId: null,
        storageProviderId: 7,
        storagePathFromRoot: '/proc-1/test/attachments/' + key,
        uploadedByUserId: 'user-1',
    };
}

function createAttachmentSet(id: number, dataKey: string): ProcessInstanceAttachmentSetEntity {
    return {
        id,
        dataKey,
        name: dataKey,
        processInstanceId: 42,
        processInstanceTaskId: null,
    };
}
