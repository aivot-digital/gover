import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {ProcessAttachmentDisplayView} from './process-attachment-display-view';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import {ProcessTaskViewAttachmentProvider} from '../modules/process/pages/details/process-task-view-attachment-context';
import type {BaseViewProps} from './base-view';
import type {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';
import type {ProcessInstanceAttachmentEntity} from '../modules/process/entities/process-instance-attachment-entity';

describe('ProcessAttachmentDisplayView', () => {
    it('should render a configuration prompt in the editor', () => {
        render(
            <ProcessAttachmentDisplayView
                {...createBaseProps()}
            />,
        );

        expect(screen.getByText('Konfigurieren Sie einen Dateinamen, um passende Vorgangsanhänge anzuzeigen.')).toBeInTheDocument();
    });

    it('should render an editor placeholder without task attachment context', () => {
        render(
            <ProcessAttachmentDisplayView
                {...createBaseProps({
                    fileName: 'evidence.pdf',
                    hint: 'Bitte prüfen Sie den Anhang sorgfältig.',
                })}
            />,
        );

        expect(screen.getByText('Anhang zum Vorgang')).toBeInTheDocument();
        expect(screen.getByText('evidence.pdf')).toBeInTheDocument();
        expect(screen.getByText('Bitte prüfen Sie den Anhang sorgfältig.')).toBeInTheDocument();
        expect(screen.getByText('Dies ist eine Vorschau. Anhänge können im Modellierungsmodus nicht angesehen oder heruntergeladen werden.')).toBeInTheDocument();
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('should render only attachments with an exact matching file name', () => {
        render(
            <ProcessTaskViewAttachmentProvider
                value={{
                    attachments: [
                        createAttachment('1', 'evidence.pdf'),
                        createAttachment('2', 'other.pdf'),
                        createAttachment('3', 'evidence.pdf'),
                    ],
                    isLoadingAttachments: false,
                    viewAttachment: jest.fn(),
                    downloadAttachment: jest.fn(),
                }}
            >
                <ProcessAttachmentDisplayView
                    {...createBaseProps({
                        fileName: 'evidence.pdf',
                    })}
                />
            </ProcessTaskViewAttachmentProvider>,
        );

        expect(screen.getAllByText('evidence.pdf')).toHaveLength(2);
        expect(screen.queryByText('other.pdf')).not.toBeInTheDocument();
    });

    it('should trigger the provided download handler for matching attachments', () => {
        const downloadAttachment = jest.fn().mockResolvedValue(undefined);

        render(
            <ProcessTaskViewAttachmentProvider
                value={{
                    attachments: [createAttachment('1', 'evidence.pdf')],
                    isLoadingAttachments: false,
                    viewAttachment: jest.fn(),
                    downloadAttachment,
                }}
            >
                <ProcessAttachmentDisplayView
                    {...createBaseProps({
                        fileName: 'evidence.pdf',
                    })}
                />
            </ProcessTaskViewAttachmentProvider>,
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

        render(
            <ProcessTaskViewAttachmentProvider
                value={{
                    attachments: [createAttachment('1', 'evidence.pdf')],
                    isLoadingAttachments: false,
                    viewAttachment,
                    downloadAttachment,
                }}
            >
                <ProcessAttachmentDisplayView
                    {...createBaseProps({
                        fileName: 'evidence.pdf',
                    })}
                />
            </ProcessTaskViewAttachmentProvider>,
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

    it('should render an explicit empty state when no attachment matches', () => {
        render(
            <ProcessTaskViewAttachmentProvider
                value={{
                    attachments: [createAttachment('1', 'other.pdf')],
                    isLoadingAttachments: false,
                    viewAttachment: jest.fn(),
                    downloadAttachment: jest.fn(),
                }}
            >
                <ProcessAttachmentDisplayView
                    {...createBaseProps({
                        fileName: 'evidence.pdf',
                    })}
                />
            </ProcessTaskViewAttachmentProvider>,
        );

        expect(screen.getByText('Für den konfigurierten Dateinamen wurden keine Anhänge gefunden.')).toBeInTheDocument();
    });
});

function createBaseProps(
    overrides?: Partial<ProcessAttachmentDisplayElement>,
): BaseViewProps<ProcessAttachmentDisplayElement, void> {
    return {
        element: {
            id: 'pa_test',
            type: ElementType.ProcessAttachmentDisplay,
            weight: 12,
            fileName: undefined,
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

function createAttachment(key: string, fileName: string): ProcessInstanceAttachmentEntity {
    return {
        key,
        fileName,
        processInstanceId: 42,
        processInstanceTaskId: null,
        storageProviderId: 7,
        storagePathFromRoot: '/proc-1/test/attachments/' + key,
        uploadedByUserId: 'user-1',
    };
}
