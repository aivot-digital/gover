import {type GridColDef, type GridValidRowModel} from '@mui/x-data-grid';
import {type ListHeaderProps} from '../list-header/list-header-props';
import {GridInitialStateCommunity} from "@mui/x-data-grid/models/gridStateCommunity";

export interface TableWrapperProps<T extends GridValidRowModel> extends ListHeaderProps {
    columns: Array<GridColDef<T, any, any>>;
    rows: T[];
    onRowClick: (item: T) => void;
    getRowId?: (item: T) => string;
    isLoading?: boolean;

    noDataMessage?: string;
    noDataFoundMessage?: string;

    initialState?: GridInitialStateCommunity;
}
