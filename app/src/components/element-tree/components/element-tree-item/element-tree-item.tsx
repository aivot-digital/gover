import React, {useEffect, useReducer, useState} from 'react';
import {useDrag} from 'react-dnd';
import {Box} from '@mui/material';
import {setExpandElementTree, setIsDraggingTreeElement} from '../../../../slices/admin-settings-slice';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {ElementTreeItemTitle} from '../element-tree-item-title/element-tree-item-title';
import {ElementTreeItemList} from '../element-tree-item-list/element-tree-item-list';
import {ElementEditor} from '../element-editor/element-editor';
import {ElementTreeItemProps} from './element-tree-item-props';
import {isAnyElementWithChildren} from '../../../../models/elements/any-element-with-children';
import {AnyElement} from '../../../../models/elements/any-element';
import {AddElementDialog} from '../../../../dialogs/add-element-dialog/add-element-dialog';
import {ElementType} from '../../../../data/element-type/element-type';
import {findNoCodeUsage, findNoCodeUsageOfChildren} from "../../../../utils/find-no-code-usage";
import {generateComponentTitle} from "../../../../utils/generate-component-title";
import {isChildOf} from "../../../../utils/is-child-of";
import {useAppSelector} from "../../../../hooks/use-app-selector";

export function ElementTreeItem<T extends AnyElement>({
                                                          parents,
                                                          element,
                                                          onPatch,
                                                          onDelete,
                                                          onClone,
                                                      }: ElementTreeItemProps<T>
) {
    const dispatch = useAppDispatch();
    const expandStatus = useAppSelector(state => state.adminSettings.expandElementTree);

    const [expanded, toggleExpanded] = useReducer(p => !p, false);
    const [showEditor, toggleShowEditor] = useReducer(p => !p, false);
    const [showAddDialog, toggleShowAddDialog] = useReducer(p => !p, false);

    const isLayoutElement = isAnyElementWithChildren(element);

    useEffect(() => {
        if (expandStatus === 'expanded' && !expanded) {
            toggleExpanded();
        } else if (expandStatus === 'collapsed' && expanded) {
            toggleExpanded();
        }
    }, [expandStatus]);

    useEffect(() => {
        if (expanded && expandStatus === 'collapsed') {
            dispatch(setExpandElementTree(undefined));
        } else if (!expanded && expandStatus === 'expanded') {
            dispatch(setExpandElementTree(undefined));
        }
    },[expanded]);

    const [{isDragging}, drag] = useDrag(() => ({
        item: element,
        type: element.type.toString(),
        collect: (monitor) => ({
            isDragging: monitor.isDragging(),
        }),
        end: (element, monitor) => {
            if (!monitor.didDrop()) {
                onPatch({});
            }
            dispatch(setIsDraggingTreeElement(false));
        },
    }), [element]);

    useEffect((() => {
        if (isDragging) {
            dispatch(setIsDraggingTreeElement(isDragging));
            onDelete();
        }
    }), [dispatch, isDragging, onDelete]);

    const handleAddElement = (addedElement: AnyElement) => {
        if (isAnyElementWithChildren(element)) {
            onPatch({
                ...element,
                children: [
                    ...element.children,
                    addedElement,
                ],
            } as any)
            toggleShowAddDialog();
            if (!expanded) {
                toggleExpanded();
            }
        }
    };

    const handleDeleteElement = () => {
        const directUsages = findNoCodeUsage(element, parents[0]);

        if (directUsages.length > 0 && !(directUsages.length === 1 && directUsages[0].id === element.id)) {
            alert(`Dieses Element kann nicht gelöscht werden. Es wird aktuell von den folgenden Elementen referenziert: ${directUsages.map(u => generateComponentTitle(u)).join(', ')}`);
            return;
        }

        if (isAnyElementWithChildren(element)) {
            const childUsages = findNoCodeUsageOfChildren(element, parents[0]);
            if (childUsages.length > 0) {
                const allReferencesArechildrenOfElement = childUsages.every(([_, referencingElements]) => referencingElements.every(e => isChildOf(e, element)));
                if (!allReferencesArechildrenOfElement) {
                    alert(`Dieses Element kann nicht gelöscht werden. Mindestens eins der Kind-Elemente wird von einer No-Code-Funktion referenziert.`);
                    return;
                }
            }
        }

        onDelete();
        toggleShowEditor();
    };

    return (
        <Box
            ref={drag}
            sx={{opacity: isDragging ? 0 : 1}}
        >
            <ElementTreeItemTitle
                isExpanded={expanded}
                onToggleExpanded={isLayoutElement ? toggleExpanded : undefined}
                element={element}
                onShowAddDialog={isLayoutElement ? toggleShowAddDialog : undefined}
                onSelect={toggleShowEditor}
            />

            {
                isLayoutElement &&
                expanded &&
                <ElementTreeItemList
                    parents={parents}
                    element={element}
                    onPatch={onPatch}
                />
            }

            {
                showAddDialog &&
                <AddElementDialog
                    parentType={element.type}
                    onAddElement={handleAddElement}
                    onClose={toggleShowAddDialog}
                />
            }

            {
                showEditor &&
                <ElementEditor
                    parents={parents}
                    element={element}
                    onSave={(update) => {
                        onPatch(update);
                        toggleShowEditor();
                    }}
                    onCancel={toggleShowEditor}
                    onDelete={(
                        element.type === ElementType.IntroductionStep ||
                        element.type === ElementType.SummaryStep ||
                        element.type === ElementType.SubmitStep
                    ) ? undefined : handleDeleteElement}
                    onClone={(
                        element.type === ElementType.IntroductionStep ||
                        element.type === ElementType.SummaryStep ||
                        element.type === ElementType.SubmitStep
                    ) ? undefined : () => {
                        onClone();
                        toggleShowEditor();
                    }}
                />
            }
        </Box>
    );
}