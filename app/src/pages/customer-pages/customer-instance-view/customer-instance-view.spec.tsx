import React from 'react';
import {createTheme, ThemeProvider} from '@mui/material';
import {render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ProcessInstanceStatus} from '../../../modules/process/enums/process-instance-status';
import {ProcessTaskStatus} from '../../../modules/process/enums/process-task-status';
import {
    CustomerTaskViewApiService,
    type ProcessInstanceStatusResponse,
} from './customer-task-view-api-service';
import {CustomerInstanceView} from './customer-instance-view';

const mocks = vi.hoisted(() => ({
    dispatch: vi.fn(),
    navigate: vi.fn(),
    params: {
        instanceAccessKey: 'instance-key',
        taskAccessKey: undefined as string | undefined,
    },
}));

vi.mock('../../../hooks/use-app-selector', () => ({
    useAppSelector: () => undefined,
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...await importOriginal<typeof import('react-router-dom')>(),
    Outlet: ({context}: {context: {taskIsActive: boolean}}) => (
        <div data-task-is-active={String(context.taskIsActive)}>Aufgabenansicht</div>
    ),
    useNavigate: () => mocks.navigate,
    useParams: () => mocks.params,
}));

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => mocks.dispatch,
}));

vi.mock('../../../providers/snackbar-provider', () => ({
    SnackbarProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../../components/logo/logo', () => ({
    Logo: () => null,
}));

vi.mock('../../../components/color-mode-picker/color-mode-picker', () => ({
    ColorModePicker: () => null,
}));

vi.mock('../../../components/prosuna-attribution/prosuna-attribution', () => ({
    ProsunaAttribution: () => null,
}));

vi.mock('../../../dialogs/privacy-dialog/privacy-dialog', () => ({
    PrivacyDialogId: 'privacy',
    PrivacyDialog: ({departmentId}: {departmentId?: number | null}) => (
        <div data-testid="privacy-dialog-department-id">{departmentId}</div>
    ),
}));

vi.mock('../../../dialogs/imprint-dialog/imprint-dialog', () => ({
    ImprintDialogId: 'imprint',
    ImprintDialog: ({departmentId}: {departmentId?: number | null}) => (
        <div data-testid="imprint-dialog-department-id">{departmentId}</div>
    ),
}));

vi.mock('../../../dialogs/accessibility-dialog/accessibility-dialog', () => ({
    AccessibilityDialogId: 'accessibility',
    AccessibilityDialog: ({departmentId}: {departmentId?: number | null}) => (
        <div data-testid="accessibility-dialog-department-id">{departmentId}</div>
    ),
}));

vi.mock('../../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../../modules/process/components/process-instance-status-icon', () => ({
    ProcessInstanceStatusIcon: () => null,
}));

vi.mock('../../../components/loading-placeholder/loading-placeholder', () => ({
    LoadingPlaceholder: () => <div>Lädt</div>,
}));

