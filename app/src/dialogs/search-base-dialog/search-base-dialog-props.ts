import {SearchBaseDialogTabProps} from './search-base-dialog-tab-props';

export interface SearchBaseDialogProps<T> {
    id?: string;
    open: boolean;
    onClose: () => void;
    /** Called once the closing transition has finished, when focus can safely move elsewhere. */
    onExited?: () => void;
    title: string;
    tabs: SearchBaseDialogTabProps<T>[];
}
