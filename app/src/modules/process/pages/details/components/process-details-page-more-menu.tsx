import React, {type ReactNode, useMemo} from 'react';
import History from '@aivot/mui-material-symbols-400-outlined/dist/history/History';
import Comment from '@aivot/mui-material-symbols-400-outlined/dist/comment/Comment';
import FileExport from '@aivot/mui-material-symbols-400-outlined/dist/file-export/FileExport';
import Science from '@aivot/mui-material-symbols-400-outlined/dist/science/Science';
import BugReport from '@aivot/mui-material-symbols-400-outlined/dist/bug-report/BugReport';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {addSnackbarMessage, SnackbarSeverity, SnackbarType} from '../../../../../slices/shell-slice';
import {ModuleIcons} from '../../../../../shells/staff/data/module-icons';
import {ProcessActionMenu, type ProcessActionMenuItem} from './process-action-menu';

export type ProcessDetailsPageMoreMenuEvent = 'export' | 'test' | 'instances' | 'delete';

interface ProcessDetailsPageMoreMenuProps {
    anchorEl: null | HTMLElement;
    onClose: () => void;

    onMenuEvent: (event: ProcessDetailsPageMoreMenuEvent) => void;
}

export function ProcessDetailsPageMoreMenu(props: ProcessDetailsPageMoreMenuProps): ReactNode {
    const {
        anchorEl,
        onClose,

        onMenuEvent,
    } = props;

    const dispatch = useAppDispatch();

    const dispatchEvent = (event: ProcessDetailsPageMoreMenuEvent | undefined): void => {
        if (event != null) {
            onMenuEvent(event);
        } else {
            dispatch(addSnackbarMessage({
                type: SnackbarType.AutoHiding,
                severity: SnackbarSeverity.Info,
                key: 'not-implemented',
                message: 'Diese Funktion ist noch nicht implementiert.',
            }));
        }
        onClose();
    };

    const items = useMemo<ProcessActionMenuItem[]>(() => {
        return entries.map((entry) => entry === 'separator' ? entry : ({
            label: entry.label,
            icon: entry.icon,
            isDangerous: entry.isDangerous,
            onClick: () => {
                dispatchEvent(entry.event);
            },
        }));
    }, [dispatchEvent]);

    return (
        <ProcessActionMenu
            anchorEl={anchorEl}
            onClose={onClose}
            items={items}
        />
    );
}


const entries: Array<{
    icon: ReactNode;
    label: string;
    event?: ProcessDetailsPageMoreMenuEvent;
    isDangerous?: boolean;
} | 'separator'> = [
    {
        icon: <History/>,
        label: 'Änderungsverlauf anzeigen',
    },
    {
        icon: <Comment/>,
        label: 'Übersicht der Notizen anzeigen',
    },
    'separator',
    {
        icon: <FileExport/>,
        label: 'Prozess exportieren (.json)',
        event: 'export',
    },
    'separator',
    {
        icon: ModuleIcons.submissions,
        label: 'Vorgänge anzeigen',
        event: 'instances',
    },
    {
        icon: <Science/>,
        label: 'Prozessmodellierung testen',
        event: 'test',
    },
    {
        icon: <BugReport/>,
        label: 'Entwicklerwerkzeuge öffnen',
    },
    'separator',
    {
        icon: <Delete/>,
        label: 'Prozess löschen',
        event: 'delete',
        isDangerous: true,
    },
];
