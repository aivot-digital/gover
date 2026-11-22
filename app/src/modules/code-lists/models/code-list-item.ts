export interface CodeListItem {
    id: number;
    codeListId: number;
    // The cells in the order of the code list columns; a column mapping's index points into them.
    columns: string[];
    created: string;
    updated: string;
}
