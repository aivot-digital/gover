import {AnyElement} from '../models/elements/any-element';

export interface EditorDispatcherProps<T extends AnyElement> {
    props: T;
    onPatch: (path: any) => void;
    additionalTabIndex?: number;
}
