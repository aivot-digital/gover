import {useState} from 'react';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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
import {copyToClipboardText} from '../../../../../../../utils/copy-to-clipboard';

vi.mock('../../../../../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../../../../../../utils/copy-to-clipboard', () => ({
    copyToClipboardText: vi.fn().mockResolvedValue(true),
}));

describe('ProcessNodeEditorOutputsTab', () => {
    it('shows and updates the data key even when the provider has no outputs', () => {
        const {node, setNode} = renderOutputsTab();
        const dataKey = screen.getByRole('textbox', {name: /Datenschlüssel/});

        expect(dataKey).toHaveValue('copy_field');
        expect(dataKey).toBeEnabled();
        expect(screen.getByRole('heading', {name: 'Ausgangsdaten'})).toBeInTheDocument();
        expect(screen.getByRole('heading', {name: 'Keine Ausgangsdaten zum Übernehmen'})).toBeInTheDocument();
        expect(screen.getByText(/kann Vorgangsdaten auf anderem Weg verändern/)).toBeInTheDocument();
        expect(screen.getByText(/auf die Daten des Prozesselements und Informationen zur Ausführung zu/)).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /Details zu/})).not.toBeInTheDocument();

        fireEvent.change(dataKey, {target: {value: 'copied_field'}});
        expect(setNode).toHaveBeenCalledWith({...node, dataKey: 'copied_field'}, false);
    });

    it('keeps the data key read only when the process cannot be edited', () => {
        renderOutputsTab({isEditable: false});

        expect(screen.getByRole('textbox', {name: /Datenschlüssel/})).toBeDisabled();
    });

    it('distinguishes element data from the optional target in process data', () => {
        renderOutputsTab({outputs: [resultOutput]});
        const dataKey = screen.getByRole('textbox', {name: /Datenschlüssel/});
        const outputSection = screen.getByRole('region', {name: 'Ergebnis'});
        const target = within(outputSection).getByRole('textbox', {name: 'Zielpfad in den Vorgangsdaten – optional'});

        expect(screen.getAllByRole('textbox')).toEqual([dataKey, target]);
        expect(dataKey).toHaveValue('copy_field');
        expect(dataKey).toHaveAccessibleDescription(/muss innerhalb dieser Prozessversion eindeutig sein/);
        expect(dataKey).toHaveAccessibleDescription(/empfehlen wir einen sprechenden Schlüssel/);
        expect(screen.getByRole('heading', {name: 'Ausgangsdaten'})).toBeInTheDocument();
        expect(within(outputSection).getByRole('heading', {name: 'Ergebnis'})).toBeInTheDocument();
        expect(outputSection).toHaveAccessibleDescription(resultOutput.description);
        expect(target).toHaveValue('');
        expect(target).not.toBeRequired();
        expect(target).not.toHaveAccessibleName(/Details/);
        expect(target).toHaveAccessibleDescription(/Ergebnis.*Das Ergebnis des Elements.*über die Elementdaten zur Verfügung.*zusätzlich in den Vorgangsdaten bereitstellen.*Zielpfad ein/);
        expect(screen.getByText(resultOutput.description)).toBeInTheDocument();
        expect(within(outputSection).getByText('Pfad in den Elementdaten:')).toBeInTheDocument();
        expect(within(outputSection).getByText('_.copy_field.result')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Pfad in den Elementdaten _.copy_field.result kopieren'})).toBeEnabled();
        expect(screen.getByRole('button', {name: 'Details zu Ergebnis anzeigen'})).toBeEnabled();
        expect(screen.queryByRole('heading', {name: 'Keine Ausgangsdaten zum Übernehmen'})).not.toBeInTheDocument();
    });

    it('updates and clears one mapping while retaining other mappings', () => {
        const {node, setNode} = renderOutputsTab({
            outputs: [resultOutput, referenceOutput],
            outputMappings: {reference: 'person.reference'},
        });
        const target = within(screen.getByRole('region', {name: 'Ergebnis'}))
            .getByRole('textbox', {name: /^Zielpfad in den Vorgangsdaten/});

        fireEvent.change(target, {target: {value: 'person.result'}});
        fireEvent.blur(target);

        expect(setNode).toHaveBeenLastCalledWith({
            ...node,
            outputMappings: {reference: 'person.reference', result: 'person.result'},
        }, false);
        expect(within(screen.getByRole('region', {name: 'Referenz'}))
            .getByRole('textbox', {name: /^Zielpfad in den Vorgangsdaten/})).toHaveValue('person.reference');

        fireEvent.click(within(screen.getByRole('region', {name: 'Ergebnis'}))
            .getByRole('button', {name: 'Vorgangsdatenpfad leeren'}));

        expect(setNode).toHaveBeenLastCalledWith({
            ...node,
            outputMappings: {reference: 'person.reference', result: null},
        }, false);
        expect(target).toHaveValue('');
    });

    it('uses the current element data key in the detail path', async () => {
        const user = userEvent.setup();
        renderOutputsTab({outputs: [resultOutput]});

        fireEvent.change(screen.getByRole('textbox', {name: /Datenschlüssel/}), {
            target: {value: 'new_key'},
        });
        expect(screen.getByText('_.new_key.result')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Pfad in den Elementdaten _.new_key.result kopieren'})).toBeInTheDocument();
        expect(screen.queryByText('_.copy_field.result')).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Details zu Ergebnis anzeigen'}));
        expect(within(screen.getByRole('dialog', {name: 'Details zu den Ausgangsdaten'}))
            .getByText('_.new_key.result')).toBeInTheDocument();
    });

    it('opens and closes the correct output details even when mappings are read only', async () => {
        const user = userEvent.setup();
        renderOutputsTab({outputs: [resultOutput, referenceOutput], isEditable: false});

        screen.getAllByRole('textbox').forEach((field) => expect(field).toBeDisabled());
        await user.click(screen.getByRole('button', {name: 'Details zu Referenz anzeigen'}));

        const dialog = screen.getByRole('dialog', {name: 'Details zu den Ausgangsdaten'});
        expect(dialog).toHaveAccessibleDescription(referenceOutput.description);
        expect(within(dialog).getByRole('heading', {name: 'Referenz'})).toBeInTheDocument();
        expect(within(dialog).getByText(referenceOutput.description)).toBeInTheDocument();
        expect(within(dialog).getByText('_.copy_field.reference')).toBeInTheDocument();
        expect(within(dialog).getByRole('button', {name: 'Pfad in den Elementdaten _.copy_field.reference kopieren'})).toBeEnabled();
        const accessSection = within(dialog).getByRole('region', {name: 'Zugriff auf die Daten'});
        expect(within(accessSection).getByText(/direkt auf den Wert in den Elementdaten.*zusätzlich unter dem eingetragenen Zielpfad/)).toBeInTheDocument();
        const typeSection = within(dialog).getByRole('region', {name: 'Datentyp'});
        expect(within(typeSection).getByText('TypeScript')).toBeInTheDocument();
        expect(within(typeSection).getByText(/Text, Zahlen oder ein Objekt mit mehreren Feldern/)).toBeInTheDocument();
        expect(within(dialog).getByTestId('expandable-code-block')).toHaveTextContent(referenceOutput.typeDefinition);
        await user.click(within(dialog).getAllByRole('button', {name: 'Schließen'})[1]);
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });

    it('keeps a mapped output accessible through its complete element data path', async () => {
        const user = userEvent.setup();
        const dataKey = 'mein_langer_datenschluessel';
        const output = {...resultOutput, key: 'storageProviderIdentifierForGeneratedAttachment'};
        const path = `_.${dataKey}.${output.key}`;
        const {setNode} = renderOutputsTab({
            outputs: [output],
            dataKey,
            outputMappings: {[output.key]: 'document.fileId'},
        });

        const outputSection = screen.getByRole('region', {name: 'Ergebnis'});
        expect(within(outputSection).getByText(path)).toBeInTheDocument();
        const target = within(outputSection).getByRole('textbox', {name: /^Zielpfad in den Vorgangsdaten/});
        expect(target).toHaveValue('document.fileId');
        expect(within(outputSection).getByText('$.')).toBeInTheDocument();
        await user.click(within(outputSection).getByRole('button', {name: `Pfad in den Elementdaten ${path} kopieren`}));
        expect(copyToClipboardText).toHaveBeenLastCalledWith(path);
        expect(target).toHaveValue('document.fileId');
        expect(setNode).not.toHaveBeenCalled();
    });
});

