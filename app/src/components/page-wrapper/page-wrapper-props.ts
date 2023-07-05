import {AppToolbarPropsAction} from "../app-toolbar/app-toolbar-props";

export interface PageWrapperProps {
    isLoading?: boolean;
    title: string;

    toolbarActions?: AppToolbarPropsAction[];
}