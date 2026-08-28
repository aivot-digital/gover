import React from 'react';
import {describe, expect, it, vi, type Mock} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import {ProcessIdentityIdInputFieldView} from './process-identity-id-input-field-view';
import {ProcessNodeEditorProvider} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import {type BaseViewProps} from './base-view';
import {type ProcessIdentityIdInputElement} from '../models/elements/form/input/process-identity-id-input-element';
import {type ProcessNodeEntity} from '../modules/process/entities/process-node-entity';
import {type ProcessNodeDefinitionMetadata} from '../modules/process/entities/process-node-definition-metadata';

describe('ProcessIdentityIdInputFieldView', () => {
    it('should display metadata labels for selected identity IDs', () => {
        renderWithEditorMetadata(
            <ProcessIdentityIdInputFieldView
                {...createBaseProps({
                    value: 'citizen',
                })}
            />,
        );

        expect(screen.getByText('Bürgerkonto')).toBeInTheDocument();
    });

    it('should show identity context and persist a scalar identity ID', async () => {
        const setValue = vi.fn();

        renderWithEditorMetadata(
            <ProcessIdentityIdInputFieldView
                {...createBaseProps({
                    value: null,
                    setValue,
                })}
            />,
        );

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Prozessidentitaeten'}));

        expect(await screen.findByText('citizen - Antragstellende - Startformular')).toBeInTheDocument();
        fireEvent.click(screen.getByText('Bürgerkonto'));

        expect(setValue).toHaveBeenCalledWith('citizen');
    });

    it('should use the first metadata entry for duplicate identity IDs', async () => {
        renderWithEditorMetadata(
            <ProcessIdentityIdInputFieldView
                {...createBaseProps({
                    value: null,
                })}
            />,
            {
                ...createMetadata(),
                forwardedIdentities: [
                    ...createMetadata().forwardedIdentities,
                    {
                        identityId: 'citizen',
                        label: 'Duplikat',
                        subLabel: 'Soll nicht erscheinen',
                        origin: createNode(2, 'Duplikatquelle'),
                    },
                ],
            },
        );

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Prozessidentitaeten'}));

        expect(await screen.findByText('Bürgerkonto')).toBeInTheDocument();
        expect(screen.queryByText('Duplikat')).not.toBeInTheDocument();
    });

    it('should preserve and invalidate identity IDs missing from loaded metadata', () => {
        renderWithEditorMetadata(
            <ProcessIdentityIdInputFieldView
                {...createBaseProps({
                    value: 'legacy-identity',
                })}
            />,
        );

        expect(screen.getByText('legacy-identity')).toBeInTheDocument();
        expect(screen.getByText('Die ausgewählte Prozessidentität „legacy-identity“ ist nicht mehr verfügbar.')).toBeInTheDocument();
    });

    it('should not duplicate an unavailable identity error returned by the backend', () => {
        const unavailableError = 'Die ausgewählte Prozessidentität „legacy-identity“ ist nicht mehr verfügbar.';

        renderWithEditorMetadata(
            <ProcessIdentityIdInputFieldView
                {...createBaseProps({
                    value: 'legacy-identity',
                    errors: [unavailableError + ' Provider error.'],
                })}
            />,
        );

        expect(screen.getByText(unavailableError + ' Provider error.')).toBeInTheDocument();
    });

    it('should not invalidate saved identity IDs before metadata has loaded', () => {
        renderWithEditorMetadata(
            <ProcessIdentityIdInputFieldView
                {...createBaseProps({
                    value: 'citizen',
                })}
            />,
            null,
        );

        expect(screen.getByText('citizen')).toBeInTheDocument();
        expect(screen.queryByText(/nicht mehr verfügbar/)).not.toBeInTheDocument();
    });

    it('should show an empty state when no identities are forwarded', async () => {
        renderWithEditorMetadata(
            <ProcessIdentityIdInputFieldView
                {...createBaseProps({
                    value: null,
                })}
            />,
            {
                ...createMetadata(),
                forwardedIdentities: [],
            },
        );

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Prozessidentitaeten'}));

        expect(await screen.findByText('Keine Prozessidentitäten verfügbar')).toBeInTheDocument();
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
        element?: Partial<ProcessIdentityIdInputElement>;
        value?: string | null;
        setValue?: Mock;
        errors?: string[];
    },
): BaseViewProps<ProcessIdentityIdInputElement, string> {
    const {
        value,
        setValue,
        errors,
        element: elementOverrides,
    } = options ?? {};

    return {
        element: {
            id: 'identity',
            type: ElementType.ProcessIdentityIdInput,
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
        errors,
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
