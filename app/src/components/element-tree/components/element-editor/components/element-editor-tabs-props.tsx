import {type AnyElement} from '../../../../../models/elements/any-element';
import {type EditorTab} from '../../../../../editors';

export interface ElementEditorTabsProps<T extends AnyElement> {
    component: T;
    currentTab: string;
    onTabChange: (tab: string) => void;
    additionalTabs: EditorTab[];
}
