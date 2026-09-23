import React, {type ReactNode} from 'react';
import History from '@aivot/mui-material-symbols-400-n25-outlined/History';
import Comment from '@aivot/mui-material-symbols-400-n25-outlined/Comment';
import FileExport from '@aivot/mui-material-symbols-400-n25-outlined/FileExport';
import Science from '@aivot/mui-material-symbols-400-n25-outlined/Science';
import BugReport from '@aivot/mui-material-symbols-400-n25-outlined/BugReport';
import {ModuleIcons} from '../../../../../shells/staff/data/module-icons';
import {ProcessActionMenu, type ProcessActionMenuItem} from './process-action-menu';
import {useNotImplemented} from '../../../../../hooks/use-not-implemented';
import Chat from '@aivot/mui-material-symbols-400-n25-outlined/Chat';

export type ProcessDetailsPageMoreMenuEvent = 'export' | 'test' | 'instances' | 'notes' | 'toggle-ai-chat';

interface ProcessDetailsPageMoreMenuProps {
    anchorEl: null | HTMLElement;
    onClose: () => void;
    showAiChat: boolean;
    aiChatVisible: boolean;
    aiChatDisabled: boolean;
    onMenuEvent: (event: ProcessDetailsPageMoreMenuEvent) => void;
}

export function ProcessDetailsPageMoreMenu(props: ProcessDetailsPageMoreMenuProps): ReactNode {
    const {
        anchorEl,
        onClose,
        showAiChat,
        aiChatVisible,
        aiChatDisabled,
        onMenuEvent,
    } = props;

    const notImplemented = useNotImplemented();

    const dispatchEvent = (event: ProcessDetailsPageMoreMenuEvent | undefined): void => {
        if (event != null) {
            onMenuEvent(event);
        } else {
            notImplemented();
        }
    };

    const items: ProcessActionMenuItem[] = entries.map((entry) => {
        if (entry === 'separator') {
            return entry;
        }

        if (entry.type === 'toggle') {
            return {
                type: 'toggle',
                label: entry.label,
                icon: entry.icon,
                checked: showAiChat,
                visible: aiChatVisible,
                disabled: aiChatDisabled,
                onToggle: () => onMenuEvent(entry.event),
            };
        }

        return {
            label: entry.label,
            icon: entry.icon,
            isDangerous: entry.isDangerous,
            onClick: () => {
                dispatchEvent(entry.event);
            },
        };
    });

    return (
        <ProcessActionMenu
            anchorEl={anchorEl}
            onClose={onClose}
            items={items}
        />
    );
}


type ProcessDetailsPageMoreMenuEntry = {
    icon: ReactNode;
    label: string;
    event?: Exclude<ProcessDetailsPageMoreMenuEvent, 'toggle-ai-chat'>;
    isDangerous?: boolean;
    type?: 'action';
} | {
    icon: ReactNode;
    label: string;
    event: 'toggle-ai-chat';
    type: 'toggle';
} | 'separator';

const entries: ProcessDetailsPageMoreMenuEntry[] = [
    {
        icon: <History/>,
        label: 'Änderungsverlauf anzeigen',
    },
    {
        icon: <Comment/>,
        label: 'Übersicht der Notizen anzeigen',
        event: 'notes',
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
    {
        icon: <Chat/>,
        label: 'KI-Chat anzeigen',
        event: 'toggle-ai-chat',
        type: 'toggle',
    },
];
