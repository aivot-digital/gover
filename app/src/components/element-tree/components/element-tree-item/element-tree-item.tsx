import React, {useEffect, useState} from 'react';
import {useDrag} from 'react-dnd';
import {Box} from '@mui/material';
import {setIsDraggingTreeElement} from '../../../../slices/admin-settings-slice';
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

export function ElementTreeItem<T extends AnyElement>({
                                                          parents,
                                                          element,
                                                          onPatch,
                                                          onDelete,
                                                          onClone
                                                      }: ElementTreeItemProps<T>) {
    const dispatch = useAppDispatch();

    const [expanded, setExpanded] = useState(false);
    const [showEditor, setShowEditor] = useState(false);
    const [showAddDialog, setShowAddDialog] = useState(false);

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
            setShowAddDialog(false);
            setExpanded(true);
        }
    };

    const handleDeleteElement = () => {
        const directUsages = findNoCodeUsage(element, parents[0]);
        if (directUsages.length > 0) {
            alert(`Dieses Element kann nicht gelöscht werden. Es wird aktuell von den folgenden Elementen referenziert: ${directUsages.map(u => generateComponentTitle(u)).join(', ')}`);
            return
        }

        if (isAnyElementWithChildren(element)) {
            const childUsages = findNoCodeUsageOfChildren(element, parents[0]);
            if (childUsages.length > 0) {
                alert(`Dieses Element kann nicht gelöscht werden. Mindestens eins der Kind-Elemente wird von einer No-Code-Funktion referenziert.`);
                return;
            }
        }

        onDelete();
        setShowEditor(false);
    };

    const isLayoutElement = isAnyElementWithChildren(element);

    return (
        <Box
            ref={drag}
            sx={{opacity: isDragging ? 0 : 1}}
        >
            <ElementTreeItemTitle
                isExpanded={expanded}
                onToggleExpanded={isLayoutElement ? () => setExpanded(!expanded) : undefined}
                element={element}
                onShowAddDialog={isLayoutElement ? () => setShowAddDialog(true) : undefined}
                onSelect={() => {
                    setShowEditor(true);
                }}
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
                    onClose={() => setShowAddDialog(false)}
                />
            }

            {
                showEditor &&
                <ElementEditor
                    parents={parents}
                    element={element}
                    onSave={(update) => {
                        onPatch(update);
                        setShowEditor(false);
                    }}
                    onCancel={() => {
                        setShowEditor(false);
                    }}
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
                        setShowEditor(false);
                    }}
                />
            }
        </Box>
    );
}
