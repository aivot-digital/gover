import React, { useEffect, useReducer } from 'react';
import { useDrag } from 'react-dnd';
import { Box } from '@mui/material';
import { setExpandElementTree, setIsDraggingTreeElement } from '../../../../slices/admin-settings-slice';
import { useAppDispatch } from '../../../../hooks/use-app-dispatch';
import { ElementTreeItemTitle } from '../element-tree-item-title/element-tree-item-title';
import { ElementTreeItemList } from '../element-tree-item-list/element-tree-item-list';
import { ElementEditor } from '../element-editor/element-editor';
import { type ElementTreeItemProps } from './element-tree-item-props';
import {
    AnyElementWithChildren,
    isAnyElementWithChildren,
} from '../../../../models/elements/any-element-with-children';
import { type AnyElement } from '../../../../models/elements/any-element';
import { AddElementDialog } from '../../../../dialogs/add-element-dialog/add-element-dialog';
import { ElementType } from '../../../../data/element-type/element-type';
import { findNoCodeUsage, findNoCodeUsageOfChildren } from '../../../../utils/find-no-code-usage';
import { generateComponentTitle } from '../../../../utils/generate-component-title';
import { isChildOf } from '../../../../utils/is-child-of';
import { useAppSelector } from '../../../../hooks/use-app-selector';

export function ElementTreeItem<T extends AnyElement>(props: ElementTreeItemProps<T>,
): JSX.Element {
    const dispatch = useAppDispatch();
    const expandStatus = useAppSelector((state) => state.adminSettings.expandElementTree);

    const [expanded, toggleExpanded] = useReducer<(f: boolean) => boolean>((p) => !p, false);
    const [showEditor, toggleShowEditor] = useReducer<(f: boolean) => boolean>((p) => !p, false);
    const [showAddDialog, toggleShowAddDialog] = useReducer<(f: boolean) => boolean>((p) => !p, false);

    const isLayoutElement = isAnyElementWithChildren(props.element);

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
    }, [expanded]);

    const [{isDragging}, drag] = useDrag(() => ({
        item: props.element,
        type: props.element.type.toString(),
        collect: (monitor) => ({
            isDragging: monitor.isDragging(),
        }),
        end: (element, monitor) => {
            if (!monitor.didDrop()) {
                props.onPatch({});
            }
            dispatch(setIsDraggingTreeElement(false));
        },
    }), [props.element]);

    useEffect(() => {
        if (isDragging) {
            dispatch(setIsDraggingTreeElement(isDragging));
            props.onDelete();
        }
    }, [dispatch, isDragging, props.onDelete]);

    const handleAddElement = (addedElement: AnyElement): void => {
        if (isAnyElementWithChildren(props.element)) {
            props.onPatch({
                ...props.element,
                children: [
                    ...props.element.children,
                    addedElement,
                ],
            } as any);
            toggleShowAddDialog();
            if (!expanded) {
                toggleExpanded();
            }
        }
    };

    const handleDeleteElement = (): void => {
        const directUsages = findNoCodeUsage(props.element, props.parents[0]);

        if (directUsages.length > 0 && !(directUsages.length === 1 && directUsages[0].id === props.element.id)) {
            alert(`Dieses Element kann nicht gelöscht werden. Es wird aktuell von den folgenden Elementen referenziert: ${ directUsages.map((u) => generateComponentTitle(u)).join(', ') }`);
            return;
        }

        if (isAnyElementWithChildren(props.element)) {
            const childUsages = findNoCodeUsageOfChildren(props.element, props.parents[0]);
            if (childUsages.length > 0) {
                const allReferencesAreChildrenOfElement = childUsages.every(([_, referencingElements]) => referencingElements.every((e) => isChildOf(e, props.element as AnyElementWithChildren)));
                if (!allReferencesAreChildrenOfElement) {
                    alert('Dieses Element kann nicht gelöscht werden. Mindestens eins der Kind-Elemente wird von einer No-Code-Funktion referenziert.');
                    return;
                }
            }
        }

        props.onDelete();
        toggleShowEditor();
    };

    return (
        <Box
            ref={ props.editable ? drag : undefined }
            sx={ {
                opacity: isDragging ? 0 : 1,
            } }
        >
            <ElementTreeItemTitle
                isExpanded={ expanded }
                onToggleExpanded={ isLayoutElement ? toggleExpanded : undefined }
                element={ props.element }
                onShowAddDialog={ isLayoutElement ? toggleShowAddDialog : undefined }
                onSelect={ toggleShowEditor }
                editable={ props.editable }
            />

            {
                isLayoutElement &&
                expanded &&
                <ElementTreeItemList
                    parents={ props.parents }
                    element={ props.element as AnyElementWithChildren }
                    onPatch={ props.onPatch as (update: Partial<AnyElementWithChildren>) => void }
                    editable={ props.editable }
                />
            }

            {
                showAddDialog &&
                <AddElementDialog
                    parentType={ props.element.type }
                    onAddElement={ handleAddElement }
                    onClose={ toggleShowAddDialog }
                />
            }

            {
                showEditor &&
                <ElementEditor
                    parents={ props.parents }
                    element={ props.element }
                    onSave={ (update) => {
                        props.onPatch(update);
                        toggleShowEditor();
                    } }
                    onCancel={ toggleShowEditor }
                    onDelete={ (
                        props.element.type === ElementType.IntroductionStep ||
                        props.element.type === ElementType.SummaryStep ||
                        props.element.type === ElementType.SubmitStep
                    ) ?
                        undefined :
                        handleDeleteElement }
                    onClone={ (
                        props.element.type === ElementType.IntroductionStep ||
                        props.element.type === ElementType.SummaryStep ||
                        props.element.type === ElementType.SubmitStep
                    ) ?
                        undefined :
                        () => {
                            props.onClone();
                            toggleShowEditor();
                        } }
                    editable={ props.editable }
                />
            }
        </Box>
    );
}
