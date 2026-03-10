import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import SwapHoriz from '@mui/icons-material/SwapHoriz';
import {ProcessActionMenu} from '../../process-action-menu';

interface ProcessNodeEditorMenuProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;

    onReplaceNode: () => void;
    onDeleteNode: () => void;
}

export function ProcessNodeEditorMenu(props: ProcessNodeEditorMenuProps) {
    const {
        anchorEl,
        onClose,

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
                    label: 'Ersetzen',
                    onClick: onReplaceNode,
                    icon: <SwapHoriz/>,
                },
                'separator',
                {
                    label: 'Löschen',
                    onClick: onDeleteNode,
                    icon: <Delete/>,
                    isDangerous: true,
                }
            ]}
        />
    );
}
