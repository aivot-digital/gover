import React, {type PropsWithChildren} from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {configureStore} from '@reduxjs/toolkit';
import {Provider} from 'react-redux';
import {createMemoryRouter, RouterProvider} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Permission} from '../../../../data/permissions/permission';
import {setPermissions, setUser, userReducer} from '../../../../slices/user-slice';
import {PermissionApiService} from '../../../permissions/permission-api-service';
import type {PermissionSet} from '../../../permissions/models/permission-set';
import {ProcessTaskStatus} from '../../enums/process-task-status';
import {ProcessInstanceTaskApiService} from '../../services/process-instance-task-api-service';
import {ProcessTaskViewPage, type ProcessTaskDetailsPageItem} from './process-task-view-page';

vi.mock('../../../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: PropsWithChildren) => children,
}));
vi.mock('../../../../components/generic-page-header/generic-page-header', () => ({GenericPageHeader: () => null}));
vi.mock('../../../search/hooks/use-record-recent-search-item', () => ({useRecordRecentSearchItem: () => {}}));
vi.mock('../../../../hooks/use-api', () => {
    const api = {};
    return {useApi: () => api};
});

async function renderPage(status: ProcessTaskStatus) {
    const permissions: PermissionSet = {
        departmentPermissions: [],
        teamPermissions: [],
        domainPermissions: [],
        processPermissions: [],
        processInstancePermissions: [],
        systemPermissions: [{
            userId: 'user',
            permissions: [Permission.PROCESS_INSTANCE_READ, Permission.PROCESS_INSTANCE_EDIT_TASK],
        }],
    };
    const store = configureStore({reducer: {user: userReducer}});
    store.dispatch(setUser({
        id: 'user',
        email: 'user@example.org',
        firstName: 'Test',
        lastName: 'User',
        fullName: 'Test User',
        enabled: true,
        verified: true,
        deletedInIdp: false,
        systemRoleId: null,
    }));
    store.dispatch(setPermissions(permissions));
    vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockResolvedValue(permissions);

    const details: ProcessTaskDetailsPageItem = {
        task: {
            ...new ProcessInstanceTaskApiService().initialize(),
            id: 20,
            processInstanceId: 10,
            status: status,
        },
        instance: null,
        process: null,
        node: {name: 'Prüfung', description: null},
        provider: null,
    };
    const getDetails = vi.spyOn(ProcessInstanceTaskApiService.prototype, 'retrieveDetails').mockResolvedValue(details);
    const router = createMemoryRouter([{
        path: '/tasks/:instanceId/:taskId',
        element: <ProcessTaskViewPage />,
        children: [
            {index: true, element: <div>Allgemeine Angaben</div>},
            {path: 'edit', element: <div>Bearbeitungsansicht</div>},
        ],
    }], {initialEntries: ['/tasks/10/20']});

    render(<Provider store={store}><RouterProvider router={router} /></Provider>);
    await act(async () => {
        await getDetails.mock.results[0]?.value;
    });

    return {router};
}

// A disabled tab takes its accessible name from the tooltip, so locate it by its visible label.
function getEditTab(): HTMLElement {
    const tab = screen.getByText('Aufgabe bearbeiten').closest<HTMLElement>('[role="tab"]');
    expect(tab).not.toBeNull();
    return tab!;
}

describe('Task edit tab availability', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
    });

    it.each([
        ProcessTaskStatus.Running,
        ProcessTaskStatus.InProgress,
        ProcessTaskStatus.AwaitingStaff,
    ])('opens the edit tab for staff status %s', async (status) => {
        const {router} = await renderPage(status);

        const editTab = getEditTab();
        expect(editTab).toHaveAttribute('aria-disabled', 'false');

        await act(async () => {
            fireEvent.click(editTab);
        });

        expect(router.state.location.pathname).toBe('/tasks/10/20/edit');
        expect(screen.getByText('Bearbeitungsansicht')).toBeInTheDocument();
    });

    it.each([
        ProcessTaskStatus.Paused,
        ProcessTaskStatus.AwaitingPayment,
        ProcessTaskStatus.AwaitingCustomer,
        ProcessTaskStatus.Completed,
        ProcessTaskStatus.Aborted,
        ProcessTaskStatus.Failed,
        ProcessTaskStatus.Restarted,
        ProcessTaskStatus.DeadlineExceeded,
    ])('keeps the edit tab disabled for status %s', async (status) => {
        const {router} = await renderPage(status);

        const editTab = getEditTab();
        expect(editTab).toHaveAttribute('aria-disabled', 'true');
        expect(editTab).toHaveAttribute('tabindex', '-1');
        await userEvent.hover(screen.getByText('Aufgabe bearbeiten'));
        expect(await screen.findByRole('tooltip')).toHaveTextContent(
            'Diese Aufgabe kann in ihrem aktuellen Status nicht bearbeitet werden.',
        );

        await act(async () => {
            fireEvent.click(editTab);
        });

        expect(router.state.location.pathname).toBe('/tasks/10/20');
        expect(screen.queryByText('Bearbeitungsansicht')).not.toBeInTheDocument();
    });
});
