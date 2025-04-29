import React from 'react';
import {ElementType} from '../../data/element-type/element-type';
import {Box, Typography} from '@mui/material';
import {ElementTreeItemDropTarget} from '../element-tree-item-drop-target/element-tree-item-drop-target';
import {type ElementTreeItemListProps} from './element-tree-item-list-props';
import {selectDraggingTreeElement, selectIsDraggingTreeElement} from '../../slices/admin-settings-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {type AnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {cloneElement} from '../../utils/clone-element';
import {ElementTreeItem} from '../element-tree-item/element-tree-item';
import {type ElementTreeEntity} from '../element-tree/element-tree-entity';
import {hasChildElement} from '../../modules/elements/utils/has-child-element';
import {removeChildElement} from '../../modules/elements/utils/remove-child-element';
import {StepElement} from '../../models/elements/steps/step-element';
import {AnyFormElement} from '../../models/elements/form/any-form-element';

export function ElementTreeItemList<T extends AnyElementWithChildren, E extends ElementTreeEntity>(props: ElementTreeItemListProps<T, E>) {
    const {
        element,
        isRootList,
    } = props;

    const draggingTreeElement = useAppSelector(selectDraggingTreeElement);
    const isDraggingTreeElement = useAppSelector(selectIsDraggingTreeElement);

    const handleElementDropAsChildOfThisElement = (droppedElement: StepElement | AnyFormElement, indexOfDrop: number) => {
        // Check if the dropped element is a child of the element
        // If so, only a patch of the current element is needed.
        if (hasChildElement(element, droppedElement)) {
            // Remove the dropped element from the current element
            const thisElementWithoutDroppedElement = removeChildElement(element, droppedElement);

            // Create a copy of the children array
            const updatedChildren = [
                ...thisElementWithoutDroppedElement.children,
            ];

            // Insert the dropped element at the correct position
            updatedChildren.splice(indexOfDrop, 0, droppedElement);

            // Propagate the patch to the parent element
            props.onPatch({
                ...element,
                children: updatedChildren,
            } as Partial<T>, {});
        }
        // If the dropped element is not a child of the element, it is a new child
        else {
            // Create a copy of the children array
            const updatedChildren = [
                ...element.children,
            ];

            // Insert the dropped element at the correct position
            updatedChildren.splice(indexOfDrop, 0, droppedElement);

            // Propagate the patch to the parent element
            props.onMove({
                ...element,
                children: updatedChildren,
            } as Partial<T>, droppedElement);
        }
    };

    return (
        <Box
            sx={{
                paddingLeft: isRootList ? 0 : 4, /* TODO: Check if 4 is correct and equals 2em */
            }}
        >
            {
                (element.children as (StepElement | AnyFormElement)[])
                    .filter((child) => child != null && child !== draggingTreeElement)
                    .map((child, index) => (
                        <ElementTreeItemDropTarget
                            key={child.id}
                            element={props.element}
                            onDrop={(droppedElement) => {
                                handleElementDropAsChildOfThisElement(droppedElement, index);
                            }}
                        >
                            <ElementTreeItem
                                parents={[...props.parents, props.element]}
                                entity={props.entity}
                                element={child}
                                onPatch={(updatedElement, updatedEntity) => {
                                    const updatedChildren = [...props.element.children];
                                    const index = updatedChildren.indexOf(child);
                                    if (index >= 0) {
                                        // @ts-expect-error
                                        updatedChildren[index] = {
                                            ...child,
                                            ...updatedElement,
                                        };
                                    }

                                    // @ts-expect-error
                                    const patch: Partial<T> = {
                                        children: updatedChildren,
                                    };

                                    props.onPatch(patch, updatedEntity);
                                }}
                                onMove={(updatedElement, droppedElement) => {
                                    // Test if the original parent element has the dropped element as a child
                                    if (hasChildElement(props.element, droppedElement)) {
                                        const parentWithoutDroppedElement = removeChildElement(props.element, droppedElement);

                                        parentWithoutDroppedElement.children[index] = {
                                            ...parentWithoutDroppedElement.children[index],
                                            ...updatedElement,
                                        } as any;

                                        props.onPatch(parentWithoutDroppedElement, props.entity);
                                    } else {
                                        const updatedChildren = [...props.element.children];
                                        updatedChildren[index] = updatedElement as any;

                                        props.onMove({
                                            ...props.element,
                                            children: updatedChildren,
                                        } as Partial<T>, droppedElement);
                                    }
                                }}
                                onDelete={() => {
                                    const updatedChildren = [...props.element.children];
                                    const index = updatedChildren.indexOf(child);
                                    if (index >= 0) {
                                        updatedChildren.splice(index, 1);
                                    }

                                    // @ts-expect-error
                                    const patch: Partial<T> = {
                                        children: updatedChildren,
                                    };

                                    props.onPatch(patch, {});
                                }}
                                onClone={() => {
                                    const updatedChildren = [...props.element.children];
                                    const index = updatedChildren.indexOf(child);
                                    if (index >= 0) {
                                        const clonedElem = cloneElement(child);
                                        updatedChildren.splice(index + 1, 0, clonedElem);
                                    }

                                    // @ts-expect-error
                                    const patch: Partial<T> = {
                                        children: updatedChildren,
                                    };

                                    props.onPatch(patch, {});
                                }}
                                editable={props.editable}
                                scope={props.scope}
                            />
                        </ElementTreeItemDropTarget>
                    ))
            }

            {
                props.element.children.length === 0 &&
                <ElementTreeItemDropTarget
                    element={props.element}
                    isPlaceholder
                    onDrop={(droppedElement) => {
                        handleElementDropAsChildOfThisElement(droppedElement, 0);
                    }}
                >
                    <Typography
                        sx={{
                            ml: 6,
                            py: 0.75,
                            fontStyle: 'italic',
                        }}
                    >
                        Noch keine {props.element.type === ElementType.Root ? 'Abschnitte' : 'Elemente'} vorhanden
                    </Typography>
                </ElementTreeItemDropTarget>
            }

            {
                props.element.children.length > 0 &&
                isDraggingTreeElement &&
                <ElementTreeItemDropTarget
                    element={props.element}
                    isPlaceholder
                    onDrop={(droppedElement) => {
                        handleElementDropAsChildOfThisElement(droppedElement, props.element.children.length);
                    }}
                >
                    <Typography
                        sx={{
                            ml: 6,
                            py: 0.75,
                            fontStyle: 'italic',
                        }}
                    >
                        Ans Ende anfügen
                    </Typography>
                </ElementTreeItemDropTarget>
            }
        </Box>
    );
}
