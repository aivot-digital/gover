import {AnyElement} from '../../../models/elements/any-element';
import {Box, Paper, Typography} from '@mui/material';
import {ELEMENT_TREE_DND_ITEM_TYPE, useElementTreeContext} from '../element-tree-context';
import React, {useEffect, useMemo, useState} from 'react';
import {getElementIconForType} from '../../../data/element-type/element-icons';
import {AnyElementWithChildren, isAnyElementWithChildren} from '../../../models/elements/any-element-with-children';
import {ElementTreeChildList} from './element-tree-child-list';
import {useDrag} from 'react-dnd';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {Actions} from '../../actions/actions';
import ChevronRight from '@aivot/mui-material-symbols-400-outlined/dist/chevron-right/ChevronRight';
import {
    createElementEditorNavigationLink,
    useElementEditorNavigation,
} from '../../../hooks/use-element-editor-navigation';
import {ElementTreeEditor} from './element-tree-editor';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import {checkId} from '../../../utils/id-utils';
import ReportOutlinedIcon from '@mui/icons-material/ReportOutlined';
import BuildCircleOutlinedIcon from '@mui/icons-material/BuildCircleOutlined';
import {AnyInputElement, isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {Action} from '../../actions/actions-props';
import {DefaultTabs} from '../../element-editor/default-tabs';
import {ElementWithParents} from '../../../utils/flatten-elements';
import OfflineBoltOutlinedIcon from '@mui/icons-material/OfflineBoltOutlined';

interface ElementTreeItemProps<T extends AnyElement> {
    parents: Array<AnyElement>;
    value: T;
    onChange: (value: T) => void;
    onDelete: (value: T) => void;
    isDraggable: boolean;
}

export function ElementTreeItem<T extends AnyElement>(props: ElementTreeItemProps<T>) {
    const {
        parents,
        value,
        onChange,
        onDelete,
        isDraggable,
    } = props;

    const {
        id: valueId,
        type,
    } = value;

    const {
        root,
        editable,
        expandCommand,
        activeSearchResultPath,
        allElements,
    } = useElementTreeContext();

    const {
        navigateToElementEditor,
        currentEditedElementId,
        currentEditorTab,
        closeElementEditor,
    } = useElementEditorNavigation();

    const Icon = useMemo(() => {
        return getElementIconForType(type);
    }, [type]);

    const title = useMemo(() => {
        return generateComponentTitle(value);
    }, [value]);

    const parentPath = useMemo(() => {
        return parents.map((element) => element.id);
    }, [parents]);

    const pathParts = useMemo(() => {
        return [
            ...parentPath,
            valueId,
        ];
    }, [parentPath, valueId]);

    const path = useMemo(() => {
        return pathParts.join('.');
    }, [pathParts]);

    const [{isDragging}, drag] = useDrag(() => ({
        type: ELEMENT_TREE_DND_ITEM_TYPE,
        canDrag: editable && isDraggable,
        item: {
            id: valueId,
            type: type,
            path: pathParts,
            parentPath: parentPath,
        },
        collect: (monitor) => ({
            isDragging: monitor.isDragging(),
        }),
    }), [editable, isDraggable, valueId, type, pathParts, parentPath]);

    const [isCollapsed, setIsCollapsed] = useState(true);

    const isHighlighted = useMemo(() => {
        return currentEditedElementId === valueId;
    }, [currentEditedElementId, valueId]);

    const isActiveSearchResult = useMemo(() => {
        if (activeSearchResultPath == null || activeSearchResultPath.length !== pathParts.length) {
            return false;
        }

        return activeSearchResultPath.every((segment, index) => segment === pathParts[index]);
    }, [activeSearchResultPath, pathParts]);

    useEffect(() => {
        if (!isAnyElementWithChildren(value) || expandCommand.type == null) {
            return;
        }

        if (expandCommand.type === 'collapse-all') {
            setIsCollapsed(true);
            return;
        }

        if (expandCommand.type === 'expand-all') {
            setIsCollapsed(false);
            return;
        }

        if (expandCommand.type === 'expand-to-path' && expandCommand.targetPath != null) {
            const isParentOfTarget = (
                pathParts.length < expandCommand.targetPath.length &&
                expandCommand.targetPath.slice(0, pathParts.length).every((segment, index) => segment === pathParts[index])
            );

            if (isParentOfTarget) {
                setIsCollapsed(false);
            }
        }
    }, [expandCommand, pathParts, value]);

    useEffect(() => {
        if (currentEditedElementId != null &&
            isAnyElementWithChildren(value) &&
            checkIfChildExists(value, currentEditedElementId)) {
            setIsCollapsed(false);
        }
    }, [currentEditedElementId]);

    const icons: Action[] = useMemo(() => {
        const leadingIcons: Action[] = getIcons(root, value, allElements);
        const trailingIcons: Action[] = isAnyElementWithChildren(value) ? [
            {
                icon: <ChevronRight fontSize="small"/>,
                tooltip: isCollapsed ? 'Ausklappen' : 'Einklappen',
                onClick: () => {
                    setIsCollapsed((prev => !prev));
                },
                visible: isAnyElementWithChildren(value),
            },
        ] : [];

        return [
            ...leadingIcons,
            ...(leadingIcons.length > 0 && trailingIcons.length > 0 ? ['separator'] : []) as Action[],
            ...trailingIcons,
        ];
    }, [value]);

    return (
        <>
            <Paper
                component="button"
                ref={(element) => {
                    if (element == null || !editable || !isDraggable) {
                        return;
                    }
                    drag(element);
                }}
                data-element-path={path}
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    cursor: editable && isDraggable ? 'grab' : 'default',
                    opacity: isDragging ? 0.45 : 1,
                    borderColor: (isActiveSearchResult || isHighlighted) ? 'primary.main' : undefined,
                    backgroundColor: (isActiveSearchResult || isHighlighted) ? 'action.selected' : undefined,
                    borderRadius: 2,
                    px: 1,
                    py: 0.5,
                    width: '100%',
                }}
                variant="outlined"
                onDoubleClick={() => {
                    navigateToElementEditor(value.id);
                }}
            >
                <Icon fontSize="small"/>

                <Typography
                    sx={{
                        ml: 2,
                    }}
                >
                    {title}
                </Typography>

                <Actions
                    sx={{
                        ml: 'auto',
                    }}
                    actions={icons}
                    dense={true}
                    size="small"
                />
            </Paper>

            {
                isAnyElementWithChildren(value) &&
                !isCollapsed &&
                <Box
                    sx={{
                        pl: 4,
                    }}
                >
                    <ElementTreeChildList
                        parents={[
                            ...parents,
                            value,
                        ]}
                        value={value.children ?? []}
                        onChange={(changedChildren) => {
                            onChange({
                                ...value,
                                children: changedChildren,
                            } as T);
                        }}
                    />
                </Box>
            }

            <ElementTreeEditor
                open={currentEditedElementId === value.id && currentEditorTab != null}
                parents={parents}
                value={value}
                onChange={(updatedValue) => {
                    onChange(updatedValue);
                    closeElementEditor();
                }}
                onCancel={closeElementEditor}
                onDelete={() => {
                    closeElementEditor();
                    onDelete(value);
                }}
            />
        </>
    );
}

