import React from 'react';
import {useDrop} from 'react-dnd';
import {Box} from '@mui/material';
import {type ElementTreeItemDropTargetProps} from './element-tree-item-drop-target-props';
import {type AnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {ElementChildOptions} from '../../data/element-type/element-child-options';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectIsDraggingTreeElement} from '../../slices/admin-settings-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {StepElement} from '../../models/elements/steps/step-element';
import {AnyFormElement} from '../../models/elements/form/any-form-element';

export function ElementTreeItemDropTarget<T extends AnyElementWithChildren>(props: ElementTreeItemDropTargetProps<T>) {
    const dispatch = useAppDispatch();
    const {
        onDrop,
    } = props;

    const isDraggingTreeElement = useAppSelector(selectIsDraggingTreeElement);
    const acceptedChildren: string[] = (ElementChildOptions[props.element.type] ?? []).map((e) => e.toString());

    const [{isOver}, drop] = useDrop(
        () => ({
            accept: acceptedChildren,
            canDrop: (droppedElement: StepElement | AnyFormElement, _) => {
                return true; // canDrop(droppedElementPath, path);
            },
            drop: (droppedElement: StepElement | AnyFormElement, monitor) => {
                onDrop(droppedElement);
            },
            collect: (monitor) => {
                return {
                    isOver: /* canDrop(monitor.getItem(), path) && */ monitor.isOver({shallow: true}),
                };
            },
        }),
        [dispatch, acceptedChildren, onDrop],
    );

    return (
        <Box
            sx={{
                position: 'relative',
            }}
        >
            {
                isDraggingTreeElement &&
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
