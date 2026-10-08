import {useState} from 'react';
import {literalAuthoredValue} from '../../../../../../../models/element-data';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {type GroupLayout} from '../../../../../../../models/elements/form/layout/group-layout';
import {type ProcessNodeEntity} from '../../../../../entities/process-node-entity';
import {ProcessNodeApiService} from '../../../../../services/process-node-api-service';
import {ProcessNodeType, type ProcessNodeProvider} from '../../../../../services/process-node-provider-api-service';
import {ProcessNodeEditorProvider} from '../process-node-editor-context';
import {ProcessNodeEditorMoreTab} from './process-node-editor-more-tab';

vi.mock('../../../../../../../components/rich-text-input-component/rich-text-input-component', () => ({
    RichTextInputComponent: () => null,
}));

const deadline = (key: string, label: string) => ({
    key, label, description: `Zeit für ${label}`,
    port: {key: `${key}_overdue`, label: `Fristüberschreitung: ${label}`, description: 'Fortsetzen', deadlineKey: null},
});
const message = deadline('message', 'Nachrichtenversand');
const customer = deadline('customer', 'Formulareinreichung');
const resolution = (deadlineDefinitions = [message, customer]) => ({deadlineDefinitions, ports: [], problems: []});

beforeEach(() => {
    vi.spyOn(ProcessNodeApiService.prototype, 'resolvePorts').mockResolvedValue(resolution());
});
afterEach(() => vi.restoreAllMocks());

