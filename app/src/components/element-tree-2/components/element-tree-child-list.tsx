import {AnyElement} from '../../../models/elements/any-element';
import {Box, Button, Paper, Typography} from '@mui/material';
import {ElementTreeItem} from './element-tree-item';
import Add from '@aivot/mui-material-symbols-400-outlined/dist/add/Add';
import {Fragment, useMemo, useState} from 'react';
import {useDrop} from 'react-dnd';
import {
    ELEMENT_TREE_DND_ITEM_TYPE,
    ElementTreeDragItem,
    useElementTreeContext,
} from '../element-tree-context';
import {AddElementDialog} from '../../../dialogs/add-element-dialog/add-element-dialog';

interface ElementTreeChildListProps<T extends AnyElement> {
    parents: Array<AnyElement>;
    value: T[];
    onChange: (value: T[]) => void;
    addNewElementLabel?: string;
}

interface ElementTreeDropSlotProps {
    parentPath: string[];
    targetIndex: number;
}

function ElementTreeDropSlot(props: ElementTreeDropSlotProps) {
    const {
        parentPath,
        targetIndex,
    } = props;

    const {
        editable,
        canDropElement,
        moveElement,
    } = useElementTreeContext();

    const [{isOver, canDrop, isDraggingTreeElement}, drop] = useDrop(() => ({
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
            isDraggingTreeElement: monitor.getItemType() === ELEMENT_TREE_DND_ITEM_TYPE,
        }),
    }), [editable, canDropElement, moveElement, parentPath, targetIndex]);

    return (
        <Box
            ref={drop as any}
            sx={{
                height: editable && isDraggingTreeElement ? 16 : 0,
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

    const parentPath = useMemo(() => {
        return parents.map((element) => element.id);
    }, [parents]);

    const [showAddElementDialog, setShowAddElementDialog] = useState(false);

    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                gap: 1,
            }}
        >
            {
                <ElementTreeDropSlot
                    parentPath={parentPath}
                    targetIndex={0}
                />
            }

            {
                value
                    .map((element, index) => (
                        <Box key={element.id}>
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

                            <ElementTreeDropSlot
                                parentPath={parentPath}
                                targetIndex={index + 1}
                            />
                        </Box>
                    ))
            }

            <Paper
                component={Button}
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    px: 1,
                    py: 0.5,
                    borderStyle: 'dashed',
                    justifyContent: 'flex-start',
                    opacity: 0.5,
                    '&:hover': {
                        opacity: 1,
                    }
                }}
                variant="outlined"
                onClick={() => {
                    setShowAddElementDialog(true);
                }}
            >
                <Add fontSize="small"/>

                <Typography
                    sx={{
                        ml: 2,
                    }}
                >
                    {addNewElementLabel}
                </Typography>
            </Paper>

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
            />
        </Box>
    );
}
