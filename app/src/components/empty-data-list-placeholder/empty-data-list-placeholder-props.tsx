import {ReactNode} from 'react';

export interface EmptyDataListPlaceholderProps {
    title?: ReactNode;
    description?: ReactNode;
    helperText?: ReactNode;
    addText?: ReactNode;
    onAdd?: () => void;
}
