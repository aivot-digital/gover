import React from 'react';
import {ElementType} from '../../../../data/element-type/element-type';
import {Box, Typography} from '@mui/material';
import {ElementTreeItemDropTarget} from '../element-tree-item-drop-target/element-tree-item-drop-target';
import {ElementTreeItem} from '../element-tree-item/element-tree-item';
import {ElementTreeItemListProps} from './element-tree-item-list-props';
import {selectIsDraggingTreeElement} from '../../../../slices/admin-settings-slice';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {AnyElementWithChildren} from '../../../../models/elements/any-element-with-children';
import {cloneElement} from "../../../../utils/clone-element";


export function ElementTreeItemList<T extends AnyElementWithChildren>({
                                                                          element,
                                                                          isRootList,
                                                                          onPatch
                                                                      }: ElementTreeItemListProps<T>) {
    const isDraggingTreeElement = useAppSelector(selectIsDraggingTreeElement);

    return (
        <Box
            sx={{
                paddingLeft: isRootList ? '0' : '2em'
            }}
        >
            {
                element.children.map((child, index) => (
                    <ElementTreeItemDropTarget
                        key={child.id}
                        element={element}
                        onDrop={droppedElement => {
                            const updatedChildren = [...element.children];
                            // @ts-ignore
                            updatedChildren.splice(index, 0, droppedElement);
                            // @ts-ignore
                            onPatch({
                                children: updatedChildren,
                            });
                        }}
                    >
                        <ElementTreeItem
                            element={child}
                            onPatch={patch => {
                                const updatedChildren = [...element.children];
                                const index = updatedChildren.indexOf(child);
                                if (index >= 0) {
                                    // @ts-ignore
                                    updatedChildren[index] = {
                                        ...child,
                                        ...patch,
                                    };
                                }
                                // @ts-ignore
                                onPatch({
                                    children: updatedChildren,
                                });
                            }}
                            onDelete={() => {
                                const updatedChildren = [...element.children];
                                const index = updatedChildren.indexOf(child);
                                if (index >= 0) {
                                    updatedChildren.splice(index, 1);
                                }
                                // @ts-ignore
                                onPatch({
                                    children: updatedChildren,
                                });
                            }}
                            onClone={() => {
                                const updatedChildren = [...element.children];
                                const index = updatedChildren.indexOf(child);
                                if (index >= 0) {
                                    const clonedElem = cloneElement(child);
                                    updatedChildren.splice(index, 0, clonedElem);
                                }
                                // @ts-ignore
                                onPatch({
                                    children: updatedChildren,
                                });
                            }}
                        />
                    </ElementTreeItemDropTarget>
                ))
            }
            {
                element.children.length === 0 &&
                <ElementTreeItemDropTarget
                    element={element}
                    isPlaceholder
                    onDrop={droppedElement => {
                        // @ts-ignore
                        onPatch({
                            children: [droppedElement],
                        });
                    }}
                >
                    <Typography
                        sx={{
                            ml: 6,
                            py: 0.75,
                            fontStyle: 'italic',
                        }}
                    >
                        Noch keine {element.type === ElementType.Root ? 'Abschnitte' : 'Elemente'} vorhanden
                    </Typography>
                </ElementTreeItemDropTarget>
            }
            {
                element.children.length > 0 &&
                isDraggingTreeElement &&
                <ElementTreeItemDropTarget
                    element={element}
                    isPlaceholder
                    onDrop={droppedElement => {
                        const updatedChildren = [...element.children];
                        // @ts-ignore
                        updatedChildren.push(droppedElement);
                        console.log('Dropped at end', droppedElement);
                        // @ts-ignore
                        onPatch({
                            children: updatedChildren,
                        });
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
