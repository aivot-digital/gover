import {fireEvent, render, screen} from '@testing-library/react';
import {ProcessAttachmentDisplayView} from './process-attachment-display-view';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import {ProcessTaskViewAttachmentProvider} from '../modules/process/pages/details/process-task-view-attachment-context';
import {BaseViewProps} from './base-view';
import {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';

describe('ProcessAttachmentDisplayView', () => {
    it('should render an editor placeholder without task attachment context', () => {
        render(
            <ProcessAttachmentDisplayView
                {...createBaseProps({
                    fileName: 'evidence.pdf',
                })}
            />,
        );

        expect(screen.getByText('Anlagen mit dem Dateinamen "evidence.pdf" werden hier angezeigt.')).toBeInTheDocument();
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
                    attachments: [
                        createAttachment('1', 'evidence.pdf'),
                    ],
                    isLoadingAttachments: false,
                    downloadAttachment: downloadAttachment,
                }}
            >
                <ProcessAttachmentDisplayView
                    {...createBaseProps({
                        fileName: 'evidence.pdf',
                    })}
                />
            </ProcessTaskViewAttachmentProvider>,
        );

        fireEvent.click(screen.getByRole('button', {name: 'delete'}));

        expect(downloadAttachment).toHaveBeenCalledWith(expect.objectContaining({
            key: '1',
            fileName: 'evidence.pdf',
        }));
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

function createAttachment(key: string, fileName: string) {
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
