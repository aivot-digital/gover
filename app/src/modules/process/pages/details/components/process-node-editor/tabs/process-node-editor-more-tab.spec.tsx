import {useState} from 'react';
import {literalAuthoredValue} from '../../../../../../../models/element-data';
import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
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
        const input = within(await screen.findByRole('group', {name: 'Nachrichtenversand'})).getByRole('textbox', {name: /^Frist/});
        expect(screen.getByRole('heading', {name: 'Fristen', level: 5})).toBeInTheDocument();
        expect(screen.getByRole('heading', {name: 'Anforderungen und Anmerkungen', level: 5})).toBeInTheDocument();
        const checkbox = within(screen.getByRole('group', {name: 'Nachrichtenversand'})).getByRole('checkbox', {name: 'Aufgabe bei Fristüberschreitung beenden'});
        expect(checkbox).not.toBeChecked();
        expect(checkbox).toBeDisabled();
        expect(checkbox).toHaveAccessibleDescription('Legen Sie zunächst eine Frist fest, um diese Option zu aktivieren.');
        expect(input).toHaveAccessibleDescription(/Ohne Angabe gilt keine Frist/);
        fireEvent.change(input, {target: {value: '3'}});
        expect(checkbox).not.toHaveAccessibleDescription(/Legen Sie zunächst eine Frist fest/);
        expect(checkbox).toHaveAccessibleDescription(/Ausgang „Fristüberschreitung: Nachrichtenversand“/);
        const otherPhase = within(screen.getByRole('group', {name: 'Formulareinreichung'}));
        expect(otherPhase.getByRole('checkbox')).toHaveAccessibleDescription('Legen Sie zunächst eine Frist fest, um diese Option zu aktivieren.');
        await userEvent.click(checkbox);
        expect(setNode).toHaveBeenLastCalledWith(expect.objectContaining({
            deadlines: {message: {days: 3, strict: true}}, ports: [],
        }), false);
        fireEvent.change(within(screen.getByRole('group', {name: 'Formulareinreichung'})).getByRole('textbox', {name: /^Frist/}), {target: {value: '7'}});
        expect(setNode).toHaveBeenLastCalledWith(expect.objectContaining({
            deadlines: {message: {days: 3, strict: true}, customer: {days: 7, strict: false}}, ports: [],
        }), false);
    });

    it('retains strictness when the duration is cleared and disables the checkbox', async () => {
        const {setNode} = renderMoreTab({deadlines: {message: {days: 3, strict: true}}});
        const input = within(await screen.findByRole('group', {name: 'Nachrichtenversand'})).getByRole('textbox', {name: /^Frist/});
        const checkbox = within(screen.getByRole('group', {name: 'Nachrichtenversand'})).getByRole('checkbox', {name: 'Aufgabe bei Fristüberschreitung beenden'});
        fireEvent.change(input, {target: {value: ''}});
        expect(checkbox).toBeChecked();
        expect(checkbox).toBeDisabled();
        expect(checkbox).toHaveAccessibleDescription('Legen Sie zunächst eine Frist fest, um diese Option zu aktivieren.');
        expect(setNode).toHaveBeenLastCalledWith(expect.objectContaining({deadlines: {message: {days: null, strict: true}}}), false);
        fireEvent.change(input, {target: {value: '2'}});
        expect(checkbox).toBeEnabled();
    });

    it('shows offered deadlines for triggers and respects read-only mode', async () => {
        renderMoreTab({deadlines: {message: {days: 3, strict: true}}}, false, ProcessNodeType.Trigger);
        expect(within(await screen.findByRole('group', {name: 'Nachrichtenversand'})).getByRole('textbox', {name: /^Frist/})).toBeDisabled();
        expect(within(screen.getByRole('group', {name: 'Nachrichtenversand'})).getByRole('checkbox', {name: 'Aufgabe bei Fristüberschreitung beenden'})).toBeDisabled();
    });

    it('explains the missing duration on hover and pluralizes the day unit', async () => {
        renderMoreTab();
        const input = within(await screen.findByRole('group', {name: 'Nachrichtenversand'})).getByRole('textbox', {name: /^Frist/});
        const group = within(screen.getByRole('group', {name: 'Nachrichtenversand'}));
        await userEvent.hover(group.getByText('Aufgabe bei Fristüberschreitung beenden'));
        expect(await screen.findByRole('tooltip')).toHaveTextContent('Legen Sie zunächst eine Frist fest');
        await userEvent.unhover(group.getByText('Aufgabe bei Fristüberschreitung beenden'));
        fireEvent.change(input, {target: {value: '1'}});
        expect(group.getByText('Tag')).toBeVisible();
        fireEvent.change(input, {target: {value: '2'}});
        expect(group.getByText('Tage')).toBeVisible();
    });

    it('does not blame a missing duration when the configuration is read-only', async () => {
        renderMoreTab({}, false);
        within(await screen.findByRole('group', {name: 'Nachrichtenversand'})).getByRole('textbox', {name: /^Frist/});
        for (const checkbox of screen.getAllByRole('checkbox')) {
            expect(checkbox).toBeDisabled();
            expect(checkbox).not.toHaveAccessibleDescription(/Legen Sie zunächst eine Frist fest/);
        }
    });

    it('preserves settings for temporarily hidden phases and previews unsaved configuration', async () => {
        const {updateNode} = renderMoreTab({deadlines: {message: {days: 3, strict: true}}});
        within(await screen.findByRole('group', {name: 'Nachrichtenversand'})).getByRole('textbox', {name: /^Frist/});
        vi.mocked(ProcessNodeApiService.prototype.resolvePorts).mockResolvedValueOnce(resolution([customer]));
        act(() => updateNode({configuration: {executionType: literalAuthoredValue('automatic')}}));
        await waitFor(() => expect(screen.queryByRole('status')).not.toBeInTheDocument());
        expect(screen.getByRole('heading', {name: 'Fristen', level: 5})).toBeInTheDocument();
        expect(screen.queryByRole('group', {name: 'Nachrichtenversand'})).not.toBeInTheDocument();
        expect(ProcessNodeApiService.prototype.resolvePorts).toHaveBeenLastCalledWith(expect.objectContaining({
            configuration: {executionType: literalAuthoredValue('automatic')}, deadlines: {message: {days: 3, strict: true}},
        }));
        act(() => updateNode({configuration: {executionType: literalAuthoredValue('manual')}}));
        expect(within(await screen.findByRole('group', {name: 'Nachrichtenversand'})).getByRole('textbox', {name: /^Frist/})).toHaveValue('3');
        expect(within(screen.getByRole('group', {name: 'Nachrichtenversand'})).getByRole('checkbox', {name: 'Aufgabe bei Fristüberschreitung beenden'})).toBeChecked();
    });

    it('ignores outdated preview responses', async () => {
        let resolveOld!: (value: ReturnType<typeof resolution>) => void;
        vi.mocked(ProcessNodeApiService.prototype.resolvePorts).mockImplementationOnce(() => new Promise((resolve) => { resolveOld = resolve; }));
        const {updateNode} = renderMoreTab();
        expect(screen.queryByRole('heading', {name: 'Fristen'})).not.toBeInTheDocument();
        expect(screen.getByRole('heading', {name: 'Anforderungen und Anmerkungen', level: 5})).toBeInTheDocument();
        vi.mocked(ProcessNodeApiService.prototype.resolvePorts).mockResolvedValueOnce(resolution([customer]));
        act(() => updateNode({configuration: {executionType: literalAuthoredValue('automatic')}}));
        within(await screen.findByRole('group', {name: 'Formulareinreichung'})).getByRole('textbox', {name: /^Frist/});
        await act(async () => resolveOld(resolution()));
        expect(screen.queryByRole('group', {name: 'Nachrichtenversand'})).not.toBeInTheDocument();
    });

    it('shows a recoverable error when preview loading fails', async () => {
        vi.mocked(ProcessNodeApiService.prototype.resolvePorts).mockRejectedValueOnce(new Error('Failed'));
        renderMoreTab();
        expect(await screen.findByRole('alert')).toHaveTextContent('Fristen konnten nicht geladen werden');
        expect(screen.queryByRole('heading', {name: 'Fristen'})).not.toBeInTheDocument();
        expect(screen.getByRole('heading', {name: 'Anforderungen und Anmerkungen', level: 5})).toBeInTheDocument();
    });

    it('has no deadline controls when the provider offers no phases', async () => {
        vi.mocked(ProcessNodeApiService.prototype.resolvePorts).mockResolvedValueOnce(resolution([]));
        renderMoreTab();
        await waitFor(() => expect(screen.queryByRole('status')).not.toBeInTheDocument());
        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: 'Fristen'})).not.toBeInTheDocument();
        expect(screen.getByRole('heading', {name: 'Anforderungen und Anmerkungen', level: 5})).toBeInTheDocument();
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
            incomingMetadataLoadFailed: false,
            incomingMetadata: null,
        }}>
            <ProcessNodeEditorMoreTab/>
        </ProcessNodeEditorProvider>;
    }
    render(<Harness/>);
    return {setNode, updateNode: (update: Partial<ProcessNodeEntity>) => updateNode(update)};
}
