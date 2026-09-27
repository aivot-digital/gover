import {GridRowId, GridRowSelectionModel} from '@mui/x-data-grid';

export function getSelectedRowIds(selectionModel: GridRowSelectionModel | undefined, allRowIds: GridRowId[]): GridRowId[] {
    if (selectionModel == null) {
        return [];
    }

    if (Array.isArray(selectionModel)) {
        return selectionModel;
    }

    if (selectionModel.type === 'include') {
        return Array.from(selectionModel.ids);
    }

    return allRowIds.filter((rowId) => !selectionModel.ids.has(rowId));
}

export function hasSelectedGridRows(selectionModel: GridRowSelectionModel | undefined, allRowIds: GridRowId[]): boolean {
    return getSelectedRowIds(selectionModel, allRowIds).length > 0;
}
