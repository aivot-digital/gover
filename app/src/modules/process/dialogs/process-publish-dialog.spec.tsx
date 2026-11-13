import {render, screen, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {type ProcessEntity} from '../entities/process-entity';
import {type ProcessNodeProblems} from '../entities/process-node-problems';
import {type ProcessVersionEntity} from '../entities/process-version-entity';
import {ProcessDefinitionVersionApiService} from '../services/process-definition-version-api-service';
import {ProcessPublishDialog} from './process-publish-dialog';

vi.mock('../../permissions/hooks/use-permissions', () => ({
    useHasProcessPermission: () => true,
}));
vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));
vi.mock('../components/node-problems-alert', () => ({
    NodeProblemsAlert: () => null,
}));

const process = {id: 3, publishedVersion: null} as ProcessEntity;
const version = {processVersion: 2} as ProcessVersionEntity;

describe('ProcessPublishDialog', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('reports the validated node problems once per opening', async () => {
        const nodeProblems = [{node: {id: 42}, problems: ['Öffentlicher Titel fehlt.']}] as ProcessNodeProblems[];
        const validate = vi.spyOn(ProcessDefinitionVersionApiService.prototype, 'validate')
            .mockResolvedValue({versionProblems: [], nodeProblems});
        const onNodeProblems = vi.fn();

        const {rerender} = render(
            <ProcessPublishDialog
                open={true}
                process={process}
                version={version}
                availableNodeProviders={[]}
                onPublish={vi.fn()}
                onNodeProblems={onNodeProblems}
                onClose={vi.fn()}
            />,
        );

        await waitFor(() => expect(onNodeProblems).toHaveBeenCalledWith(nodeProblems));
        expect(screen.getByRole('button', {name: 'Prozessversion veröffentlichen'})).toBeDisabled();

        // A parent re-render passes a new callback, which must not start another validation.
        rerender(
            <ProcessPublishDialog
                open={true}
                process={process}
                version={version}
                availableNodeProviders={[]}
                onPublish={vi.fn()}
                onNodeProblems={vi.fn()}
                onClose={vi.fn()}
            />,
        );

        expect(validate).toHaveBeenCalledTimes(1);
    });
});
