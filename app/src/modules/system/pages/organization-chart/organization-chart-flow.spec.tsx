import {type PropsWithChildren} from 'react';
import {act, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {type ElkNode} from 'elkjs/lib/elk.bundled.js';
import {ElkLoadError} from '../../../../utils/elk-loader';
import {OrganizationChartFlow} from './organization-chart-flow';
import {type OrganizationChartDepartmentItem} from './organization-chart-types';

const {getElk, fitView} = vi.hoisted(() => ({getElk: vi.fn(), fitView: vi.fn()}));

vi.mock('../../../../utils/elk-loader', async (importOriginal) => ({
    ...await importOriginal<typeof import('../../../../utils/elk-loader')>(),
    createElkLoader: () => getElk,
}));
vi.mock('@xyflow/react', async (importOriginal) => ({
    ...await importOriginal<typeof import('@xyflow/react')>(),
    ReactFlowProvider: ({children}: PropsWithChildren) => children,
    ReactFlow: ({nodes}: {nodes: {id: string; data: {item: {name: string}}}[]}) => (
        <ul aria-label="Organigramm">{nodes.map((node) => <li key={node.id}>{node.data.item.name}</li>)}</ul>
    ),
    useReactFlow: () => ({fitView}),
}));

function department(id: number, name: string): OrganizationChartDepartmentItem {
    return {
        id, name, depth: 0, created: '', updated: '', color: '#123456',
        children: [], members: [], canReadDetails: false, canReadMemberships: false,
    };
}

const elk = {
    layout: async (graph: ElkNode) => ({
        ...graph,
        children: graph.children?.map((node) => ({...node, x: 10, y: 20})),
    }),
};

describe('OrganizationChartFlow loading', () => {
    beforeEach(() => {
        getElk.mockReset();
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
    });

    it('shows a local download error and restores the diagram after retry', async () => {
        getElk.mockRejectedValueOnce(new ElkLoadError(new Error('offline'))).mockResolvedValue(elk);
        render(<OrganizationChartFlow view="departments" rootDepartments={[department(1, 'Bürgerbüro')]} teams={[]} canReadUsers={false}/>);

        expect(await screen.findByRole('alert')).toHaveTextContent('Die Darstellung des Organigramms konnte nicht geladen werden.');
        await userEvent.click(screen.getByRole('button', {name: 'Erneut versuchen'}));

        expect(await screen.findByText('Bürgerbüro')).toBeVisible();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(getElk).toHaveBeenCalledTimes(2);
    });

    it('ignores an obsolete download failure after the input has changed', async () => {
        let rejectFirst!: (error: unknown) => void;
        getElk.mockReturnValueOnce(new Promise((_, reject) => { rejectFirst = reject; })).mockResolvedValue(elk);
        const {rerender} = render(<OrganizationChartFlow view="departments" rootDepartments={[department(1, 'Alt')]} teams={[]} canReadUsers={false}/>);
        await waitFor(() => expect(getElk).toHaveBeenCalledTimes(1));

        rerender(<OrganizationChartFlow view="departments" rootDepartments={[department(2, 'Neu')]} teams={[]} canReadUsers={false}/>);
        expect(await screen.findByText('Neu')).toBeVisible();
        await act(async () => { rejectFirst(new ElkLoadError(new Error('offline'))); });

        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(screen.getByText('Neu')).toBeVisible();
        expect(screen.queryByText('Alt')).not.toBeInTheDocument();
    });
});
