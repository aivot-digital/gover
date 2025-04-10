import {SearchBaseDialogTabProps} from './search-base-dialog-tab-props';

export interface SearchBaseDialogProps<T> {
    open: boolean;
    onClose: () => void;
    title: string;
    tabs: SearchBaseDialogTabProps<T>[];
}