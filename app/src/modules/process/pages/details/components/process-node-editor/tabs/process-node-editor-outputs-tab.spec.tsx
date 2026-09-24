import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {type GroupLayout} from '../../../../../../../models/elements/form/layout/group-layout';
import {type ProcessNodeEntity} from '../../../../../entities/process-node-entity';
import {
    ProcessNodeExecutionType,
    type ProcessNodeOutput,
    type ProcessNodeProvider,
    ProcessNodeType,
} from '../../../../../services/process-node-provider-api-service';
import {ProcessNodeEditorProvider} from '../process-node-editor-context';
import {ProcessNodeEditorOutputsTab} from './process-node-editor-outputs-tab';

vi.mock('../../../../../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

describe('ProcessNodeEditorOutputsTab', () => {
    it('shows and updates the data key even when the provider has no outputs', () => {
        const {node, setNode} = renderOutputsTab();
        const dataKey = screen.getByRole('textbox', {name: /Datenschlüssel/});

        expect(dataKey).toHaveValue('copy_field');
        expect(dataKey).toBeEnabled();
        expect(screen.getByText(/Mit dem Datenschlüssel greifen Sie auf die Elementdaten und Ausführungsmetadaten/)).toBeInTheDocument();
        expect(screen.getByRole('heading', {name: 'Ausgangsdaten'})).toBeInTheDocument();
        expect(screen.getByRole('heading', {name: 'Keine zuweisbaren Ausgangsdaten'})).toBeInTheDocument();
        expect(screen.getByText(/erzeugt keine Ausgangsdaten oder schreibt Vorgangsdaten auf anderem Weg/)).toBeInTheDocument();
        expect(screen.getByText(/Über den Datenschlüssel können Sie dennoch auf Metadaten zu seiner Ausführung zugreifen/)).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: 'Datenstruktur der Ausgangsdaten'})).not.toBeInTheDocument();

        fireEvent.change(dataKey, {target: {value: 'copied_field'}});
        expect(setNode).toHaveBeenCalledWith({...node, dataKey: 'copied_field'}, false);
    });

    it('keeps the data key read only when the process cannot be edited', () => {
        renderOutputsTab({isEditable: false});

        expect(screen.getByRole('textbox', {name: /Datenschlüssel/})).toBeDisabled();
    });

    it('still shows mappings and the output data structure for providers with outputs', () => {
        renderOutputsTab({outputs: [{
            key: 'result',
            label: 'Ergebnis',
            description: 'Das Ergebnis des Elements.',
            typeDefinition: 'string',
        }]});

        expect(screen.getByRole('textbox', {name: /Datenschlüssel/})).toHaveValue('copy_field');
        expect(screen.getByText(/Mit dem Datenschlüssel greifen Sie auf die Elementdaten und Ausführungsmetadaten/)).toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: /Ergebnis/})).toBeInTheDocument();
        expect(screen.getByRole('heading', {name: 'Datenstruktur der Ausgangsdaten'})).toBeInTheDocument();
        expect(screen.getByText('_.copy_field.result')).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: 'Keine zuweisbaren Ausgangsdaten'})).not.toBeInTheDocument();
    });
});

function renderOutputsTab({
    outputs = [],
    isEditable = true,
}: {
    outputs?: ProcessNodeOutput[];
    isEditable?: boolean;
} = {}) {
    const node: ProcessNodeEntity = {
        id: 1,
        processId: 2,
        processVersion: 1,
        processNodeDefinitionKey: 'test-node',
        processNodeDefinitionVersion: 1,
        name: null,
        description: null,
        dataKey: 'copy_field',
        configuration: {},
        outputMappings: {},
        timeLimitDays: null,
        requirements: null,
        notes: null,
        savedWithErrors: false,
        created: '',
        updated: '',
    };
    const provider: ProcessNodeProvider = {
        key: 'test-node',
        componentKey: 'test-node',
        componentType: 'ProcessNodeDefinition',
        componentVersion: '1.0.0',
        deprecationNotice: null,
        majorVersion: 1,
        type: ProcessNodeType.Action,
        executionTypes: [ProcessNodeExecutionType.Automatic],
        name: 'Testelement',
        abstractDescription: '',
        description: '',
        documentationUrl: null,
        parentPluginKey: 'test-plugin',
        ports: [],
        outputs,
    };
    const setNode = vi.fn();

    render(
        <ProcessNodeEditorProvider value={{
            provider,
            layout: {} as GroupLayout,
            testClaim: null,
            node,
            setNode,
            isEditable,
            problems: null,
            incomingMetadata: null,
        }}>
            <ProcessNodeEditorOutputsTab/>
        </ProcessNodeEditorProvider>,
    );

    return {node, setNode};
}
