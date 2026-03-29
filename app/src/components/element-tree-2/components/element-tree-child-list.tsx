import {AnyElement} from '../../../models/elements/any-element';
import {Box, Paper, Typography} from '@mui/material';
import {ElementTreeItem} from './element-tree-item';
import Add from '@aivot/mui-material-symbols-400-outlined/dist/add/Add';
import {useMemo, useState} from 'react';
import {useDragLayer, useDrop} from 'react-dnd';
import {
    ELEMENT_TREE_DND_ITEM_TYPE,
    ElementTreeDragItem,
    useElementTreeContext,
} from '../element-tree-context';
import {AddElementDialog} from '../../../dialogs/add-element-dialog/add-element-dialog';
import {useTheme} from '@mui/material/styles';
import {ELEMENT_TREE_LAYOUT} from '../element-tree-layout';

interface ElementTreeChildListProps<T extends AnyElement> {
    parents: Array<AnyElement>;
    value: T[];
    onChange: (value: T[]) => void;
    addNewElementLabel?: string;
}

interface ElementTreeDropSlotProps {
    parentPath: string[];
    targetIndex: number;
    height: number;
}

function ElementTreeDropSlot(props: ElementTreeDropSlotProps) {
    const {
        parentPath,
        targetIndex,
        height,
    } = props;

    const {
        editable,
        canDropElement,
        moveElement,
    } = useElementTreeContext();

    const [{isOver, canDrop}, drop] = useDrop(() => ({
        accept: ELEMENT_TREE_DND_ITEM_TYPE,
        canDrop: (dragItem: ElementTreeDragItem) => {
            if (!editable) {
                return false;
            }

            return canDropElement(dragItem, parentPath, targetIndex);
        },
        drop: (dragItem: ElementTreeDragItem) => {
            moveElement(dragItem, parentPath, targetIndex);
        },
        collect: (monitor) => ({
            isOver: monitor.isOver({shallow: true}),
            canDrop: monitor.canDrop(),
        }),
    }), [editable, canDropElement, moveElement, parentPath, targetIndex]);

    return (
        <Box
            ref={drop as any}
            sx={{
                height: `${height}px`,
                borderTop: '3px solid',
                borderTopColor: (isOver && canDrop) ? 'primary.main' : 'transparent',
                transition: 'height 120ms ease, border-top-color 120ms ease',
            }}
        />
    );
}

