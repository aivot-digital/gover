import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {ElementType} from '../data/element-type/element-type';
import type {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';
import {ProcessAttachmentDisplayEditor} from './process-attachment-display-editor';
import {ProcessNodeEditorProvider} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import type {ProcessNodeDefinitionMetadata} from '../modules/process/entities/process-node-definition-metadata';
import type {ProcessNodeEntity} from '../modules/process/entities/process-node-entity';

describe('ProcessAttachmentDisplayEditor', () => {
    it('should suggest and patch the configured attachment set key', async () => {
        const onPatch = jest.fn();

        renderWithEditorMetadata(
            <ProcessAttachmentDisplayEditor
                element={createElement()}
                onPatch={onPatch}
                editable
                hasSummaryLayoutParent={false}
                scope="application"
            />,
        );

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Schlüssel des Anlagensatzes'}));
        fireEvent.click(await screen.findByText('Fallunterlagen'));

        expect(onPatch).toHaveBeenCalledWith({
            attachmentSetKey: 'case_documents',
        });
    });

    it('should patch the configured label and hint', () => {
        const onPatch = jest.fn();

        renderWithEditorMetadata(
            <ProcessAttachmentDisplayEditor
                element={createElement()}
                onPatch={onPatch}
                editable
                hasSummaryLayoutParent={false}
                scope="application"
            />,
        );

        fireEvent.change(screen.getByRole('textbox', {name: 'Beschriftung'}), {
            target: {
                value: 'Fallunterlagen',
            },
        });
        fireEvent.change(screen.getByRole('textbox', {name: 'Hinweis'}), {
            target: {
                value: 'Bitte prüfen Sie den Anhang sorgfältig.',
            },
        });

        expect(onPatch).toHaveBeenCalledWith({
            label: 'Fallunterlagen',
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
