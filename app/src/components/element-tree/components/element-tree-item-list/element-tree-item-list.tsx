import React from 'react';
import { ElementType } from '../../../../data/element-type/element-type';
import { Box, Typography } from '@mui/material';
import { ElementTreeItemDropTarget } from '../element-tree-item-drop-target/element-tree-item-drop-target';
import { type ElementTreeItemListProps } from './element-tree-item-list-props';
import { selectIsDraggingTreeElement } from '../../../../slices/admin-settings-slice';
import { useAppSelector } from '../../../../hooks/use-app-selector';
import { type AnyElementWithChildren } from '../../../../models/elements/any-element-with-children';
import { cloneElement } from '../../../../utils/clone-element';
import { ElementTreeItem } from '../element-tree-item/element-tree-item';
import { type Application } from '../../../../models/entities/application';
import { type Preset } from '../../../../models/entities/preset';

export function ElementTreeItemList<T extends AnyElementWithChildren, E extends Application | Preset>(props: ElementTreeItemListProps<T, E>): JSX.Element {
    const isDraggingTreeElement = useAppSelector(selectIsDraggingTreeElement);

    return (
        <Box
            sx={ {
                paddingLeft: (props.isRootList ?? false) ? '0' : '2em',
            } }
        >
            {
                props.element.children.map((child, index) => (
                    <ElementTreeItemDropTarget
                        key={ child.id }
                        element={ props.element }
                        onDrop={ (droppedElement) => {
                            const updatedChildren = [...props.element.children];
                            // @ts-ignore
                            updatedChildren.splice(index, 0, droppedElement);
                            // @ts-ignore
                            props.onPatch({
                                children: updatedChildren,
                            });
                        } }
                    >
                        <ElementTreeItem
                            parents={ [...props.parents, props.element] }
                            entity={ props.entity }
                            element={ child }
                            onPatch={ (patch) => {
                                const updatedChildren = [...props.element.children];
                                const index = updatedChildren.indexOf(child);
                                if (index >= 0) {
                                    // @ts-ignore
                                    updatedChildren[index] = {
                                        ...child,
                                        ...patch,
                                    };
                                }
                                // @ts-ignore
                                props.onPatch({
                                    children: updatedChildren,
                                });
                            } }
                            onDelete={ () => {
                                const updatedChildren = [...props.element.children];
                                const index = updatedChildren.indexOf(child);
                                if (index >= 0) {
                                    updatedChildren.splice(index, 1);
                                }
                                // @ts-ignore
                                props.onPatch({
                                    children: updatedChildren,
                                });
                            } }
                            onClone={ () => {
                                const updatedChildren = [...props.element.children];
                                const index = updatedChildren.indexOf(child);
                                if (index >= 0) {
                                    const clonedElem = cloneElement(child);
                                    updatedChildren.splice(index, 0, clonedElem);
                                }
                                // @ts-ignore
                                props.onPatch({
                                    children: updatedChildren,
                                });
                            } }
                            editable={ props.editable }
                        />
                    </ElementTreeItemDropTarget>
                ))
            }

            {
                props.element.children.length === 0 &&
                <ElementTreeItemDropTarget
                    element={ props.element }
                    isPlaceholder
                    onDrop={ (droppedElement) => {
                        // @ts-ignore
                        props.onPatch({
                            children: [droppedElement],
                        });
                    } }
                >
                    <Typography
                        sx={ {
                            ml: 6,
                            py: 0.75,
                            fontStyle: 'italic',
                        } }
                    >
                        Noch keine { props.element.type === ElementType.Root ? 'Abschnitte' : 'Elemente' } vorhanden
                    </Typography>
                </ElementTreeItemDropTarget>
            }

            {
                props.element.children.length > 0 &&
                isDraggingTreeElement &&
                <ElementTreeItemDropTarget
                    element={ props.element }
                    isPlaceholder
                    onDrop={ (droppedElement) => {
                        const updatedChildren = [...props.element.children];
                        // @ts-ignore
                        updatedChildren.push(droppedElement);
                        // @ts-ignore
                        props.onPatch({
                            children: updatedChildren,
                        });
                    } }
                >
                    <Typography
                        sx={ {
                            ml: 6,
                            py: 0.75,
                            fontStyle: 'italic',
                        } }
                    >
                        Ans Ende anfügen
                    </Typography>
                </ElementTreeItemDropTarget>
            }
        </Box>
    );
}