describe('configurable phase deadlines', () => {
    it('defaults to optional soft deadlines and changes each phase independently without updating saved ports', async () => {
        const {setNode} = renderMoreTab();
        const input = await screen.findByRole('textbox', {name: /^Nachrichtenversand/});
        const checkbox = screen.getByRole('checkbox', {name: /^Harte Frist: Nachrichtenversand/});
        expect(checkbox).not.toBeChecked();
        expect(checkbox).toBeDisabled();
        expect(input).toHaveAccessibleDescription(/Ohne Angabe gilt keine Frist/);
        fireEvent.change(input, {target: {value: '3'}});
        await userEvent.click(checkbox);
        expect(setNode).toHaveBeenLastCalledWith(expect.objectContaining({
            deadlines: {message: {days: 3, strict: true}}, ports: [],
        }), false);
        fireEvent.change(screen.getByRole('textbox', {name: /^Formulareinreichung/}), {target: {value: '7'}});
        expect(setNode).toHaveBeenLastCalledWith(expect.objectContaining({
            deadlines: {message: {days: 3, strict: true}, customer: {days: 7, strict: false}}, ports: [],
        }), false);
    });

    it('retains strictness when the duration is cleared and disables the checkbox', async () => {
        const {setNode} = renderMoreTab({deadlines: {message: {days: 3, strict: true}}});
        const input = await screen.findByRole('textbox', {name: /^Nachrichtenversand/});
        const checkbox = screen.getByRole('checkbox', {name: /^Harte Frist: Nachrichtenversand/});
        fireEvent.change(input, {target: {value: ''}});
        expect(checkbox).toBeChecked();
        expect(checkbox).toBeDisabled();
        expect(setNode).toHaveBeenLastCalledWith(expect.objectContaining({deadlines: {message: {days: null, strict: true}}}), false);
        fireEvent.change(input, {target: {value: '2'}});
        expect(checkbox).toBeEnabled();
    });

    it('shows offered deadlines for triggers and respects read-only mode', async () => {
        renderMoreTab({deadlines: {message: {days: 3, strict: true}}}, false, ProcessNodeType.Trigger);
        expect(await screen.findByRole('textbox', {name: /^Nachrichtenversand/})).toBeDisabled();
        expect(screen.getByRole('checkbox', {name: /^Harte Frist: Nachrichtenversand/})).toBeDisabled();
    });

    it('preserves settings for temporarily hidden phases and previews unsaved configuration', async () => {
        const {updateNode} = renderMoreTab({deadlines: {message: {days: 3, strict: true}}});
        await screen.findByRole('textbox', {name: /^Nachrichtenversand/});
        vi.mocked(ProcessNodeApiService.prototype.resolvePorts).mockResolvedValueOnce(resolution([customer]));
        act(() => updateNode({configuration: {executionType: literalAuthoredValue('automatic')}}));
        await waitFor(() => expect(screen.queryByRole('status')).not.toBeInTheDocument());
        expect(screen.queryByRole('textbox', {name: /^Nachrichtenversand/})).not.toBeInTheDocument();
        expect(ProcessNodeApiService.prototype.resolvePorts).toHaveBeenLastCalledWith(expect.objectContaining({
            configuration: {executionType: literalAuthoredValue('automatic')}, deadlines: {message: {days: 3, strict: true}},
        }));
        act(() => updateNode({configuration: {executionType: literalAuthoredValue('manual')}}));
        expect(await screen.findByRole('textbox', {name: /^Nachrichtenversand/})).toHaveValue('3');
        expect(screen.getByRole('checkbox', {name: /^Harte Frist: Nachrichtenversand/})).toBeChecked();
    });

    it('ignores outdated preview responses', async () => {
        let resolveOld!: (value: ReturnType<typeof resolution>) => void;
        vi.mocked(ProcessNodeApiService.prototype.resolvePorts).mockImplementationOnce(() => new Promise((resolve) => { resolveOld = resolve; }));
        const {updateNode} = renderMoreTab();
        vi.mocked(ProcessNodeApiService.prototype.resolvePorts).mockResolvedValueOnce(resolution([customer]));
        act(() => updateNode({configuration: {executionType: literalAuthoredValue('automatic')}}));
        await screen.findByRole('textbox', {name: /^Formulareinreichung/});
        await act(async () => resolveOld(resolution()));
        expect(screen.queryByRole('textbox', {name: /^Nachrichtenversand/})).not.toBeInTheDocument();
    });

    it('shows a recoverable error when preview loading fails', async () => {
        vi.mocked(ProcessNodeApiService.prototype.resolvePorts).mockRejectedValueOnce(new Error('Failed'));
        renderMoreTab();
        expect(await screen.findByRole('alert')).toHaveTextContent('Fristen konnten nicht geladen werden');
    });

    it('has no deadline controls when the provider offers no phases', async () => {
        vi.mocked(ProcessNodeApiService.prototype.resolvePorts).mockResolvedValueOnce(resolution([]));
        renderMoreTab();
        await waitFor(() => expect(screen.queryByRole('status')).not.toBeInTheDocument());
        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    });
});

function renderMoreTab(overrides: Partial<ProcessNodeEntity> = {}, isEditable = true, type = ProcessNodeType.Action) {
    const node = {...ProcessNodeApiService.initialize(), ...overrides};
    const setNode = vi.fn();
    const provider: ProcessNodeProvider = {
        key: 'test.node', componentKey: 'node', componentType: 'ProcessNodeDefinition', componentVersion: '1.0.0',
        majorVersion: 1, parentPluginKey: 'test', name: 'Test', abstractDescription: '', description: '',
        deprecationNotice: null, documentationUrl: null, type, executionTypes: [], outputs: [],
    };
    let updateNode!: (update: Partial<ProcessNodeEntity>) => void;
    function Harness() {
        const [currentNode, setCurrentNode] = useState(node);
        updateNode = (update) => setCurrentNode((current) => ({...current, ...update}));
        return <ProcessNodeEditorProvider value={{
            node: currentNode,
            provider,
            layout: {} as GroupLayout,
            testClaim: null,
            setNode: (next, updateOriginal) => {
                setNode(next, updateOriginal);
                setCurrentNode(next);
            },
            isEditable,
            problems: null,
            incomingMetadata: null,
        }}>
            <ProcessNodeEditorMoreTab/>
        </ProcessNodeEditorProvider>;
    }
    render(<Harness/>);
    return {setNode, updateNode: (update: Partial<ProcessNodeEntity>) => updateNode(update)};
}
