import {formatProcessDataPath, parseProcessDataPath} from '../../../utils/process-data-path';
import {ProcessDataStructureCategory, type ProcessDataStructureEntry} from '../entities/process-data-structure';

// Element metadata have their own UI tab, while remaining part of the protected data namespace in the API.
export type ProcessDataStructureTab = ProcessDataStructureCategory | 'ElementMetadata';

export const processDataStructureTabs: Record<ProcessDataStructureTab, { label: string; prefix: string }> = {
    [ProcessDataStructureCategory.ProcessData]: {
        label: 'Vorgangsdaten',
        prefix: '$',
    },
    [ProcessDataStructureCategory.ElementData]: {
        label: 'Elementdaten',
        prefix: '_',
    },
    ElementMetadata: {
        label: 'Elementmetadaten',
        prefix: '$$.taskMetadata',
    },
    [ProcessDataStructureCategory.ProtectedProcessData]: {
        label: 'Geschützte Vorgangsdaten',
        prefix: '$$',
    },
};

export function matchesDataStructureTab(category: ProcessDataStructureCategory, path: string | null, tab: ProcessDataStructureTab): boolean {
    if (category !== ProcessDataStructureCategory.ProtectedProcessData) return category === tab;
    if (tab !== 'ElementMetadata' && tab !== ProcessDataStructureCategory.ProtectedProcessData) return false;
    if (path == null) return true;
    const isElementMetadata = path === 'taskMetadata' || path.startsWith('taskMetadata.');
    return tab === 'ElementMetadata' ? isElementMetadata : !isElementMetadata;
}

export function getDataStructureTabEntries(entries: ProcessDataStructureEntry[], tab: ProcessDataStructureTab): ProcessDataStructureEntry[] {
    return entries.filter(entry => matchesDataStructureTab(entry.category, entry.path, tab))
        .filter(entry => tab !== 'ElementMetadata' || entry.path !== 'taskMetadata')
        .map(entry => tab === 'ElementMetadata' ? {
            ...entry,
            path: entry.path.slice('taskMetadata.'.length),
        } : entry);
}

export interface ProcessDataStructureTreeNode {
    segment: string;
    path: string;
    entries: ProcessDataStructureEntry[];
    children: Map<string, ProcessDataStructureTreeNode>;
}

export function buildProcessDataStructureTree(entries: ProcessDataStructureEntry[]): ProcessDataStructureTreeNode {
    const root: ProcessDataStructureTreeNode = {
        segment: '$',
        path: '',
        entries: [],
        children: new Map(),
    };
    for (const entry of entries) {
        const segments = parseProcessDataPath(entry.path);
        if (segments == null || segments.length === 0) continue;
        let parent = root;
        segments.forEach((segment, index) => {
            let child = parent.children.get(segment);
            if (child == null) {
                child = {
                    segment,
                    path: formatProcessDataPath(segments.slice(0, index + 1)),
                    entries: [],
                    children: new Map(),
                };
                parent.children.set(segment, child);
            }
            parent = child;
        });
        if (!parent.entries.some(candidate => candidate.origin?.nodeId === entry.origin?.nodeId &&
            candidate.label === entry.label && candidate.description === entry.description)) {
            parent.entries.push(entry);
        }
    }
    return root;
}

export function hasMixedStructure(node: ProcessDataStructureTreeNode): boolean {
    const segments = [...node.children.keys()];
    return segments.some(isArraySegment) && segments.some(segment => !isArraySegment(segment));
}

export function isArraySegment(segment: string): boolean {
    return /^(?:[0-9]+|\*)$/.test(segment);
}

export function filterProcessDataStructureTree(node: ProcessDataStructureTreeNode, search: string, prefix = '$'): ProcessDataStructureTreeNode | null {
    const query = search.trim().toLocaleLowerCase('de');
    if (query === '' || [node.path, prefix + '.' + node.path, ...node.entries.flatMap(entry =>
        [entry.label, entry.description, entry.origin?.name, entry.origin?.dataKey])]
        .some(value => value?.toLocaleLowerCase('de').includes(query))) return node;
    const children = new Map<string, ProcessDataStructureTreeNode>();
    for (const [key, child] of node.children) {
        const filtered = filterProcessDataStructureTree(child, search, prefix);
        if (filtered != null) children.set(key, filtered);
    }
    return children.size > 0 ? {
        ...node,
        children,
    } : null;
}
