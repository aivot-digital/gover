import {type AnyElement} from '../models/elements/any-element';
import {type Application} from '../models/entities/application';
import {type Preset} from '../models/entities/preset';

export interface EditorDispatcherProps<T extends AnyElement, E extends Application | Preset> {
    props: T;
    onPatch: (path: Partial<T>) => void;

    entity: E;
    onPatchEntity: (path: Partial<E>) => void;

    additionalTabIndex?: number;

    editable: boolean;
}
