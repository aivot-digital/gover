import {AnyElement} from '../../../../../../models/elements/any-element';
import {EditorTab} from "../../../../../../editors";

export interface ElementEditorContentProps<T extends AnyElement> {
    element: T;
    currentTab: string;
    additionalTabs: EditorTab[];
    onChange: (update: T) => void;
}
