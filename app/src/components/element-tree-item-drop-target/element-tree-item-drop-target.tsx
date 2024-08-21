import React from 'react';
import {useDispatch, useSelector} from 'react-redux';
import {useDrop} from 'react-dnd';
import {Box} from '@mui/material';
import {type RootState} from '../../store';
import {type AnyElement} from '../../models/elements/any-element';
import {type ElementTreeItemDropTargetProps} from './element-tree-item-drop-target-props';
import {type AnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {ElementChildOptions} from '../../data/element-type/element-child-options';


export function ElementTreeItemDropTarget<T extends AnyElementWithChildren>(props: ElementTreeItemDropTargetProps<T>): JSX.Element {
    const dispatch = useDispatch();
    const adminSettings = useSelector((state: RootState) => state.adminSettings);
    const acceptedChildren: string[] = (ElementChildOptions[props.element.type] ?? []).map((e) => e.toString());

    const [{isOver}, drop] = useDrop(
        () => ({
            accept: acceptedChildren,
            canDrop: (droppedElement: AnyElement, _) => {
                return true; // canDrop(droppedElementPath, path);
            },
            drop: (droppedElement: AnyElement, monitor) => {
                const didDrop = monitor.didDrop();
                if (didDrop) {
                    return;
                }
                props.onDrop(droppedElement);
            },
            collect: (monitor) => {
                return {
                    isOver: /* canDrop(monitor.getItem(), path) && */ monitor.isOver({shallow: true}),
                };
            },
        }),
        [dispatch, acceptedChildren, props.onDrop],
    );

    return (
        <Box
            sx={{
                position: 'relative',
            }}
        >
            {
                adminSettings.isDraggingTreeElement &&
                <Box
                    ref={drop}
                    sx={{
                        position: 'absolute',
                        height: '100%',
                        width: '100%',
                        borderTop: isOver && !props.isPlaceholder ? '4px solid lightgray' : 'none',
                        backgroundColor: isOver && props.isPlaceholder ? '#00000055' : 'transparent',
                        borderRadius: 0.25,
                        fontStyle: 'italic',
                        opacity: 0.5,
                    }}
                >
                </Box>
            }

            {props.children}
        </Box>
    );
}
