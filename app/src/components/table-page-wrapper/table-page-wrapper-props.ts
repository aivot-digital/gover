import {type PageWrapperProps} from '../page-wrapper/page-wrapper-props';
import {type GridValidRowModel} from '@mui/x-data-grid';
import {type ListHeaderProps} from '../list-header/list-header-props';
import {type TableWrapperProps} from '../table-wrapper/table-wrapper-props';

export interface TablePageWrapperProps<T extends GridValidRowModel> extends TableWrapperProps<T>, PageWrapperProps, ListHeaderProps {
}
