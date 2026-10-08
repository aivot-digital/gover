import {useState} from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {type GroupLayout} from '../../../../../../../models/elements/form/layout/group-layout';
import {type ProcessNodeEntity} from '../../../../../entities/process-node-entity';
import {ProcessNodeApiService} from '../../../../../services/process-node-api-service';
import {ProcessNodeType, type ProcessNodeProvider} from '../../../../../services/process-node-provider-api-service';
import {ProcessNodeEditorProvider} from '../process-node-editor-context';
import {ProcessNodeEditorMoreTab} from './process-node-editor-more-tab';

vi.mock('../../../../../../../components/rich-text-input-component/rich-text-input-component', () => ({
    RichTextInputComponent: () => null,
}));

describe('process node deadline strictness', () => {
    it('defaults to soft and enables the checkbox once a deadline is entered', async () => {
        const {setNode} = renderMoreTab();
        const checkbox = screen.getByRole('checkbox', {name: /^Strikt/});
        expect(checkbox).not.toBeChecked();
        expect(checkbox).toBeDisabled();
        expect(checkbox).toHaveAccessibleDescription(/Nur bei einer strikten Frist/);

        fireEvent.change(screen.getByRole('textbox', {name: /Frist/}), {target: {value: '3'}});
        expect(checkbox).toBeEnabled();
        await userEvent.click(checkbox);
        expect(checkbox).toBeChecked();
        expect(setNode).toHaveBeenLastCalledWith(expect.objectContaining({timeLimitDays: 3, timeLimitStrict: true}), false);

        await userEvent.click(checkbox);
        expect(setNode).toHaveBeenLastCalledWith(expect.objectContaining({timeLimitDays: 3, timeLimitStrict: false}), false);
    });

    it('retains strictness when the deadline is cleared and restores it when a deadline is reentered', () => {
        const {setNode} = renderMoreTab({timeLimitDays: 3, timeLimitStrict: true});
        const input = screen.getByRole('textbox', {name: /Frist/});
        const checkbox = screen.getByRole('checkbox', {name: /^Strikt/});
        fireEvent.change(input, {target: {value: ''}});
        expect(checkbox).toBeChecked();
        expect(checkbox).toBeDisabled();
        expect(setNode).toHaveBeenLastCalledWith(expect.objectContaining({timeLimitDays: null, timeLimitStrict: true}), false);

        fireEvent.change(input, {target: {value: '2'}});
        expect(checkbox).toBeEnabled();
        expect(checkbox).toBeChecked();
    });

    it('disables deadline settings when the node cannot be edited', () => {
        renderMoreTab({timeLimitDays: 3, timeLimitStrict: true}, false);
        expect(screen.getByRole('textbox', {name: /Frist/})).toBeDisabled();
        expect(screen.getByRole('checkbox', {name: /^Strikt/})).toBeDisabled();
    });

    it('does not show deadline settings for non-action nodes', () => {
        renderMoreTab({}, true, ProcessNodeType.Trigger);
        expect(screen.queryByRole('textbox', {name: /Frist/})).not.toBeInTheDocument();
        expect(screen.queryByRole('checkbox', {name: /^Strikt/})).not.toBeInTheDocument();
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
    function Harness() {
        const [currentNode, setCurrentNode] = useState(node);
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
    return {setNode};
}
