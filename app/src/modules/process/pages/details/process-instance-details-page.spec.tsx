import {type PropsWithChildren} from 'react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Actions} from '../../../../components/actions/actions';
import {type GenericPageHeaderProps} from '../../../../components/generic-page-header/generic-page-header-props';
import {type ProcessInstanceDetails} from '../../entities/process-instance-details';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {ProcessTaskStatus} from '../../enums/process-task-status';
import {ProcessInstanceDetailsPage} from './process-instance-details-page';

const state = vi.hoisted(() => ({item: undefined as ProcessInstanceDetails | undefined}));
vi.mock('../../../permissions/hooks/use-permissions', () => ({useRefreshPermissionSet: () => vi.fn()}));
vi.mock('../../../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: PropsWithChildren) => children,
}));
vi.mock('../../../../components/generic-details-page/generic-details-page', () => ({
    GenericDetailsPage: ({header}: {header: (item?: ProcessInstanceDetails) => GenericPageHeaderProps}) => (
        <Actions actions={header(state.item).actions ?? []} />
    ),
}));

function renderPage() {
    return render(
        <MemoryRouter>
            <ProcessInstanceDetailsPage />
        </MemoryRouter>,
    );
}

describe('Instance header active task action', () => {
    beforeEach(() => {
        state.item = {
            instance: {
                ...new ProcessInstanceApiService().initialize(),
                id: 17,
            },
            processName: 'Prüfung',
            departmentId: 1,
            departmentName: null,
            triggerName: 'Start',
            triggerType: null,
            activeTasks: [],
        };
    });

    it('disables the action when no active task exists', () => {
        renderPage();
        expect(screen.getByRole('button', {name: 'Aktive Aufgabe aufrufen'})).toBeDisabled();
    });

    it('links directly to a single active task', () => {
        state.item!.activeTasks = [
            {
                id: 8,
                name: 'Prüfung',
                status: ProcessTaskStatus.Running,
                assignedUserId: null,
            },
        ];
        renderPage();
        expect(screen.getByRole('link', {name: 'Aktive Aufgabe aufrufen'})).toHaveAttribute('href', '/tasks/17/8');
    });

    it('lets users choose when several tasks are active', async () => {
        state.item!.activeTasks = [
            {
                id: 8,
                name: 'Prüfung',
                status: ProcessTaskStatus.Running,
                assignedUserId: null,
            },
            {
                id: 9,
                name: 'Freigabe',
                status: ProcessTaskStatus.Paused,
                assignedUserId: null,
            },
        ];
        const user = userEvent.setup();
        renderPage();
        await user.click(screen.getByRole('button', {name: 'Aktive Aufgabe aufrufen'}));
        expect(screen.getByRole('menu', {name: 'Aktive Aufgabe auswählen'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Prüfung'})).toHaveAttribute('href', '/tasks/17/8');
        expect(screen.getByRole('menuitem', {name: 'Freigabe'})).toHaveAttribute('href', '/tasks/17/9');
    });
});
