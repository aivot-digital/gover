import {SearchBaseDialogTabProps} from './search-base-dialog-tab-props';

export interface SearchBaseDialogProps<T> {
    id?: string;
    open: boolean;
    onClose: () => void;
    title: string;
    tabs: SearchBaseDialogTabProps<T>[];
}
