import {type LoadingWrapperProps} from '../loading-wrapper/loading-wrapper-props';

export interface PageWrapperProps extends LoadingWrapperProps {
    is404?: boolean;
    error?: string;
    title: string;
    fullWidth?: boolean;
    fullHeight?: boolean;
    background?: boolean;
}
