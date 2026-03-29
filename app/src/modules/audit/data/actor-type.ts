import {SvgIconComponent} from '@mui/icons-material';
import AccountTree from '@mui/icons-material/AccountTree';
import AccountCircle from '@aivot/mui-material-symbols-400-outlined/dist/account-circle/AccountCircle';
import Borg from '@aivot/mui-material-symbols-400-outlined/dist/borg/Borg';

type ActorDefault = 'System' |
    'Process' |
    'User';

const actorTypeDefaultLabels: Record<ActorDefault, string> = {
    'System': 'System',
    'Process': 'Prozess',
    'User': 'Mitarbeiter:in',
};

export function getActorTypeLabel(actorType: ActorDefault | string | null | undefined): string {
    if (!actorType) {
        return '';
    }
    return actorTypeDefaultLabels[actorType as ActorDefault] ?? actorType;
}

const actorDefaultColors: Record<ActorDefault, 'default' | 'success' | 'warning' | 'primary' | 'error' | 'info'> = {
    'System': 'success',
    'Process': 'info',
    'User': 'primary',
};

export function getActorTypeColor(actorType: ActorDefault | string | null | undefined): 'default' | 'success' | 'warning' | 'primary' | 'error' | 'info' {
    if (!actorType) {
        return 'default';
    }
    return actorDefaultColors[actorType as ActorDefault] ?? 'default';
}

const actorDefaultIcons: Record<ActorDefault, SvgIconComponent> = {
    'System': Borg,
    'Process': AccountTree,
    'User': AccountCircle,
};

export function getActorTypeIcon(actorType: ActorDefault | string | null | undefined): SvgIconComponent | null {
    if (!actorType) {
        return null;
    }
    return actorDefaultIcons[actorType as ActorDefault] ?? null;
}