export function ElementTreeChildList<T extends AnyElement>(props: ElementTreeChildListProps<T>) {
    const {
        parents,
        value,
        onChange,
        addNewElementLabel = 'Neues Element hinzufügen',
    } = props;
    const theme = useTheme();

    const {
        editable,
        displayContext,
    } = useElementTreeContext();

    const parentPath = useMemo(() => {
        return parents.map((element) => element.id);
    }, [parents]);
    const isRootList = parents.length === 1;
    const showNestedConnectors = !isRootList;
    const isDraggingTreeElement = useDragLayer((monitor) => monitor.getItemType() === ELEMENT_TREE_DND_ITEM_TYPE);
    const connectorColor = theme.palette.mode === 'dark' ? theme.palette.grey[600] : theme.palette.grey[400];
    const dropSlotHeightPx = isDraggingTreeElement ? ELEMENT_TREE_LAYOUT.expandedDropSlotHeightPx : ELEMENT_TREE_LAYOUT.collapsedDropSlotHeightPx;
    const connectorY = dropSlotHeightPx + ELEMENT_TREE_LAYOUT.iconCenterYPx;
    const elbowTop = connectorY - ELEMENT_TREE_LAYOUT.elbowSizePx + 2;
    const elbowLeft = ELEMENT_TREE_LAYOUT.iconCenterXPx - ELEMENT_TREE_LAYOUT.childrenIndentPx;
    const horizontalExtensionLeft = elbowLeft + ELEMENT_TREE_LAYOUT.elbowSizePx - 1;
    const horizontalExtensionWidth = ELEMENT_TREE_LAYOUT.childrenIndentPx - ELEMENT_TREE_LAYOUT.iconCenterXPx - ELEMENT_TREE_LAYOUT.elbowSizePx + 3;

    const [showAddElementDialog, setShowAddElementDialog] = useState(false);

    const entries = value.map((element, index) => ({
        key: element.id,
        targetIndex: index,
        content: (
            <ElementTreeItem
                parents={parents}
                value={element}
                onChange={(updatedElement) => {
                    const updatedValue = [...value];
                    updatedValue[index] = updatedElement;
                    onChange(updatedValue);
                }}
                onDelete={() => {
                    const updatedValue = [...value];
                    updatedValue.splice(index, 1);
                    onChange(updatedValue);
                }}
                isDraggable={true}
            />
        ),
    }));

    if (editable) {
        entries.push({
            key: 'add-element',
            targetIndex: value.length,
            content: (
                <Paper
                    component="button"
                    type="button"
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 0.75,
                        minWidth: 0,
                        px: 1,
                        py: 0.5,
                        minHeight: `${ELEMENT_TREE_LAYOUT.itemHeightPx}px`,
                        cursor: editable ? 'pointer' : 'default',
                        width: '100%',
                        maxWidth: '100%',
                        boxSizing: 'border-box',
                        borderStyle: 'dashed',
                        borderRadius: 1,
                        justifyContent: 'flex-start',
                        textAlign: 'left',
                        appearance: 'none',
                        backgroundColor: 'background.paper',
                        color: 'text.secondary',
                        transition: 'background-color 120ms ease, border-color 120ms ease',
                        '&:hover': {
                            backgroundColor: 'action.hover',
                            borderColor: 'primary.main',
                        },
                    }}
                    variant="outlined"
                    onClick={() => {
                        setShowAddElementDialog(true);
                    }}
                >
                    <Box
                        sx={{
                            width: 24,
                            display: 'flex',
                            justifyContent: 'center',
                            flexShrink: 0,
                        }}
                    >
                        <Add sx={{fontSize: 20}}/>
                    </Box>

                    <Typography
                        variant="body2"
                        sx={{
                            minWidth: 0,
                            fontWeight: 500,
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                        }}
                    >
                        {addNewElementLabel}
                    </Typography>
                </Paper>
            ),
        });
    }

    return (
        <Box
            sx={{
                display: 'grid',
                minWidth: 0,
                gap: `${ELEMENT_TREE_LAYOUT.gapPx}px`,
                ...(showNestedConnectors ?
                    {
                        position: 'relative',
                        pt: `${ELEMENT_TREE_LAYOUT.connectorTopPaddingPx}px`,
                        pl: `${ELEMENT_TREE_LAYOUT.childrenIndentPx}px`,
                        '&::before': {
                            content: '""',
                            position: 'absolute',
                            top: 0,
                            bottom: 0,
                            left: `${ELEMENT_TREE_LAYOUT.iconCenterXPx}px`,
                            width: 2,
                            bgcolor: connectorColor,
                            zIndex: 0,
                        },
                    } :
                    {}),
            }}
        >
            {
                entries.map((entry, index) => (
                    <Box
                        key={entry.key}
                        sx={{
                            position: 'relative',
                            minWidth: 0,
                            zIndex: 1,
                            ...(showNestedConnectors ?
                                {
                                    '&::before': {
                                        content: '""',
                                        position: 'absolute',
                                        left: `${elbowLeft}px`,
                                        top: `${elbowTop}px`,
                                        width: `${ELEMENT_TREE_LAYOUT.elbowSizePx}px`,
                                        height: `${ELEMENT_TREE_LAYOUT.elbowSizePx}px`,
                                        borderLeft: `2px solid ${connectorColor}`,
                                        borderBottom: `2px solid ${connectorColor}`,
                                        borderBottomLeftRadius: 8,
                                        zIndex: 0,
                                    },
                                    '&::after': {
                                        content: '""',
                                        position: 'absolute',
                                        left: `${horizontalExtensionLeft}px`,
                                        top: `${connectorY}px`,
                                        width: `${horizontalExtensionWidth}px`,
                                        height: 2,
                                        borderRadius: 999,
                                        bgcolor: connectorColor,
                                        zIndex: 0,
                                    },
                                } :
                                {}),
                        }}
                    >
                        {
                            showNestedConnectors &&
                            index === entries.length - 1 &&
                            <Box
                                sx={{
                                    position: 'absolute',
                                    left: `${ELEMENT_TREE_LAYOUT.iconCenterXPx - ELEMENT_TREE_LAYOUT.childrenIndentPx}px`,
                                    top: `${connectorY - 2}px`,
                                    width: 2,
                                    bottom: `-${ELEMENT_TREE_LAYOUT.gapPx + 2}px`,
                                    bgcolor: 'background.paper',
                                    zIndex: 2,
                                }}
                            />
                        }

                        <ElementTreeDropSlot
                            parentPath={parentPath}
                            targetIndex={entry.targetIndex}
                            height={dropSlotHeightPx}
                        />

                        {entry.content}
                    </Box>
                ))
            }

            <AddElementDialog
                show={showAddElementDialog}
                parentType={parents[parents.length - 1].type}
                onAddElement={(ele) => {
                    const updatedValue = [...value, ele];
                    onChange(updatedValue as T[]);
                    setShowAddElementDialog(false);
                }}
                onClose={() => {
                    setShowAddElementDialog(false);
                }}
                displayContext={displayContext}
            />
        </Box>
    );
}
