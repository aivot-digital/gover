import React from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ProcessDataKeyInputFieldView} from './process-data-key-input-field-view';
import {ProcessNodeEditorProvider} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {ViewDispatcherContextProvider, ViewDispatcherMode} from '../components/view-dispatcher/view-dispatcher.context';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import type {BaseViewProps} from './base-view';
import type {
    ProcessDataKeyInputFieldElement,
} from '../models/elements/form/input/process-data-key-input-field-element';
import type {ProcessNodeEntity} from '../modules/process/entities/process-node-entity';
import type {ProcessNodeDefinitionMetadata} from '../modules/process/entities/process-node-definition-metadata';

describe('ProcessDataKeyInputFieldView', () => {
    it('should offer relative child keys when scoped by another process data key input', async () => {
        renderWithContexts(createBaseProps());

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Child'}));

        expect(await screen.findByText('name')).toBeInTheDocument();
        expect(screen.getByText('amount')).toBeInTheDocument();
        expect(screen.queryByText('items.*.name')).not.toBeInTheDocument();
        expect(screen.queryByText('nested.*.value')).not.toBeInTheDocument();
        expect(screen.queryByText('other.*.name')).not.toBeInTheDocument();
    });

    it('should store the selected scoped child key without the replicating wildcard', async () => {
        const setValue = jest.fn();
        renderWithContexts(createBaseProps({setValue}));

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Child'}));
        fireEvent.click(await screen.findByText('name'));

        expect(setValue).toHaveBeenCalledWith('name');
    });

    it('should not accept typed values while scoped choices are active', () => {
        const setValue = jest.fn();
        renderWithContexts(createBaseProps({setValue}));

        fireEvent.change(screen.getByRole('combobox', {name: 'Child'}), {
            target: {
                value: 'free.typed.key',
            },
        });

        expect(setValue).not.toHaveBeenCalled();
    });

    it('should clear a scoped value that is no longer selectable', async () => {
        const setValue = jest.fn();
        renderWithContexts(createBaseProps({
            value: 'legacy',
            setValue,
        }));

        await waitFor(() => {
            expect(setValue).toHaveBeenCalledWith(null);
        });
    });
});

function renderWithContexts(
    props: BaseViewProps<ProcessDataKeyInputFieldElement, string>,
    metadata: ProcessNodeDefinitionMetadata = createMetadata(),
) {
    const scopeElement = createProcessDataKeyInputElement('containerKey', 'Container');

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
            <ViewDispatcherContextProvider
                value={{
                    rootElement: createRootElement(),
                    allElements: [scopeElement, props.element],
                    mode: ViewDispatcherMode.Viewer,
                    rootAuthoredElementValues: props.authoredElementValues,
                    rootDerivedData: props.derivedData,
                }}
            >
                <ProcessDataKeyInputFieldView {...props} />
            </ViewDispatcherContextProvider>
        </ProcessNodeEditorProvider>,
    );
}

function createBaseProps(
    options?: {
        value?: string | null;
        setValue?: jest.Mock;
    },
): BaseViewProps<ProcessDataKeyInputFieldElement, string> {
    const {
        value,
        setValue,
    } = options ?? {};

    return {
        element: {
            ...createProcessDataKeyInputElement('childKey', 'Child'),
            disableWildCards: true,
            scopeProcessDataKeyInputElementId: 'containerKey',
        },
        isBusy: false,
        isDeriving: false,
        value,
        setValue: setValue ?? jest.fn(),
        onBlur: jest.fn(),
        errors: undefined,
        errorDetails: undefined,
        authoredElementValues: {
            containerKey: 'items',
        },
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

function createProcessDataKeyInputElement(id: string, label: string): ProcessDataKeyInputFieldElement {
    return {
        id,
        type: ElementType.ProcessDataKeyInput,
        weight: 12,
        label,
        hint: undefined,
        required: undefined,
        disabled: undefined,
        technical: undefined,
        destinationKey: undefined,
        validation: undefined,
        value: undefined,
        disableWildCards: true,
        scopeProcessDataKeyInputElementId: undefined,
        metadata: undefined,
        name: undefined,
        override: undefined,
        testProtocolSet: undefined,
        visibility: undefined,
    };
}

function createMetadata(): ProcessNodeDefinitionMetadata {
    return {
        reusableUiDefinitions: [],
        forwardedAttachmentSets: [],
        forwardedProcessDataKeys: [
            {
                processDataKey: 'items',
                label: 'Items',
                subLabel: null,
                origin: createNode(1, 'Startformular'),
            },
            {
                processDataKey: 'items.*.name',
                label: 'Name',
                subLabel: null,
                origin: createNode(1, 'Startformular'),
            },
            {
                processDataKey: 'items.*.amount',
                label: 'Amount',
                subLabel: null,
                origin: createNode(1, 'Startformular'),
            },
            {
                processDataKey: 'items.*.nested.*.value',
                label: 'Nested',
                subLabel: null,
                origin: createNode(1, 'Startformular'),
            },
            {
                processDataKey: 'other.*.name',
                label: 'Other',
                subLabel: null,
                origin: createNode(2, 'Other node'),
            },
        ],
        forwardedIdentities: [],
    };
}

function createRootElement() {
    return {
        id: 'root',
        type: ElementType.GroupLayout,
        name: undefined,
        children: [],
        metadata: undefined,
        override: undefined,
        testProtocolSet: undefined,
        visibility: undefined,
    } as any;
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
