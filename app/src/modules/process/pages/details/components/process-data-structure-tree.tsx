import {useEffect, useMemo, useState} from 'react';
import {Box, IconButton, Tooltip, Typography} from '@mui/material';
import {RichTreeView} from '@mui/x-tree-view/RichTreeView';
import {treeItemClasses} from '@mui/x-tree-view/TreeItem';
import ChevronRight from '@aivot/mui-material-symbols-400-n25-outlined/ChevronRight';
import Expand from '@aivot/mui-material-symbols-400-n25-outlined/Expand';
import CollapseAll from '@aivot/mui-material-symbols-400-n25-outlined/CollapseAll';
import {isArraySegment, type ProcessDataStructureTreeNode} from '../../../utils/process-data-structure';
import {pluralize} from '../../../../../utils/humanization-utils';

interface StructureItem {
    id: string;
    label: string;
    children: StructureItem[];
}

function toItems(node: ProcessDataStructureTreeNode): StructureItem[] {
    return [...node.children.values()]
        .sort((a, b) => a.segment.localeCompare(b.segment, 'de', {numeric: true}))
        .map(child => ({
            id: child.path,
            label: isArraySegment(child.segment) ? `[${child.segment}]` : child.segment,
            children: toItems(child),
        }));
}

function branchIds(items: StructureItem[]): string[] {
    return items.flatMap(item => item.children.length > 0 ? [item.id, ...branchIds(item.children)] : []);
}

function countDefinedPaths(node: ProcessDataStructureTreeNode | null): number {
    if (node == null) return 0;
    return (node.entries.length > 0 ? 1 : 0) + [...node.children.values()]
        .reduce((count, child) => count + countDefinedPaths(child), 0);
}

export function ProcessDataStructureTree(props: {
    node: ProcessDataStructureTreeNode | null;
    search: string;
    selectedPath: string;
    pathCount: number;
    label: string;
    prefix: string;
    onSelect: (path: string) => void;
}) {
    const items = useMemo(() => props.node == null ? [] : toItems(props.node), [props.node]);
    const branches = useMemo(() => branchIds(items), [items]);
    const [expanded, setExpanded] = useState<string[]>([]);
    const [searchExpanded, setSearchExpanded] = useState<string[]>([]);
    const searching = props.search.trim() !== '';
    // Open matching ancestors on each new query; retain the user's normal expansion when clearing it.
    useEffect(() => {
        setSearchExpanded(branches);
    }, [branches, props.search]);
    const effectiveExpanded = searching ? searchExpanded : expanded;
    const setEffectiveExpanded = searching ? setSearchExpanded : setExpanded;
    const allExpanded = branches.length > 0 && branches.every(id => effectiveExpanded.includes(id));
    const expandLabel = allExpanded ? 'Alle zuklappen' : 'Alle aufklappen';
    const visiblePathCount = useMemo(() => countDefinedPaths(props.node), [props.node]);

    return <>
        <Box sx={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: 1,
            mt: 3,
            pb: 1.5,
            flexShrink: 0,
        }}>
            <Box sx={{
                flex: 1,
                minWidth: 0,
            }}>
                <Typography variant="h6"
                            component="h3">
                    {props.label} <Box component="span"
                                       sx={{
                                           fontFamily: 'monospace',
                                           fontSize: '0.875rem',
                                           fontWeight: 400,
                                           color: 'text.secondary',
                                           ml: 0.5,
                                       }}>{props.prefix}</Box>
                </Typography>
                <Typography component="p"
                            variant="caption"
                            color="textSecondary"
                            role="status"
                            sx={{mt: 0.5}}>
                    {searching ? `${visiblePathCount} von ${props.pathCount} ${pluralize(props.pathCount, 'Pfad', 'Pfaden')} angezeigt`
                        : `${props.pathCount} ${pluralize(props.pathCount, 'definierter Pfad', 'definierte Pfade')}`}
                </Typography>
            </Box>
            <Tooltip title={expandLabel}
                     arrow>
                <span>
                    <IconButton size="small"
                                color="primary"
                                aria-label={expandLabel}
                                disabled={branches.length === 0}
                                onClick={() => setEffectiveExpanded(allExpanded ? [] : branches)}>
                        {allExpanded ? <CollapseAll/> : <Expand/>}
                    </IconButton>
                </span>
            </Tooltip>
        </Box>
        <Box sx={{
            flex: 1,
            minHeight: 0,
            overflow: 'auto',
            pb: 2,
        }}>
            {items.length === 0 ? <Typography variant="body2"
                                              color="textSecondary"
                                              sx={{p: 1}}>
                Keine passenden Pfade gefunden.
            </Typography> : <RichTreeView
                aria-label={`Struktur: ${props.label}`}
                items={items}
                itemChildrenIndentation={20}
                expansionTrigger="iconContainer"
                expandedItems={effectiveExpanded}
                onExpandedItemsChange={(_, ids) => setEffectiveExpanded(ids)}
                selectedItems={props.selectedPath || null}
                onSelectedItemsChange={(_, id) => {
                    if (id != null) props.onSelect(id);
                }}
                slots={{
                    expandIcon: ChevronRight,
                    collapseIcon: CollapseIcon,
                }}
                slotProps={{
                    item: ({itemId}) => ({
                        title: `${props.prefix}.${itemId}`,
                        'aria-label': `${props.prefix}.${itemId}`,
                    }),
                }}
                sx={{
                    [`& .${treeItemClasses.content}`]: {
                        minHeight: 38,
                        borderRadius: 1,
                        my: 0.25,
                        pr: 1.5,
                        '&[data-selected]': {color: 'primary.main'},
                        '&[data-focused]': {
                            outline: '2px solid',
                            outlineColor: 'primary.main',
                            outlineOffset: -2,
                        },
                    },
                    [`& .${treeItemClasses.label}`]: {
                        fontFamily: 'monospace',
                        fontSize: '0.875rem',
                        overflowWrap: 'anywhere',
                        minWidth: 0,
                    },
                    [`& .${treeItemClasses.iconContainer}`]: {color: 'text.secondary'},
                }}
            />}
        </Box>
    </>;
}

function CollapseIcon() {
    return <ChevronRight sx={{transform: 'rotate(90deg)'}}/>;
}
