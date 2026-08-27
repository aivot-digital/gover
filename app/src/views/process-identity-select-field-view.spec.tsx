import {describe, expect, it, vi, type Mock} from 'vitest';
import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {ProcessIdentitySelectFieldView} from './process-identity-select-field-view';
import {ProcessNodeEditorProvider} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import type {BaseViewProps} from './base-view';
import type {ProcessIdentitySelectElement} from '../models/elements/form/input/process-identity-select-element';
import type {ProcessNodeEntity} from '../modules/process/entities/process-node-entity';
import type {ProcessNodeDefinitionMetadata} from '../modules/process/entities/process-node-definition-metadata';

describe('ProcessIdentitySelectFieldView', () => {
    it('should display the metadata label for a selected identity ID', () => {
        renderWithEditorMetadata(
            <ProcessIdentitySelectFieldView
                {...createBaseProps({
                    value: ['citizen'],
                })}
            />,
        );

        expect(screen.getByText('Bürgerkonto')).toBeInTheDocument();
    });

    it('should render full metadata context and persist identity IDs', async () => {
        const setValue = vi.fn();

        renderWithEditorMetadata(
            <ProcessIdentitySelectFieldView
                {...createBaseProps({
                    value: null,
                    setValue,
                })}
            />,
        );

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Prozessidentitaeten'}));

        expect(await screen.findByText('citizen - Antragstellende - Startformular')).toBeInTheDocument();
        fireEvent.click(screen.getByText('Bürgerkonto'));

        expect(setValue).toHaveBeenCalledWith(['citizen']);
    });

    it('should render a single select while retaining the list value contract when maxItems is one', () => {
        renderWithEditorMetadata(
            <ProcessIdentitySelectFieldView
                {...createBaseProps({
                    element: {
                        maxItems: 1,
                    },
                    value: ['citizen'],
                })}
            />,
        );

        expect(screen.getByRole('combobox', {name: 'Prozessidentitaeten'})).toHaveValue('Bürgerkonto');
        expect(screen.getByText('1/1')).toBeInTheDocument();
    });

    it('should emit a one-item list when selecting in single-select mode', async () => {
        const setValue = vi.fn();

        renderWithEditorMetadata(
            <ProcessIdentitySelectFieldView
                {...createBaseProps({
                    element: {
                        maxItems: 1,
                    },
                    value: null,
                    setValue,
                })}
            />,
        );

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Prozessidentitaeten'}));
        fireEvent.click(await screen.findByText('Bürgerkonto'));

        expect(setValue).toHaveBeenCalledWith(['citizen']);
    });

    it('should retain and mark unavailable saved identity IDs as invalid', () => {
        renderWithEditorMetadata(
            <ProcessIdentitySelectFieldView
                {...createBaseProps({
                    value: ['legacy-identity'],
                })}
            />,
        );

        expect(screen.getByText('legacy-identity')).toBeInTheDocument();
        expect(screen.getByText('Die ausgewählte Prozessidentität „legacy-identity“ ist nicht mehr verfügbar.')).toBeInTheDocument();
    });

    it('should not mark values invalid before incoming metadata has loaded', () => {
        renderWithEditorMetadata(
            <ProcessIdentitySelectFieldView
                {...createBaseProps({
                    value: ['citizen'],
                })}
            />,
            null,
        );

        expect(screen.getByText('citizen')).toBeInTheDocument();
        expect(screen.queryByText(/nicht mehr verfügbar/)).not.toBeInTheDocument();
    });
});

function renderWithEditorMetadata(
    children: React.ReactElement,
    metadata: ProcessNodeDefinitionMetadata | null = createMetadata(),
) {
    return render(
        <ProcessNodeEditorProvider
            value={{
                provider: {} as any,
                layout: {} as any,
                testClaim: null,
                node: createNode(10, 'E-Mail'),
                setNode: vi.fn(),
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
        element?: Partial<ProcessIdentitySelectElement>;
        value?: string[] | null;
        setValue?: Mock;
    },
): BaseViewProps<ProcessIdentitySelectElement, string[]> {
    const {
        value,
        setValue,
        element: elementOverrides,
    } = options ?? {};

    return {
        element: {
            id: 'identities',
            type: ElementType.ProcessIdentitySelect,
            weight: 12,
            label: 'Prozessidentitaeten',
            hint: undefined,
            required: undefined,
            disabled: undefined,
            technical: undefined,
            destinationKey: undefined,
            validation: undefined,
            value: undefined,
            placeholder: 'Prozessidentität auswählen',
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
        setValue: setValue ?? vi.fn(),
        onBlur: vi.fn(),
        errors: undefined,
        errorDetails: undefined,
        authoredElementValues: {},
        onAuthoredElementValuesChange: vi.fn(),
        onElementBlur: vi.fn(),
        derivedData: createDerivedRuntimeElementData(),
        onDerive: vi.fn(),
        onEvent: vi.fn(),
        onResetErrors: vi.fn(),
        suppressErrors: false,
        derivationTriggerIdQueue: [],
    };
}

function createMetadata(): ProcessNodeDefinitionMetadata {
    return {
        reusableUiDefinitions: [],
        forwardedAttachmentSets: [],
        forwardedProcessDataKeys: [],
        forwardedIdentities: [
            {
                identityId: 'citizen',
                label: 'Bürgerkonto',
                subLabel: 'Antragstellende',
                origin: createNode(1, 'Startformular'),
            },
        ],
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
