import {type PageWrapperProps} from '../page-wrapper/page-wrapper-props';
import {FormEvent} from 'react';

export interface FormPageWrapperProps extends PageWrapperProps {
    hasChanged?: boolean;
    onSave?: (event: FormEvent<HTMLFormElement>) => void;
    onReset?: () => void;
    onDelete?: () => void;

    tabs?: Array<{
        label: string;
        content: JSX.Element;
    }>;
}
