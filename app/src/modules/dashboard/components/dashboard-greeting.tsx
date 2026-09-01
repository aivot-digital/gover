import Logout from '@aivot/mui-material-symbols-400-n25-outlined/Logout';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectUser} from '../../../slices/user-slice';
import {Permission} from '../../../data/permissions/permission';
import {useHasSystemPermission} from '../../permissions/hooks/use-permissions';
import {type Action} from '../../../components/actions/actions-props';
import {useLogout} from '../../../hooks/use-logout';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';

function getGreeting(hour: number): string {
    if (hour >= 23 || hour < 4) return 'Willkommen zu später Stunde';
    if (hour >= 4 && hour < 11) return 'Guten Morgen';
    if (hour >= 11 && hour < 18) return 'Guten Tag';
    return 'Guten Abend';
}

export function DashboardGreeting() {
    const user = useAppSelector(selectUser);
    const logout = useLogout();
    const canConfigureDashboard = useHasSystemPermission(Permission.SYSTEM_CONFIG_READ);
    const firstName = user?.firstName?.trim();
    const greeting = getGreeting(new Date().getHours());
    const title = firstName ? `${greeting}, ${firstName}` : greeting;
    const actions: Action[] = [
        ...(canConfigureDashboard ? [{
            icon: ModuleIcons.dashboardSettings,
            tooltip: 'Übersicht konfigurieren',
            ariaLabel: 'Übersicht konfigurieren',
            to: '/settings/dashboard',
        } satisfies Action, 'separator' as const] : []),
        {
            icon: <Logout/>,
            tooltip: 'Abmelden',
            ariaLabel: 'Abmelden',
            onClick: () => void logout(),
        },
    ];

    return <GenericPageHeader title={title} icon={ModuleIcons.dashboard} actions={actions}/>;
}
