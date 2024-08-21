import {type GridColDef, type GridValidRowModel} from '@mui/x-data-grid';
import {type ListHeaderProps} from '../list-header/list-header-props';

export interface TableWrapperProps<T extends GridValidRowModel> extends ListHeaderProps {
    columns: Array<GridColDef<T, any, any>>;
    rows: T[];
    onRowClick: (item: T) => void;
    getRowId?: (item: T) => string;
    isLoading?: boolean;

    noDataMessage?: string;
    noDataFoundMessage?: string;
}
