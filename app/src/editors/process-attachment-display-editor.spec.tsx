import {describe, expect, it, vi} from 'vitest';
import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ThemeProvider} from '@mui/material';
import {BaseTheme} from '../theming/base-theme';
import {ElementType} from '../data/element-type/element-type';
import type {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';
import {ProcessAttachmentDisplayEditor} from './process-attachment-display-editor';
import {ProcessNodeEditorProvider} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import type {ProcessNodeDefinitionMetadata} from '../modules/process/entities/process-node-definition-metadata';
import type {ProcessNodeEntity} from '../modules/process/entities/process-node-entity';

describe('ProcessAttachmentDisplayEditor', () => {
    it('requires an attachment set and stores its key while displaying its title', async () => {
        const onPatch = vi.fn();
        const user = userEvent.setup();

        function Editor() {
            const [element, setElement] = React.useState<ProcessAttachmentDisplayElement>({...createElement(), attachmentSetKey: null});
            return <ProcessAttachmentDisplayEditor element={element} onPatch={patch => {
                onPatch(patch);
                setElement(current => ({...current, ...patch}));
            }} editable hasSummaryLayoutParent={false} scope="application"/>;
        }
        renderWithEditorMetadata(<Editor/>);
        const input = screen.getByRole('combobox', {name: 'Schlüssel des Anlagensatzes'});
        expect(input).toBeRequired();
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription(/Alle Vorgangsanlagen aus Anlagensätzen mit diesem Schlüssel werden angezeigt/);
        fireEvent.mouseDown(input);
        fireEvent.click(await screen.findByRole('option', {name: /^Fallunterlagen/}));
        expect(input).toHaveValue('Fallunterlagen');
        expect(input).not.toHaveAttribute('aria-invalid', 'true');

        expect(onPatch).toHaveBeenCalledWith({
            attachmentSetKey: 'case_documents',
        });

        await user.click(input);
        await user.click(screen.getByRole('button', {name: 'Leeren'}));
        expect(onPatch).toHaveBeenLastCalledWith({attachmentSetKey: null});
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription(/Alle Vorgangsanlagen aus Anlagensätzen mit diesem Schlüssel werden angezeigt/);
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

        fireEvent.change(screen.getByRole('textbox', {name: /^Beschriftung/}), {
            target: {
                value: 'Neue Fallunterlagen',
            },
        });
        fireEvent.blur(screen.getByRole('textbox', {name: /^Beschriftung/}));
        fireEvent.change(screen.getByRole('textbox', {name: /^Hinweis/}), {
            target: {
                value: 'Bitte prüfen Sie den Anhang sorgfältig.',
            },
        });

        fireEvent.blur(screen.getByRole('textbox', {name: /^Hinweis/}));

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
                incomingMetadataLoadFailed: false,
                incomingMetadata: metadata,
            }}
        >
            <ThemeProvider theme={BaseTheme}>{children}</ThemeProvider>
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
        ports: [],
        portResolutionProblems: [],
        processNodeDefinitionKey: 'test',
        processNodeDefinitionVersion: 1,
        description: null,
        dataKey: 'node_' + id,
        configuration: {},
        outputMappings: {},
        deadlines: {},
        deadlineDefinitions: [],
        requirements: null,
        notes: null,
        savedWithErrors: false,
        created: '2026-01-01T00:00:00Z',
        updated: '2026-01-01T00:00:00Z',
    };
}
