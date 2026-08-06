import {describe, expect, it, vi} from 'vitest';
import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {ElementType} from '../data/element-type/element-type';
import type {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';
import {ProcessAttachmentDisplayEditor} from './process-attachment-display-editor';
import {ProcessNodeEditorProvider} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import type {ProcessNodeDefinitionMetadata} from '../modules/process/entities/process-node-definition-metadata';
import type {ProcessNodeEntity} from '../modules/process/entities/process-node-entity';

// This spec verifies the editor patch wiring. Mock the nested input controls so the
// test does not depend on MUI Autocomplete/Popper behavior in jsdom.
vi.mock('../components/process-instance-attachment-set-select/process-instance-attachment-set-select', () => ({
    ProcessInstanceAttachmentSetSelect: (props: {
        label: string;
        onChange: (value: string[] | null) => void;
    }) => (
        <button
            type="button"
            onClick={() => props.onChange(['case_documents'])}
        >
            {props.label}
        </button>
    ),
}));

vi.mock('../components/text-field/text-field-component', () => ({
    TextFieldComponent: (props: {
        label: string;
        value?: string | null;
        onChange: (value: string | null) => void;
    }) => (
        <input
            aria-label={props.label}
            value={props.value ?? ''}
            onChange={(event) => props.onChange(event.target.value.length > 0 ? event.target.value : null)}
        />
    ),
}));

describe('ProcessAttachmentDisplayEditor', () => {
    it('should suggest and patch the configured attachment set key', () => {
        const onPatch = vi.fn();

        renderWithEditorMetadata(
            <ProcessAttachmentDisplayEditor
                element={createElement()}
                onPatch={onPatch}
                editable
                hasSummaryLayoutParent={false}
                scope="application"
            />,
        );

        fireEvent.click(screen.getByText('Schlüssel des Anlagensatzes'));

        expect(onPatch).toHaveBeenCalledWith({
            attachmentSetKey: 'case_documents',
        });
    });

    it('should patch the configured label and hint', () => {
        const onPatch = vi.fn();

        renderWithEditorMetadata(
            <ProcessAttachmentDisplayEditor
                element={createElement()}
                onPatch={onPatch}
                editable
                hasSummaryLayoutParent={false}
                scope="application"
            />,
        );

        fireEvent.change(screen.getByLabelText('Beschriftung'), {
            target: {
                value: 'Neue Fallunterlagen',
            },
        });
        fireEvent.change(screen.getByLabelText('Hinweis'), {
            target: {
                value: 'Bitte prüfen Sie den Anhang sorgfältig.',
            },
        });

        expect(onPatch).toHaveBeenCalledWith({
            label: 'Neue Fallunterlagen',
        });
        expect(onPatch).toHaveBeenCalledWith({
            hint: 'Bitte prüfen Sie den Anhang sorgfältig.',
        });
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

function createElement(): ProcessAttachmentDisplayElement {
    return {
        id: 'attachment-display',
        type: ElementType.ProcessAttachmentDisplay,
        name: undefined,
        testProtocolSet: undefined,
        visibility: undefined,
        override: undefined,
        metadata: undefined,
        weight: 12,
        attachmentSetKey: 'case_documents',
        label: 'Fallunterlagen',
        hint: undefined,
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
                isMultifile: true,
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
