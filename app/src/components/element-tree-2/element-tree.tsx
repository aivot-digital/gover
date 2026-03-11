import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {Box, Typography} from '@mui/material';
import {DndProvider} from 'react-dnd';
import {HTML5Backend} from 'react-dnd-html5-backend';
import {isRootElement} from '../../models/elements/root-element';
import {AnyElement} from '../../models/elements/any-element';
import {AnyElementWithChildren, isAnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {getElementIconForType} from '../../data/element-type/element-icons';
import {Actions} from '../actions/actions';
import Search from '@aivot/mui-material-symbols-400-outlined/dist/search/Search';
import Expand from '@aivot/mui-material-symbols-400-outlined/dist/expand/Expand';
import CollapseAll from '@aivot/mui-material-symbols-400-outlined/dist/collapse-all/CollapseAll';
import {ElementTreeChildList} from './components/element-tree-child-list';
import {ElementTreeContextProvider, ElementTreeDragItem, ElementTreeExpandCommand} from './element-tree-context';
import {ElementChildOptions} from '../../data/element-type/element-child-options';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {SearchInput} from '../search-input/search-input';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import {flattenElementsWithParents} from '../../utils/flatten-elements';

export interface ElementTreeProps<T extends AnyElement> {
    value: T;
    onChange: (patch: T) => void;
    editable: boolean;
}

interface ElementTreeSearchResult {
    id: string;
    title: string;
    path: string[];
}

export function ElementTree<T extends AnyElement>(props: ElementTreeProps<T>) {
    const {
        value,
        onChange,
        editable,
    } = props;

    const {
        type,
    } = value;

    const [showSearch, setShowSearch] = useState(false);
    const [search, setSearch] = useState('');
    const [currentSearchResultIndex, setCurrentSearchResultIndex] = useState(0);
    const [expandCommand, setExpandCommand] = useState<ElementTreeExpandCommand>({
        type: null,
        version: 0,
    });

    const allElements = useMemo(() => {
        return flattenElementsWithParents(value, [], false);
    }, [value]);

    const Icon = useMemo(() => {
        return getElementIconForType(type);
    }, [type]);

    const title = useMemo(() => {
        return generateComponentTitle(value);
    }, [value]);

    const children = useMemo(() => {
        return isAnyElementWithChildren(value) ? value.children ?? [] : [];
    }, [value]);

    const scrollContainerRef = useRef<HTMLDivElement>(undefined);

    const handleScrollToElement = (elementPath: string) => {
        if (scrollContainerRef.current == null) {
            return;
        }

        const scrollContainer = scrollContainerRef.current;
        const element = scrollContainer.querySelector(`[data-element-path="${elementPath}"]`) as HTMLElement | null;
        if (element == null) {
            return;
        }

        const scrollContainerRect = scrollContainer.getBoundingClientRect();
        const elementRect = element.getBoundingClientRect();
        const centerOffset = elementRect.top - scrollContainerRect.top - (scrollContainer.clientHeight / 2) + (elementRect.height / 2);

        scrollContainer.scrollTo({
            top: scrollContainer.scrollTop + centerOffset,
            behavior: 'smooth',
        });
    };

    const canDropElement = useCallback((dragItem: ElementTreeDragItem, targetParentPath: string[], targetIndex: number) => {
        return canDropElementInTree(value, dragItem, targetParentPath, targetIndex);
    }, [value]);

    const moveElement = useCallback((dragItem: ElementTreeDragItem, targetParentPath: string[], targetIndex: number) => {
        const updatedValue = moveElementInTree(value, dragItem, targetParentPath, targetIndex);
        if (updatedValue !== value) {
            onChange(updatedValue as T);
        }
    }, [value, onChange]);

    const flattenedSearchResults = useMemo(() => {
        if (!isAnyElementWithChildren(value)) {
            return [];
        }
        return flattenTreeForSearch(value, [value.id]);
    }, [value]);

    const normalizedSearch = useMemo(() => {
        return search.trim().toLowerCase();
    }, [search]);

    const searchResults = useMemo(() => {
        if (normalizedSearch.length === 0) {
            return [];
        }

        return flattenedSearchResults.filter((entry) => {
            return (
                entry.id.toLowerCase().includes(normalizedSearch) ||
                entry.title.toLowerCase().includes(normalizedSearch)
            );
        });
    }, [flattenedSearchResults, normalizedSearch]);

    const activeSearchResult = useMemo(() => {
        if (searchResults.length === 0) {
            return null;
        }

        return searchResults[currentSearchResultIndex] ?? searchResults[0];
    }, [searchResults, currentSearchResultIndex]);

    const handleNextSearchResult = () => {
        if (searchResults.length === 0) {
            return;
        }

        setCurrentSearchResultIndex((prev) => (
            (prev + 1) % searchResults.length
        ));
    };

    const handlePreviousSearchResult = () => {
        if (searchResults.length === 0) {
            return;
        }

        setCurrentSearchResultIndex((prev) => (
            (prev - 1 + searchResults.length) % searchResults.length
        ));
    };

    const handleExpandAll = () => {
        setExpandCommand((prev) => ({
            type: 'expand-all',
            version: prev.version + 1,
        }));
    };

    const handleCollapseAll = () => {
        setExpandCommand((prev) => ({
            type: 'collapse-all',
            version: prev.version + 1,
        }));
    };

    useEffect(() => {
        setCurrentSearchResultIndex(0);
    }, [normalizedSearch]);

    useEffect(() => {
        if (searchResults.length === 0) {
            return;
        }

        if (currentSearchResultIndex < searchResults.length) {
            return;
        }

        setCurrentSearchResultIndex(0);
    }, [currentSearchResultIndex, searchResults]);

    useEffect(() => {
        if (activeSearchResult == null) {
            return;
        }

        setExpandCommand((prev) => ({
            type: 'expand-to-path',
            version: prev.version + 1,
            targetPath: activeSearchResult.path,
        }));

        const outerFrameId = window.requestAnimationFrame(() => {
            window.requestAnimationFrame(() => {
                handleScrollToElement(activeSearchResult.path.join('.'));
            });
        });

        return () => {
            window.cancelAnimationFrame(outerFrameId);
        };
    }, [activeSearchResult]);

    return (
        <Box>
            <Box
                sx={{
                    borderBottom: '1px solid #ccc',
                    py: 2,
                    px: 2,
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                    }}
                >
                    <Icon/>

                    <Typography
                        sx={{
                            ml: 1,
                        }}
                    >
                        {title}
                    </Typography>

                    <Actions
                        dense={true}
                        sx={{
                            marginLeft: 'auto',
                        }}
                        actions={[
                            {
                                icon: <Search/>,
                                tooltip: 'Elemente durchsuchen',
                                onClick: () => {
                                    setShowSearch(true);
                                },
                            },
                            {
                                icon: <Expand/>,
                                tooltip: 'Elemente ausklappen',
                                onClick: handleExpandAll,
                            },
                            {
                                icon: <CollapseAll/>,
                                tooltip: 'Elemente einklappen',
                                onClick: handleCollapseAll,
                            },
                        ]}
                    />
                </Box>

                {
                    showSearch &&
                    <Box
                        sx={{
                            display: 'flex',
                            mt: 2,
                        }}
                    >
                        <SearchInput
                            value={search}
                            onChange={setSearch}
                            label="Element suchen"
                            placeholder="Name oder ID eingeben"
                            sx={{
                                flex: 1,
                            }}
                            autoFocus={true}
                        />

                        <Actions
                            dense={true}
                            actions={[
                                {
                                    tooltip: 'Nächstes Element',
                                    icon: <ArrowDownwardIcon/>,
                                    onClick: handleNextSearchResult,
                                    disabled: searchResults.length === 0,
                                },
                                {
                                    tooltip: 'Vorheriges Element',
                                    icon: <ArrowUpwardIcon/>,
                                    onClick: handlePreviousSearchResult,
                                    disabled: searchResults.length === 0,
                                },
                                {
                                    tooltip: 'Suche Schließen',
                                    icon: <CloseOutlinedIcon/>,
                                    onClick: () => {
                                        setShowSearch(false);
                                        setSearch('');
                                        setCurrentSearchResultIndex(0);
                                    },
                                },
                            ]}
                        />
                    </Box>
                }
            </Box>

            <Box
                ref={scrollContainerRef}
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    height: '100%',
                    overflowY: 'auto',
                    px: 2,
                    mt: 2,
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 0.5,
                    }}
                >
                    <DndProvider
                        backend={HTML5Backend}
                    >
                        <ElementTreeContextProvider
                            value={{
                                root: value,
                                editable: editable,
                                scrollToElement: handleScrollToElement,
                                canDropElement: canDropElement,
                                moveElement: moveElement,
                                expandCommand: expandCommand,
                                activeSearchResultPath: activeSearchResult?.path,
                                allElements: allElements,
                            }}
                        >
                            <ElementTreeChildList
                                parents={[value]}
                                value={children}
                                onChange={(changedChildren) => {
                                    onChange({
                                        ...value,
                                        children: changedChildren,
                                    } as T);
                                }}
                                addNewElementLabel={isRootElement(value) ? 'Neuen Abschnitt hinzufügen' : undefined}
                            />
                        </ElementTreeContextProvider>
                    </DndProvider>
                </Box>
            </Box>
        </Box>
    );
}