describe('CustomerInstanceView', () => {
    beforeEach(() => {
        mocks.dispatch.mockReset();
        mocks.navigate.mockReset();
        mocks.params.instanceAccessKey = 'instance-key';
        mocks.params.taskAccessKey = undefined;
    });

    it.each(['light', 'dark'] as const)('uses a neutral full-height surface for task content in %s mode', async mode => {
        mocks.params.taskAccessKey = 'form-task';
        vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus').mockResolvedValue(createStatus([
            createTask('form-task', ProcessTaskStatus.AwaitingCustomer),
        ]));
        const theme = createTheme({palette: {mode, background: {default: '#ffeeee', paper: '#eeffee'}}});
        render(<ThemeProvider theme={theme}><CustomerInstanceView/></ThemeProvider>);

        const task = await screen.findByText('Aufgabenansicht');
        const main = screen.getByRole('main');
        const surfaceColor = mode === 'light' ? '#ffffff' : '#1c1c1c';
        expect(screen.getByRole('banner').firstElementChild).toHaveStyle({
            backgroundColor: surfaceColor,
            borderBottomWidth: '1px',
        });
        expect(screen.getByRole('contentinfo')).toHaveStyle({
            backgroundColor: surfaceColor,
            borderTopWidth: '1px',
            flexShrink: '0',
        });
        expect(main).toContainElement(task);
        expect(main).toHaveStyle({flex: '1 0 auto'});
        expect(main.parentElement).toHaveStyle({
            backgroundColor: mode === 'light' ? '#ffffff' : '#1c1c1c',
            minHeight: '100dvh',
            display: 'flex',
            flexDirection: 'column',
        });
    });

    it('ignores completed history and opens the first active customer task', async () => {
        vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus').mockResolvedValue(createStatus([
            createTask('completed-task', ProcessTaskStatus.Completed),
            createTask('payment-task', ProcessTaskStatus.AwaitingPayment),
            createTask('form-task', ProcessTaskStatus.AwaitingCustomer),
        ]));

        render(<CustomerInstanceView/>);

        await waitFor(() => expect(mocks.navigate).toHaveBeenCalledWith(
            '/process/instance-key/tasks/payment-task',
            {replace: true},
        ));
        expect(screen.queryByText('Aufgabenansicht')).not.toBeInTheDocument();
    });

    it('keeps the route when the selected task is still active', async () => {
        mocks.params.taskAccessKey = 'form-task';
        vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus').mockResolvedValue(createStatus([
            createTask('completed-task', ProcessTaskStatus.Completed),
            createTask('form-task', ProcessTaskStatus.AwaitingCustomer),
        ]));

        render(<CustomerInstanceView/>);

        expect(await screen.findByText('Aufgabenansicht')).toHaveAttribute('data-task-is-active', 'true');
        expect(mocks.navigate).not.toHaveBeenCalled();
    });

    it('keeps a completed customer task route available as history', async () => {
        mocks.params.taskAccessKey = 'completed-task';
        vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus').mockResolvedValue(createStatus([
            createTask('completed-task', ProcessTaskStatus.Completed),
        ]));

        render(<CustomerInstanceView/>);

        expect(await screen.findByText('Aufgabenansicht')).toHaveAttribute('data-task-is-active', 'false');
        expect(mocks.navigate).not.toHaveBeenCalled();
    });

    it('shows the instance page when only task history remains', async () => {
        vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus').mockResolvedValue(createStatus([
            createTask('older-task', ProcessTaskStatus.Completed),
            createTask('newer-task', ProcessTaskStatus.Completed),
        ]));

        render(<CustomerInstanceView/>);

        expect(await screen.findByText('Freuen Sie sich. Es gibt für Sie nichts zu tun!')).toBeInTheDocument();
        expect(mocks.navigate).not.toHaveBeenCalled();
    });

    it('passes the process version department IDs to the dialogs', async () => {
        vi.spyOn(CustomerTaskViewApiService.prototype, 'getInstanceStatus').mockResolvedValue({
            ...createStatus([]),
            privacyDepartmentId: 71,
            imprintDepartmentId: 72,
            accessibilityDepartmentId: 73,
        });

        render(<CustomerInstanceView/>);

        expect(await screen.findByTestId('privacy-dialog-department-id')).toHaveTextContent('71');
        expect(screen.getByTestId('imprint-dialog-department-id')).toHaveTextContent('72');
        expect(screen.getByTestId('accessibility-dialog-department-id')).toHaveTextContent('73');
    });
});

function createStatus(tasks: ProcessInstanceStatusResponse['tasks']): ProcessInstanceStatusResponse {
    return {
        title: 'Testvorgang',
        status: ProcessInstanceStatus.Running,
        statusOverride: '',
        tasks,
        privacyDepartmentId: null,
        imprintDepartmentId: null,
        accessibilityDepartmentId: null,
        legalSupportDepartmentId: null,
        technicalSupportDepartmentId: null,
        theme: {
            primaryColor: '#733635',
            secondaryColor: '#A0C9CB',
            primaryColorDark: null,
            secondaryColorDark: null,
            logoUrl: '/logo.svg',
            logoUrlDark: '/logo-dark.svg',
            faviconUrl: '/favicon.svg',
        },
    };
}

function createTask(accessKey: string, status: ProcessTaskStatus) {
    return {
        accessKey,
        status,
        statusOverride: '',
    };
}
