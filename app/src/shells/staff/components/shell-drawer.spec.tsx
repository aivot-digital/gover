import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {MemoryRouter, useNavigate} from 'react-router-dom';
import {ShellDrawer} from './shell-drawer';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectPermissions, selectUser} from '../../../slices/user-slice';
import {selectMinimizeDrawer} from '../../../slices/shell-slice';
import {Permission} from '../../../data/permissions/permission';
import {type PermissionSet} from '../../../modules/permissions/models/permission-set';
import {ProcessInstanceTaskApiService} from '../../../modules/process/services/process-instance-task-api-service';
import {createSimulatedDashboardData} from '../../../modules/dashboard/dashboard-simulation';
import {DashboardActivityPeriod} from '../../../modules/dashboard/models/dashboard-overview';

const {dispatch} = vi.hoisted(() => ({dispatch: vi.fn()}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => dispatch}));
vi.mock('../../../hooks/use-app-selector', () => ({useAppSelector: vi.fn()}));
vi.mock('./shell-user-menu', () => ({ShellUserMenu: () => null}));
vi.mock('./shell-notifications-menu', () => ({ShellNotificationsMenu: () => null}));
vi.mock('./shell-drawer-user-icon', () => ({default: () => null}));
vi.mock('./shell-drawer-logo', () => ({default: () => null}));
vi.mock('../../../components/color-mode-picker/color-mode-picker', () => ({ColorModePicker: () => null}));
vi.mock('simplebar-react', () => ({default: ({children}: {children: React.ReactNode}) => <div>{children}</div>}));

const permissions: PermissionSet = {
    systemPermissions: [{userId: 'demo', permissions: [Permission.PRESET_READ]}],
    departmentPermissions: [], teamPermissions: [], domainPermissions: [], processPermissions: [], processInstancePermissions: [],
};

function Navigation() {
    const navigate = useNavigate();
    return <>
        <button onClick={() => navigate('/?simulate=1')}>Simulation öffnen</button>
        <button onClick={() => navigate('/')}>Simulation verlassen</button>
    </>;
}

function renderDrawer(url: string) {
    return render(<MemoryRouter initialEntries={[url]}><Navigation/><ShellDrawer/></MemoryRouter>);
}

describe('ShellDrawer simulation', () => {
    beforeEach(() => {
        vi.spyOn(ProcessInstanceTaskApiService.prototype, 'getAssignedTaskCount').mockResolvedValue(4);
        vi.mocked(useAppSelector).mockImplementation((selector) => {
            if (selector === selectPermissions) return permissions;
            if (selector === selectUser) return {id: 'demo'};
            if (selector === selectMinimizeDrawer) return false;
            return undefined;
        });
    });

    it('uses the dashboard demo count and resumes live counts when leaving simulation', async () => {
        const {overview} = createSimulatedDashboardData(DashboardActivityPeriod.ThreeMonths);
        renderDrawer('/?simulate=1');
        expect(screen.getByRole('link', {name: `Aufgaben ${overview.tasks.total}`})).toBeVisible();
        expect(ProcessInstanceTaskApiService.prototype.getAssignedTaskCount).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', {name: 'Simulation verlassen'}));
        expect(await screen.findByRole('link', {name: 'Aufgaben 4'})).toBeVisible();
        expect(ProcessInstanceTaskApiService.prototype.getAssignedTaskCount).toHaveBeenCalledTimes(1);

        fireEvent.click(screen.getByRole('button', {name: 'Simulation öffnen'}));
        expect(screen.getByRole('link', {name: `Aufgaben ${overview.tasks.total}`})).toBeVisible();
        expect(ProcessInstanceTaskApiService.prototype.getAssignedTaskCount).toHaveBeenCalledTimes(1);
    });

    it.each(['/', '/?simulate=0', '/?simulate=true'])('keeps unfinished navigation disabled for %s', (url) => {
        renderDrawer(url);
        expect(screen.getByRole('button', {name: 'Vorlagen'})).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('button', {name: 'Marktplatz'})).toHaveAttribute('aria-disabled', 'true');
    });

    it('enables the two entries only while simulation is active', () => {
        renderDrawer('/');
        fireEvent.click(screen.getByRole('button', {name: 'Simulation öffnen'}));
        expect(screen.getByRole('link', {name: 'Vorlagen'})).toHaveAttribute('href', '/presets');
        expect(screen.getByRole('button', {name: 'Marktplatz'})).not.toHaveAttribute('aria-disabled', 'true');
        fireEvent.click(screen.getByRole('button', {name: 'Simulation verlassen'}));
        expect(screen.getByRole('button', {name: 'Vorlagen'})).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('button', {name: 'Marktplatz'})).toHaveAttribute('aria-disabled', 'true');
    });

    it('preserves permission filtering during simulation', () => {
        vi.mocked(useAppSelector).mockReturnValue(undefined);
        renderDrawer('/?simulate=1');
        expect(screen.queryByText('Vorlagen')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Marktplatz'})).not.toHaveAttribute('aria-disabled', 'true');
    });
});
