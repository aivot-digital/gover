import { type AnyElement } from '../models/elements/any-element';
import { type Application } from '../models/entities/application';

export interface EditorDispatcherProps<T extends AnyElement> {
    props: T;
    onPatch: (path: Partial<T>) => void;

    application: Application;
    onPatchApplication: (path: Partial<Application>) => void;

    additionalTabIndex?: number;

    editable: boolean;
}
