import {AnyElement} from '../../../../../models/elements/any-element';
import {EditorType} from '../../../../editor.map';

export interface ElementEditorTabsProps<T extends AnyElement> {
    component: T;
    currentTab: string;
    onTabChange: (tab: string) => void;
    additionalTabs: {
        label: string;
        editor: EditorType;
    }[];
}
