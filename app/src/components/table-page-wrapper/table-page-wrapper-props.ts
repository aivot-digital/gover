import {PageWrapperProps} from "../page-wrapper/page-wrapper-props";
import {GridColDef, GridValidRowModel} from "@mui/x-data-grid";
import {ListHeaderProps} from "../list-header/list-header-props";

export interface TablePageWrapperProps<T extends GridValidRowModel> extends PageWrapperProps, ListHeaderProps {
    columns: GridColDef<T, any, any>[];
    rows: T[];
    onRowClick: (item: T) => void;
}