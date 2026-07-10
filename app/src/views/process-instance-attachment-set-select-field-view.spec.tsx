import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {ProcessInstanceAttachmentSetSelectFieldView} from './process-instance-attachment-set-select-field-view';
import {ProcessNodeEditorProvider} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import type {BaseViewProps} from './base-view';
import type {
    ProcessInstanceAttachmentSetSelectElement,
} from '../models/elements/form/input/process-instance-attachment-set-select-element';
import type {ProcessNodeEntity} from '../modules/process/entities/process-node-entity';
import type {ProcessNodeDefinitionMetadata} from '../modules/process/entities/process-node-definition-metadata';

describe('ProcessInstanceAttachmentSetSelectFieldView', () => {
    it('should display selected attachment set labels instead of data keys', () => {
        renderWithEditorMetadata(
            <ProcessInstanceAttachmentSetSelectFieldView
                {...createBaseProps({
                    value: ['case_documents'],
                })}
            />,
        );

        expect(screen.getByText('Fallunterlagen')).toBeInTheDocument();
    });

    it('should persist selected attachment set data keys', async () => {
        const setValue = jest.fn();

        renderWithEditorMetadata(
            <ProcessInstanceAttachmentSetSelectFieldView
                {...createBaseProps({
                    value: null,
                    setValue,
                })}
            />,
        );

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Anlagensaetze'}));
        fireEvent.click(await screen.findByText('Fallunterlagen'));

        expect(setValue).toHaveBeenCalledWith(['case_documents']);
    });

    it('should render unknown saved data keys as fallback labels', () => {
        renderWithEditorMetadata(
            <ProcessInstanceAttachmentSetSelectFieldView
                {...createBaseProps({
                    value: ['legacy_documents'],
                })}
            />,
        );

        expect(screen.getByText('legacy_documents')).toBeInTheDocument();
    });
});

function renderWithEditorMetadata(children: React.ReactElement, metadata: ProcessNodeDefinitionMetadata = createMetadata()) {
    return render(
        <ProcessNodeEditorProvider
            value={{
                provider: {} as any,
                layout: {} as any,
                testClaim: null,
                node: createNode(10, 'E-Mail'),
                setNode: jest.fn(),
                isEditable: true,
                problems: null,
                incomingMetadata: metadata,
            }}
        >
            {children}
        </ProcessNodeEditorProvider>,
    );
}

function createBaseProps(
    options?: {
        element?: Partial<ProcessInstanceAttachmentSetSelectElement>;
        value?: string[] | null;
        setValue?: jest.Mock;
    },
): BaseViewProps<ProcessInstanceAttachmentSetSelectElement, string[]> {
    const {
        value,
        setValue,
        element: elementOverrides,
    } = options ?? {};

    return {
        element: {
            id: 'attachmentSets',
            type: ElementType.ProcessInstanceAttachmentSetSelect,
            weight: 12,
            label: 'Anlagensaetze',
            hint: undefined,
            required: undefined,
            disabled: undefined,
            technical: undefined,
            destinationKey: undefined,
            validation: undefined,
            value: undefined,
            placeholder: 'Anlagensatz auswaehlen',
            minItems: undefined,
            maxItems: undefined,
            metadata: undefined,
            name: undefined,
            override: undefined,
            testProtocolSet: undefined,
            visibility: undefined,
            ...elementOverrides,
        },
        isBusy: false,
        isDeriving: false,
        value,
        setValue: setValue ?? jest.fn(),
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

function createMetadata(): ProcessNodeDefinitionMetadata {
    return {
        reusableUiDefinitions: [],
        forwardedAttachmentSets: [
            {
                dataKey: 'case_documents',
                label: 'Fallunterlagen',
                subLabel: 'Upload',
                origin: createNode(1, 'Startformular'),
            },
        ],
        forwardedProcessDataKeys: [],
        forwardedIdentities: [],
    };
}

function createNode(id: number, name: string): ProcessNodeEntity {
    return {
        id,
        name,
        processId: 1,
        processVersion: 1,
        processNodeDefinitionKey: 'test',
        processNodeDefinitionVersion: 1,
        description: null,
        dataKey: 'node_' + id,
        configuration: {},
        outputMappings: {},
        timeLimitDays: null,
        requirements: null,
        notes: null,
        savedWithErrors: false,
        created: '2026-01-01T00:00:00Z',
        updated: '2026-01-01T00:00:00Z',
    };
}
