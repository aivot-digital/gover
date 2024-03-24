import { type AnyElement } from '../../models/elements/any-element';
import {type EditorTab} from '../../editors';
import {type ElementTreeScope} from '../element-tree/element-tree-scope';


export interface ElementEditorTabsProps<T extends AnyElement> {
    component: T;
    currentTab: string;
    onTabChange: (tab: string) => void;
    additionalTabs: EditorTab[];
    scope: ElementTreeScope;
    rootEditor: boolean;
}
