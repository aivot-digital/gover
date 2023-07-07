import { type PageWrapperProps } from '../page-wrapper/page-wrapper-props';

export interface FormPageWrapperProps extends PageWrapperProps {
    hasChanged: boolean;
    onSave: () => void;
    onReset?: () => void;
    onDelete?: () => void;
}