function canDropElementInTree(
    tree: AnyElement,
    dragItem: ElementTreeDragItem,
    targetParentPath: string[],
    targetIndex: number,
): boolean {
    const sourceInfo = findSourceInTree(tree, dragItem);
    if (sourceInfo == null) {
        return false;
    }

    const targetParent = findElementAtPath(tree, targetParentPath);
    if (targetParent == null || !isAnyElementWithChildren(targetParent)) {
        return false;
    }

    if (isPathPrefix(dragItem.path, targetParentPath)) {
        return false;
    }

    const acceptedChildren = ElementChildOptions[targetParent.type] ?? [];
    if (!acceptedChildren.includes(sourceInfo.element.type)) {
        return false;
    }

    const clampedTargetIndex = clamp(targetIndex, 0, (targetParent.children ?? []).length);
    if (arePathsEqual(sourceInfo.parentPath, targetParentPath)) {
        if (sourceInfo.childIndex === clampedTargetIndex || sourceInfo.childIndex + 1 === clampedTargetIndex) {
            return false;
        }
    }

    return true;
}

function moveElementInTree<T extends AnyElement>(
    tree: T,
    dragItem: ElementTreeDragItem,
    targetParentPath: string[],
    targetIndex: number,
): T {
    if (!canDropElementInTree(tree, dragItem, targetParentPath, targetIndex)) {
        return tree;
    }

    const sourceInfo = findSourceInTree(tree, dragItem);
    if (sourceInfo == null) {
        return tree;
    }

    const sameParent = arePathsEqual(sourceInfo.parentPath, targetParentPath);
    const treeWithoutSource = replaceElementAtPath(tree, sourceInfo.parentPath, (currentParent) => {
        if (!isAnyElementWithChildren(currentParent)) {
            return currentParent;
        }

        const updatedChildren = [...currentParent.children ?? []];
        updatedChildren.splice(sourceInfo.childIndex, 1);

        return {
            ...currentParent,
            children: updatedChildren as any,
        } as AnyElement;
    });

    const targetParent = findElementAtPath(treeWithoutSource, targetParentPath);
    if (targetParent == null || !isAnyElementWithChildren(targetParent)) {
        return tree;
    }

    const adjustedTargetIndex = sameParent && sourceInfo.childIndex < targetIndex ? targetIndex - 1 : targetIndex;
    const clampedTargetIndex = clamp(adjustedTargetIndex, 0, (targetParent.children ?? []).length);

    return replaceElementAtPath(treeWithoutSource, targetParentPath, (currentParent) => {
        if (!isAnyElementWithChildren(currentParent)) {
            return currentParent;
        }

        const updatedChildren = [...currentParent.children ?? []];
        updatedChildren.splice(clampedTargetIndex, 0, sourceInfo.element);

        return {
            ...currentParent,
            children: updatedChildren as any,
        } as AnyElement;
    });
}

