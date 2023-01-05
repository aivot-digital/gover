import {AnyElement} from '../../../../../../models/elements/any-element';
import {EditorType} from '../../../../../editor.map';

export interface ElementEditorContentProps<T extends AnyElement> {
    element: T;
    currentTab: string;
    additionalTabs: {
        label: string;
        editor: EditorType;
    }[];
    onChange: (update: T) => void;
}