const resultOutput: ProcessNodeOutput = {
    key: 'result',
    label: 'Ergebnis',
    description: 'Das Ergebnis des Elements.',
    typeDefinition: 'string',
};

const referenceOutput: ProcessNodeOutput = {
    key: 'reference',
    label: 'Referenz',
    description: 'Die zugehörige Referenz.',
    typeDefinition: '{ id: number; title: string }',
};

function renderOutputsTab({
    outputs = [],
    isEditable = true,
    outputMappings = {},
    dataKey = 'copy_field',
}: {
    outputs?: ProcessNodeOutput[];
    isEditable?: boolean;
    outputMappings?: ProcessNodeEntity['outputMappings'];
    dataKey?: string;
} = {}) {
    const node: ProcessNodeEntity = {
        id: 1,
        processId: 2,
        processVersion: 1,
        processNodeDefinitionKey: 'test-node',
        processNodeDefinitionVersion: 1,
        name: null,
        description: null,
        dataKey,
        configuration: {},
        outputMappings,
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

    function Harness() {
        const [currentNode, setCurrentNode] = useState(node);

        return (
            <ProcessNodeEditorProvider value={{
                provider,
                layout: {} as GroupLayout,
                testClaim: null,
                node: currentNode,
                setNode: (nextNode, updateOriginal) => {
                    setNode(nextNode, updateOriginal);
                    setCurrentNode(nextNode);
                },
                isEditable,
                problems: null,
                incomingMetadata: null,
            }}>
                <ProcessNodeEditorOutputsTab/>
            </ProcessNodeEditorProvider>
        );
    }

    render(<Harness/>);

    return {node, setNode};
}