function findSourceInTree(tree: AnyElement, dragItem: ElementTreeDragItem): {
    parentPath: string[];
    childIndex: number;
    element: AnyElement;
} | null {
    if (!arePathsEqual(dragItem.path, [...dragItem.parentPath, dragItem.id])) {
        return null;
    }

    const sourceParent = findElementAtPath(tree, dragItem.parentPath);
    if (sourceParent == null || !isAnyElementWithChildren(sourceParent)) {
        return null;
    }

    const sourceChildren = sourceParent.children ?? [];
    const childIndex = sourceChildren.findIndex((child) => child.id === dragItem.id);
    if (childIndex < 0) {
        return null;
    }

    return {
        parentPath: dragItem.parentPath,
        childIndex: childIndex,
        element: sourceChildren[childIndex],
    };
}

function findElementAtPath(tree: AnyElement, path: string[]): AnyElement | null {
    if (path.length === 0 || tree.id !== path[0]) {
        return null;
    }

    let currentElement: AnyElement = tree;
    for (let i = 1; i < path.length; i++) {
        if (!isAnyElementWithChildren(currentElement)) {
            return null;
        }

        const child = (currentElement.children ?? []).find((entry) => entry.id === path[i]);
        if (child == null) {
            return null;
        }

        currentElement = child;
    }

    return currentElement;
}

