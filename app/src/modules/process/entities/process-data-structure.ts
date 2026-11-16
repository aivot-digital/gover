export enum ProcessDataStructureCategory {
    ProcessData = 'ProcessData',
    ElementData = 'ElementData',
    ProtectedProcessData = 'ProtectedProcessData',
}

export interface ProcessDataStructureOrigin {
    nodeId: number;
    dataKey: string;
    name: string | null;
}

export interface ProcessDataStructureEntry {
    category: ProcessDataStructureCategory;
    unexpandedStructure: boolean;
    path: string;
    label: string;
    description: string | null;
    origin: ProcessDataStructureOrigin | null;
}

export enum ProcessDataStructureIssueKind {
    MetadataUnavailable = 'MetadataUnavailable',
    InvalidPath = 'InvalidPath',
}

export interface ProcessDataStructure {
    entries: ProcessDataStructureEntry[];
    issues: {
        kind: ProcessDataStructureIssueKind;
        category: ProcessDataStructureCategory;
        origin: ProcessDataStructureOrigin;
        path: string | null;
    }[];
}