function checkIfChildExists(element: AnyElementWithChildren, childId: string): boolean {
    if (element.children == null) {
        return false;
    }

    return element.children.some(child => child.id === childId || (isAnyElementWithChildren(child) && checkIfChildExists(child, childId)));
}

function getIcons<T extends AnyElement>(root: AnyElement, element: T, allElements: ElementWithParents[]): Action[] {
    const actions: Action[] = [];

    if (isAnyInputElement(element) && element.technical) {
        actions.push({
            icon: <VisibilityOffOutlinedIcon/>,
            tooltip: 'Technisches Feld (im Formular nicht sichtbar)',
            to: createElementEditorNavigationLink(element.id, DefaultTabs.properties),
        });
    }

    const msg = checkId(root, element.id);
    if (msg != null) {
        actions.push({
            icon: <ReportOutlinedIcon color="error"/>,
            tooltip: msg,
            to: createElementEditorNavigationLink(element.id, DefaultTabs.properties),
        });
    }

    const referencesToThisElement = allElements
        .map(({element}) => ({
            element: element,
            targets: [
                ...element.visibility?.referencedIds ?? [],
                ...element.override?.referencedIds ?? [],
                ...(element as AnyInputElement).value?.referencedIds ?? [],
                ...(element as AnyInputElement).validation?.referencedIds ?? [],
            ],
        }))
        .filter(ref => ref.targets.includes(element.id))
        .map(({element}) => generateComponentTitle(element))
        .join(', ');
    if (referencesToThisElement.length > 0) {
        actions.push({
            icon: <OfflineBoltOutlinedIcon/>,
            tooltip: 'Von Element(en) referenziert: ' + referencesToThisElement,
            onClick: () => {

            },
        });
    }

    if (element.visibility?.type != null) {
        actions.push({
            icon: <BuildCircleOutlinedIcon/>,
            tooltip: 'Sichtbarkeit definiert',
            to: createElementEditorNavigationLink(element.id, DefaultTabs.visibility),
        });
    }

    if (element.override?.type != null) {
        actions.push({
            icon: <BuildCircleOutlinedIcon/>,
            tooltip: 'Dynamische Struktur definiert',
            to: createElementEditorNavigationLink(element.id, DefaultTabs.patch),
        });
    }

    if (isAnyInputElement(element)) {
        if (element.validation?.type != null) {
            actions.push({
                icon: <BuildCircleOutlinedIcon/>,
                tooltip: 'Validierung definiert',
                to: createElementEditorNavigationLink(element.id, DefaultTabs.validation),
            });
        }

        if (element.value?.type != null) {
            actions.push({
                icon: <BuildCircleOutlinedIcon/>,
                tooltip: 'Dynamischer Wert definiert',
                to: createElementEditorNavigationLink(element.id, DefaultTabs.value),
            });
        }
    }

    /* TODO: Find IdentityProviderMappings for this element.
    if (isAnyInputElement(element)) {
        const mappedIdentityProviders: string[] = [];
        for (const identityProviderInfo of enabledIdentityProviderInfos) {
            const isMapped = getMetadataMapping(element, identityProviderInfo.metadataIdentifier) != null;
            if (isMapped) {
                mappedIdentityProviders.push(identityProviderInfo.name);
            }
        }
        if (mappedIdentityProviders.length > 0) {
            icons.push({
                icon: <AccountCircleOutlinedIcon/>,
                tooltip: 'Verknüpfung mit Nutzerkontenanbieter vorhanden: ' + mappedIdentityProviders.join(', '),
            });
        }
    }

     */

    return actions;
}
