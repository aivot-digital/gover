import {Menu} from "../../../../../../../components/menu/menu";
import Delete from "@aivot/mui-material-symbols-400-outlined/dist/delete/Delete";

interface ProcessNodeEditorMenuProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;

    onDeleteNode: () => void;
}

export function ProcessNodeEditorMenu(props: ProcessNodeEditorMenuProps) {
    const {
        anchorEl,
        onClose,

        onDeleteNode,
    } = props;

    return (
        <Menu
            open={anchorEl != null}
            anchorEl={anchorEl}
            onClose={onClose}
            items={[
                {
                    label: 'Löschen',
                    onClick: onDeleteNode,
                    icon: <Delete/>,
                }
            ]}
        />
    );
}