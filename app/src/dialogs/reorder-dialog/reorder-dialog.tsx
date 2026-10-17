import {useDrag, useDrop, XYCoord} from 'react-dnd';
import {DialogProps} from '@mui/material/Dialog';
import {Button, Dialog, DialogActions, DialogContent, List, ListItem, ListItemIcon, ListItemText} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {useEffect, useRef, useState} from 'react';
import DragIndicator from '@aivot/mui-material-symbols-400-n25-outlined/DragIndicator';

export interface ReorderItem<T> {
    index: number;
    value: T;
}

interface ReorderDialogProps<T> {
    title: string;
    items: T[];
    getLabel: (item: T) => {
        primary: string;
        secondary?: string;
    };
    onReorder: (items: T[]) => void;
    onClose: () => void;
}

export function ReorderDialog<T>(props: ReorderDialogProps<T> & DialogProps) {
    const {
        title,
        items,
        getLabel,
        onReorder,
        onClose,
        open,
        ...dialogProps
    } = props;

    const [buffer, setBuffer] = useState<ReorderItem<T>[]>([]);
    // The dialog stays mounted while closed, so reopening it must discard an order that was not saved.
    useEffect(() => {
        if (open) {
            setBuffer(items.map((item, index) => ({
                index: index,
                value: item,
            })));
        }
    }, [items, open]);

    const handleMoveItem = (draggedIndex: number, targetIndex: number) => {
        setBuffer((prevBuffer) => {
            const updatedBuffer = [...prevBuffer];
            const [draggedItem] = updatedBuffer.splice(draggedIndex, 1);
            updatedBuffer.splice(targetIndex, 0, draggedItem);
            return updatedBuffer;
        });
    };

    const handleReorder = () => {
        const reorderedItems = buffer.map(item => item.value);
        onReorder(reorderedItems);
    };

    return (
        <Dialog
            {...dialogProps}
            open={open}
            onClose={onClose}
        >
            <DialogTitleWithClose onClose={onClose}>
                {title}
            </DialogTitleWithClose>
            <DialogContent>
                <List>
                    {
                        buffer.map((item, position) => {
                            const {
                                primary,
                                secondary,
                            } = getLabel(item.value);

                            // The original index identifies the item, while moves address its current position.
                            return (
                                <SortableListItem
                                    key={item.index}
                                    index={position}
                                    primary={primary}
                                    secondary={secondary}
                                    onMoveItem={handleMoveItem}
                                />
                            );
                        })
                    }
                </List>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Abbrechen</Button>
                <Button
                    onClick={handleReorder}
                    variant="contained"
                    color="primary"
                >
                    Reihenfolge speichern
                </Button>
            </DialogActions>
        </Dialog>
    );
}

const DragItemType = 'REORDER_ITEM';

interface SortableListItemProps {
    index: number;
    primary: string;
    secondary: string | undefined;
    onMoveItem: (dragIndex: number, hoverIndex: number) => void;
}

interface DragItem {
    index: number;
}

function SortableListItem(props: SortableListItemProps) {
    const {
        index,
        primary,
        secondary,
        onMoveItem,
    } = props;

    const ref = useRef<HTMLLIElement | null>(null);

    const [{}, drop] = useDrop<DragItem, void, {}>({
        accept: DragItemType,
        hover(item: DragItem, monitor) {
            if (!ref.current) {
                return;
            }

            const dragIndex = item.index;
            const hoverIndex = index;

            // Don't replace items with themselves
            if (dragIndex === hoverIndex) {
                return;
            }

            // Determine rectangle on screen
            const hoverBoundingRect = ref.current?.getBoundingClientRect();

            // Get vertical middle
            const hoverMiddleY =
                (hoverBoundingRect.bottom - hoverBoundingRect.top) / 2;

            // Determine mouse position
            const clientOffset = monitor.getClientOffset();

            // Get pixels to the top
            const hoverClientY = (clientOffset as XYCoord).y - hoverBoundingRect.top;

            // Only perform the move when the mouse has crossed half of the items height
            // When dragging downwards, only move when the cursor is below 50%
            // When dragging upwards, only move when the cursor is above 50%

            // Dragging downwards
            if (dragIndex < hoverIndex && hoverClientY < hoverMiddleY) {
                return;
            }

            // Dragging upwards
            if (dragIndex > hoverIndex && hoverClientY > hoverMiddleY) {
                return;
            }

            // Time to actually perform the action
            onMoveItem(dragIndex, hoverIndex);

            // Further hovers during the same drag must start from the item's new position.
            item.index = hoverIndex;
        },
    });

    const [{
        isDragging,
    }, drag] = useDrag<DragItem, void, {
        isDragging: boolean;
    }>(() => ({
        collect: (monitor) => ({
            isDragging: monitor.isDragging(),
        }),
        type: DragItemType,
        item: () => ({
            index,
        }),
    }), [index]);

    drag(drop(ref));

    return (
        <ListItem
            ref={ref}
            component="li"
            sx={{
                cursor: 'grab',
                opacity: isDragging ? 0 : 1,
            }}
        >
            <ListItemIcon>
                <DragIndicator />
            </ListItemIcon>

            <ListItemText
                primary={primary}
                secondary={secondary}
            />
        </ListItem>
    );
}