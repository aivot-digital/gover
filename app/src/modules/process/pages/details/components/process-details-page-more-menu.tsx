import {ListItemAvatar, ListItemText, Menu, MenuItem} from '@mui/material';
import React, {type ReactNode, useMemo} from 'react';
import Divider from '@mui/material/Divider';
import History from '@aivot/mui-material-symbols-400-outlined/dist/history/History';
import Comment from '@aivot/mui-material-symbols-400-outlined/dist/comment/Comment';
import FileExport from '@aivot/mui-material-symbols-400-outlined/dist/file-export/FileExport';
import Science from '@aivot/mui-material-symbols-400-outlined/dist/science/Science';
import BugReport from '@aivot/mui-material-symbols-400-outlined/dist/bug-report/BugReport';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {addSnackbarMessage, SnackbarSeverity, SnackbarType} from '../../../../../slices/shell-slice';

export type ProcessDetailsPageMoreMenuEvent = 'export' | 'test';

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

    const isOpen = useMemo(() => {
        return anchorEl != null;
    }, [anchorEl]);


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

    return (
        <Menu
            open={isOpen}
            anchorEl={anchorEl}
            onClose={onClose}
            anchorOrigin={{
                horizontal: 'right',
                vertical: 'top',
            }}
        >
            {
                entries
                    .map((e, index) => e === 'separator' ?
                        (
                            <Divider key={index.toString()}/>
                        ) :
                        (
                            <MenuItem
                                key={e.label}
                                dense={true}
                                onClick={() => {
                                    dispatchEvent(e.event);
                                }}
                            >
                                <ListItemAvatar>
                                    {e.icon}
                                </ListItemAvatar>
                                <ListItemText
                                    primary={e.label}
                                />
                            </MenuItem>
                        ))
            }
        </Menu>
    );
}


const entries: Array<{
    icon: ReactNode;
    label: string;
    event?: ProcessDetailsPageMoreMenuEvent;
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
    },
];
