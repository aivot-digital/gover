import {AnyElement} from '../../../models/elements/any-element';
import {Box, Divider, ListItemIcon, ListItemText, Menu, MenuItem, Paper, Typography} from '@mui/material';
import {ELEMENT_TREE_DND_ITEM_TYPE, useElementTreeContext} from '../element-tree-context';
import React, {useEffect, useMemo, useRef, useState} from 'react';
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
import {ELEMENT_TREE_LAYOUT} from '../element-tree-layout';
import ContentPaste from '@mui/icons-material/ContentPaste';
import ContentCopy from '@mui/icons-material/ContentCopy';
import DeleteOutline from '@mui/icons-material/DeleteOutline';
import {copyToClipboardText} from '../../../utils/copy-to-clipboard';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useConfirm} from '../../../providers/confirm-provider';
import {isSectionElementType} from '../../../models/elements/steps/step-element';

interface ElementTreeItemProps<T extends AnyElement> {
    parents: Array<AnyElement>;
    value: T;
    onChange: (value: T) => void;
    onDelete: (value: T) => void;
    onClone: (value: T) => void;
    isDraggable: boolean;
}

export function ElementTreeItem<T extends AnyElement>(props: ElementTreeItemProps<T>) {
    const showConfirm = useConfirm();
    const dispatch = useAppDispatch();
    const menuPaperRef = useRef<HTMLDivElement | null>(null);

    const {
        parents,
        value,
        onChange,
        onDelete,
        onClone,
        isDraggable,
    } = props;

    const {
        id: valueId,
        type,
    } = value;

    const {
        root,
        editable,
        scrollToElement,
        expandCommand,
        activeSearchResultPath,
        highlightedElementId,
        highlightedElementSignal,
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
    const [contextMenuPosition, setContextMenuPosition] = useState<{ mouseX: number; mouseY: number } | null>(null);
    const lastHandledExpandCommandVersionRef = useRef(0);

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

        if (expandCommand.version <= lastHandledExpandCommandVersionRef.current) {
            return;
        }

        lastHandledExpandCommandVersionRef.current = expandCommand.version;

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

    useEffect(() => {
        if (highlightedElementId !== valueId || highlightedElementSignal == null) {
            return;
        }

        const frameId = window.requestAnimationFrame(() => {
            scrollToElement(path);
        });

        return () => {
            window.cancelAnimationFrame(frameId);
        };
    }, [highlightedElementId, highlightedElementSignal, path, scrollToElement, valueId]);

    const icons: Action[] = useMemo(() => {
        const leadingIcons: Action[] = getIcons(root, value, allElements);
        const trailingIcons: Action[] = isAnyElementWithChildren(value) ? [
            {
                icon: (
                    <ChevronRight
                        fontSize="small"
                        sx={{
                            transform: isCollapsed ? 'rotate(0deg)' : 'rotate(90deg)',
                            transition: 'transform 120ms ease',
                        }}
                    />
                ),
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
    }, [allElements, isCollapsed, root, value]);

    const backgroundColor = isActiveSearchResult ?
        'action.focus' :
        (isHighlighted ? 'action.selected' : 'background.paper');
    const hoverBackgroundColor = isActiveSearchResult ?
        'action.focus' :
        (isHighlighted ? 'action.selected' : 'action.hover');
    const treeItemLabel = isSectionElementType(type) ? 'Abschnitt' : 'Element';

    const handleContextMenuOpen = (event: React.MouseEvent<HTMLDivElement>) => {
        event.preventDefault();
        event.stopPropagation();
        setContextMenuPosition({
            mouseX: event.clientX + 2,
            mouseY: event.clientY - 6,
        });
    };

    const handleContextMenuClose = () => {
        setContextMenuPosition(null);
    };

    const handleCopyId = async () => {
        const success = await copyToClipboardText(value.id);
        if (success) {
            dispatch(showSuccessSnackbar('Element-ID in Zwischenablage kopiert'));
        } else {
            dispatch(showErrorSnackbar('Element-ID konnte nicht in Zwischenablage kopiert werden'));
        }

        handleContextMenuClose();
    };

    const handleCloneElement = () => {
        if (!editable) {
            return;
        }

        onClone(value);
        handleContextMenuClose();
    };

    const handleDeleteElement = async () => {
        handleContextMenuClose();

        if (!editable) {
            return;
        }

        const confirmed = await showConfirm({
            title: `${treeItemLabel} löschen`,
            confirmButtonText: 'Löschen',
            children: (
                <Typography>
                    {treeItemLabel === 'Abschnitt'
                        ? 'Soll der Abschnitt wirklich gelöscht werden?'
                        : 'Soll das Element wirklich gelöscht werden?'}
                </Typography>
            ),
        });

        if (confirmed) {
            onDelete(value);
        }
    };

    useEffect(() => {
        if (contextMenuPosition == null) {
            return;
        }

        const handleDocumentContextMenu = (event: MouseEvent) => {
            if (menuPaperRef.current?.contains(event.target as Node)) {
                return;
            }

            setContextMenuPosition(null);
        };

        document.addEventListener('contextmenu', handleDocumentContextMenu, true);

        return () => {
            document.removeEventListener('contextmenu', handleDocumentContextMenu, true);
        };
    }, [contextMenuPosition]);

    return (
        <>
            <Paper
                component="div"
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
                    gap: 0.75,
                    minWidth: 0,
                    cursor: editable && isDraggable ? 'grab' : (editable ? 'pointer' : 'default'),
                    opacity: isDragging ? 0.45 : 1,
                    minHeight: `${ELEMENT_TREE_LAYOUT.itemHeightPx}px`,
                    borderColor: (isActiveSearchResult || isHighlighted) ? 'primary.main' : 'divider',
                    backgroundColor: backgroundColor,
                    borderRadius: 1,
                    boxSizing: 'border-box',
                    px: 1,
                    py: 0.5,
                    width: '100%',
                    maxWidth: '100%',
                    overflow: 'hidden',
                    transition: 'background-color 120ms ease, border-color 120ms ease',
                    '&:hover': {
                        backgroundColor: hoverBackgroundColor,
                    },
                }}
                variant="outlined"
                onDoubleClick={() => {
                    navigateToElementEditor(value.id);
                }}
                onContextMenu={handleContextMenuOpen}
            >
                <Box
                    sx={{
                        width: 24,
                        display: 'flex',
                        justifyContent: 'center',
                        flexShrink: 0,
                    }}
                >
                    <Icon sx={{fontSize: 20}}/>
                </Box>

                <Box
                    sx={{
                        minWidth: 0,
                        flex: 1,
                    }}
                >
                    <Typography
                        variant="body2"
                        title={title}
                        sx={{
                            fontWeight: 500,
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                        }}
                    >
                        {title}
                    </Typography>
                </Box>

                <Actions
                    sx={{
                        ml: 'auto',
                        flexShrink: 0,
                        '& .MuiIconButton-root': {
                            p: 0.375,
                        },
                        '& .MuiSvgIcon-root': {
                            fontSize: 20,
                        },
                    }}
                    actions={icons}
                    dense={true}
                    size="small"
                />
            </Paper>

            <Menu
                open={contextMenuPosition != null}
                onClose={handleContextMenuClose}
                anchorReference="anchorPosition"
                anchorPosition={contextMenuPosition == null ? undefined : {
                    top: contextMenuPosition.mouseY,
                    left: contextMenuPosition.mouseX,
                }}
                slotProps={{
                    paper: {
                        ref: menuPaperRef,
                    },
                }}
            >
                <MenuItem onClick={handleCopyId}>
                    <ListItemIcon>
                        <ContentPaste fontSize="small"/>
                    </ListItemIcon>
                    <ListItemText primary={`${treeItemLabel}-ID kopieren`}/>
                </MenuItem>

                <MenuItem
                    onClick={handleCloneElement}
                    disabled={!editable}
                >
                    <ListItemIcon>
                        <ContentCopy fontSize="small"/>
                    </ListItemIcon>
                    <ListItemText primary={`${treeItemLabel} duplizieren`}/>
                </MenuItem>

                <Divider/>

                <MenuItem
                    onClick={handleDeleteElement}
                    disabled={!editable}
                >
                    <ListItemIcon>
                        <DeleteOutline
                            fontSize="small"
                            color="error"
                        />
                    </ListItemIcon>
                    <ListItemText
                        primary={`${treeItemLabel} löschen`}
                        sx={{
                            color: 'error.main',
                        }}
                    />
                </MenuItem>
            </Menu>

            {
                isAnyElementWithChildren(value) &&
                !isCollapsed &&
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
                onClone={() => {
                    onClone(value);
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
