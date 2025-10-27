import React, {useEffect, useReducer, useState} from 'react';
import {useDrag} from 'react-dnd';
import {Box} from '@mui/material';
import {selectUseTestMode, setDraggingTreeElement, setExpandElementTree} from '../../slices/admin-settings-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {ElementTreeItemTitle} from '../element-tree-item-title/element-tree-item-title';
import {ElementTreeItemList} from '../element-tree-item-list/element-tree-item-list';
import {ElementEditor} from '../element-editor/element-editor';
import {type ElementTreeItemProps} from './element-tree-item-props';
import {type AnyElementWithChildren, isAnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {type AnyElement} from '../../models/elements/any-element';
import {AddElementDialog} from '../../dialogs/add-element-dialog/add-element-dialog';
import {ElementType} from '../../data/element-type/element-type';
import {findNoCodeUsage, findUsageOfChild} from '../../utils/find-no-code-usage';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {isChildOf} from '../../utils/is-child-of';
import {useAppSelector} from '../../hooks/use-app-selector';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {useElementEditorNavigation} from '../../hooks/use-element-editor-navigation';
import {DefaultTabs} from '../element-editor/default-tabs';

export function ElementTreeItem<T extends AnyElement, E extends ElementTreeEntity>(props: ElementTreeItemProps<T, E>) {
    const {
        element,
        lockMessage,
    } = props;

    const {
        currentEditedElementId,
        navigateToElementEditor,
        closeElementEditor,
    } = useElementEditorNavigation();

    const dispatch = useAppDispatch();
    const expandStatus = useAppSelector((state) => state.adminSettings.expandElementTree);
    const testMode = useAppSelector(selectUseTestMode);

    const [expanded, setExpanded] = useState(false);
    const [showAddDialog, toggleShowAddDialog] = useReducer((p) => !p, false);

    const isLayoutElement = isAnyElementWithChildren(props.element);
    const isNotStoreModule = !(props.element.type === ElementType.Container && props.element.storeLink != null);

    useEffect(() => {
        if (isLayoutElement) {
            if (expandStatus === 'expanded') {
                setExpanded(true);
            } else if (expandStatus === 'collapsed') {
                setExpanded(false);
            }
        }
    }, [expandStatus]);

    useEffect(() => {
        // Check if the current edited element is a child of this element
        function checkChildRecursively(element: AnyElement): boolean {
            if (isAnyElementWithChildren(element)) {
                if (element.children?.some((child) => child.id === currentEditedElementId)) {
                    return true;
                }
                return element.children?.some((child) => checkChildRecursively(child)) ?? false;
            }
            return false;
        }

        if (currentEditedElementId && isLayoutElement && !expanded) {
            if (checkChildRecursively(props.element)) {
                setExpanded(true);
            }
        }
    }, [currentEditedElementId]);

    const [{isDragging}, drag] = useDrag(() => ({
        item: element,
        type: element.type.toString(),
        collect: (monitor) => ({
            isDragging: monitor.isDragging(),
        }),
        end: (element, monitor) => {
            dispatch(setDraggingTreeElement(undefined));
        },
    }), [element]);

    useEffect(() => {
        if (isDragging) {
            dispatch(setDraggingTreeElement(element));
        }
    }, [dispatch, isDragging, element]);

    const handleAddElement = (addedElement: AnyElement): void => {
        if (isAnyElementWithChildren(props.element)) {
            props.onPatch({
                ...props.element,
                children: [
                    ...props.element.children ?? [],
                    addedElement,
                ],
            }, {});
            toggleShowAddDialog();
            if (!expanded) {
                setExpanded(true);
            }
        }
    };

    const handleDeleteElement = (): void => {
        const directUsages = findNoCodeUsage(props.element, props.parents[0]);

        if (directUsages.length > 0 && !(directUsages.length === 1 && directUsages[0].id === props.element.id)) {
            alert(`Dieses Element kann nicht gelöscht werden. Es wird aktuell von den folgenden Elementen referenziert: ${directUsages.map((u) => generateComponentTitle(u)).join(', ')}`);
            return;
        }

        if (isAnyElementWithChildren(props.element)) {
            const childUsages = findUsageOfChild(props.element, props.parents[0]);
            if (childUsages.length > 0) {
                const allReferencesAreChildrenOfElement = childUsages.every(([_, referencingElements]) => referencingElements.every((e) => isChildOf(e, props.element as AnyElementWithChildren)));
                if (!allReferencesAreChildrenOfElement) {
                    alert('Dieses Element kann nicht gelöscht werden. Mindestens eins der Kind-Elemente wird von einer No-Code-Funktion referenziert.');
                    return;
                }
            }
        }

        props.onDelete();
    };

    if (isDragging) {
        return null;
    }

    return (
        <Box
            ref={props.editable && props.disableDrag !== true ? (drag as any) : undefined}
            sx={{
                opacity: isDragging ? 0 : 1,
            }}
        >
            <ElementTreeItemTitle
                isExpanded={expanded}
                onToggleExpanded={
                    isLayoutElement ?
                        () => {
                            dispatch(setExpandElementTree(undefined));
                            setExpanded(!expanded);
                        } :
                        undefined
                }
                element={props.element}
                onShowAddDialog={isLayoutElement && isNotStoreModule ? toggleShowAddDialog : undefined}
                onSelect={() => {
                    navigateToElementEditor(props.element.id, testMode ? DefaultTabs.test : DefaultTabs.properties);
                }}
                editable={props.editable}
                enabledIdentityProviderInfos={props.enabledIdentityProviderInfos}
            />

            {
                expanded &&
                isAnyElementWithChildren(props.element) &&
                <ElementTreeItemList
                    parents={props.parents}
                    entity={props.entity}
                    element={props.element}
                    onPatch={props.onPatch}
                    onMove={props.onMove}
                    editable={props.editable && isNotStoreModule}
                    scope={props.scope}
                    enabledIdentityProviderInfos={props.enabledIdentityProviderInfos}
                    limitElementTypes={props.limitElementTypes}
                    lockMessage={isNotStoreModule ? undefined : 'Kind-Element von Store-Modulen können nicht bearbeitet werden.'}
                />
            }

            <AddElementDialog
                show={showAddDialog}
                parentType={props.element.type}
                onAddElement={handleAddElement}
                onClose={toggleShowAddDialog}
                limitElementTypes={props.limitElementTypes}
                hideGoverStore={props.scope === 'data_modelling'}
                hidePresets={props.scope === 'data_modelling'}
            />

            <ElementEditor
                open={currentEditedElementId === props.element.id}
                parents={props.parents}
                element={props.element}
                entity={props.entity}
                onSave={(updatedElement, updatedApplication) => {
                    props.onPatch(updatedElement, updatedApplication);
                    closeElementEditor();
                }}
                onCancel={() => {
                    closeElementEditor();
                }}
                onDelete={(
                    props.element.type === ElementType.IntroductionStep ||
                    props.element.type === ElementType.SummaryStep ||
                    props.element.type === ElementType.SubmitStep
                ) ?
                    undefined :
                    handleDeleteElement}
                onClone={(
                    props.element.type === ElementType.IntroductionStep ||
                    props.element.type === ElementType.SummaryStep ||
                    props.element.type === ElementType.SubmitStep
                ) ?
                    undefined :
                    () => {
                        props.onClone();
                        closeElementEditor();
                    }}
                editable={props.editable}
                scope={props.scope}
                rootEditor={false}
                lockMessage={lockMessage}
            />
        </Box>
    );
}
