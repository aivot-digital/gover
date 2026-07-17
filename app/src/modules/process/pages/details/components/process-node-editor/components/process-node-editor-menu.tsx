import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import Download from '@aivot/mui-material-symbols-400-n25-outlined/Download';
import SwapHoriz from '@aivot/mui-material-symbols-400-n25-outlined/SwapHoriz';
import {ProcessActionMenu} from '../../process-action-menu';

interface ProcessNodeEditorMenuProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;

    editable: boolean;
    onExportNode: () => void;
    onReplaceNode: () => void;
    onDeleteNode: () => void;
}

export function ProcessNodeEditorMenu(props: ProcessNodeEditorMenuProps) {
    const {
        anchorEl,
        onClose,

        editable,
        onExportNode,
        onReplaceNode,
        onDeleteNode,
    } = props;

    return (
        <ProcessActionMenu
            anchorEl={anchorEl}
            onClose={onClose}
            showArrow={false}
            anchorOrigin={{
                horizontal: 'right',
                vertical: 'bottom',
            }}
            transformOrigin={{
                horizontal: 'right',
                vertical: 'top',
            }}
            items={[
                {
                    label: 'Exportieren',
                    onClick: onExportNode,
                    icon: <Download/>,
                },
                {
                    label: 'Ersetzen',
                    onClick: onReplaceNode,
                    icon: <SwapHoriz/>,
                    disabled: !editable,
                },
                'separator',
                {
                    label: 'Löschen',
                    onClick: onDeleteNode,
                    icon: <Delete/>,
                    isDangerous: true,
                    disabled: !editable,
                }
            ]}
        />
    );
}