function replaceElementAtPath<T extends AnyElement>(
    tree: T,
    path: string[],
    updater: (element: AnyElement) => AnyElement,
): T {
    if (path.length === 0 || tree.id !== path[0]) {
        return tree;
    }

    if (path.length === 1) {
        return updater(tree) as T;
    }

    if (!isAnyElementWithChildren(tree)) {
        return tree;
    }

    const children = tree.children ?? [];
    const childIndex = children.findIndex((child) => child.id === path[1]);
    if (childIndex < 0) {
        return tree;
    }

    const childToUpdate = children[childIndex];
    const updatedChild = replaceElementAtPath(childToUpdate, path.slice(1), updater);

    if (updatedChild === childToUpdate) {
        return tree;
    }

    const updatedChildren = [...children];
    updatedChildren[childIndex] = updatedChild;

    return {
        ...tree,
        children: updatedChildren,
    } as T;
}

function arePathsEqual(pathA: string[], pathB: string[]): boolean {
    if (pathA.length !== pathB.length) {
        return false;
    }

    for (let i = 0; i < pathA.length; i++) {
        if (pathA[i] !== pathB[i]) {
            return false;
        }
    }

    return true;
}

function isPathPrefix(prefixPath: string[], fullPath: string[]): boolean {
    if (prefixPath.length > fullPath.length) {
        return false;
    }

    for (let i = 0; i < prefixPath.length; i++) {
        if (prefixPath[i] !== fullPath[i]) {
            return false;
        }
    }

    return true;
}

function clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
}

function flattenTreeForSearch(element: AnyElementWithChildren, parents: string[]): ElementTreeSearchResult[] {
    const results: ElementTreeSearchResult[] = [];

    for (const child of element.children ?? []) {
        const path = [
            ...parents,
            child.id,
        ];

        results.push({
            id: child.id,
            title: generateComponentTitle(child),
            path: path,
        });

        if (isAnyElementWithChildren(child)) {
            results.push(...flattenTreeForSearch(child, path));
        }
    }

    return results;
}
