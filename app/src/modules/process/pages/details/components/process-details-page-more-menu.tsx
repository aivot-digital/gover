import {Divider, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
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
            transformOrigin={{
                horizontal: 'left',
                vertical: 'top',
            }}
            PaperProps={{
                elevation: 6,
                sx: {
                    mt: -0.875,
                    ml: 0.5,
                    minWidth: 280,
                    overflow: 'visible',
                    '&::before': {
                        content: '""',
                        position: 'absolute',
                        top: 19,
                        left: 0,
                        width: 10,
                        height: 10,
                        bgcolor: 'background.paper',
                        transform: 'translateX(-50%) rotate(45deg)',
                        boxShadow: '-2px 2px 6px rgba(15, 23, 42, 0.08)',
                        zIndex: 0,
                    },
                },
            }}
            MenuListProps={{
                sx: {
                    py: 1,
                },
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
                                onClick={() => {
                                    dispatchEvent(e.event);
                                }}
                                sx={{
                                    minHeight: 42,
                                    px: 1.5,
                                    gap: 1,
                                }}
                            >
                                <ListItemIcon
                                    sx={{
                                        minWidth: 32,
                                        color: e.isDangerous ? 'error.main' : 'text.secondary',
                                    }}
                                >
                                    {e.icon}
                                </ListItemIcon>
                                <ListItemText
                                    primary={e.label}
                                    primaryTypographyProps={{
                                        color: e.isDangerous ? 'error.main' : 'text.primary',
                                    }}
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
