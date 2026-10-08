import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {expect, it, vi} from 'vitest';
import {ProcessFlowEditorNodeHandle} from './process-flow-editor-node-handle';

vi.mock('../../../../../../providers/confirm-provider', () => ({useConfirm: () => async () => true}));
vi.mock('@xyflow/react', () => ({
    Position: {Top: 'top', Bottom: 'bottom'},
    Handle: ({isConnectable}: {isConnectable: boolean}) => <div data-testid="source-handle" data-connectable={isConnectable}/>,
}));

it('allows deleting an unavailable connection and prevents new connections through its anchor', async () => {
    const onDeleteEdge = vi.fn();
    const onConnect = vi.fn();
    const port = {key: 'removed', label: 'Nicht verfügbar: removed', description: 'Ausgang nicht mehr verfügbar.'};
    render(<ProcessFlowEditorNodeHandle editable isConnected unavailable port={port} onClick={onConnect}
                                        onConnectToExisting={onConnect} onDeleteEdge={onDeleteEdge} wasPerformed={false}/>);
    expect(screen.getByTestId('source-handle')).toHaveAttribute('data-connectable', 'false');
    expect(screen.queryByRole('button', {name: 'Element hinzufügen'})).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', {name: /Nicht verfügbar/}));
    await userEvent.keyboard('{Delete}');
    await waitFor(() => expect(onDeleteEdge).toHaveBeenCalledWith(port));
    expect(onConnect).not.toHaveBeenCalled();
});
