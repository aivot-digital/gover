import { type FunctionComponent } from 'react';
import { type AnyElement } from '../models/elements/any-element';
import { type Application } from '../models/entities/application';
import { type Preset } from '../models/entities/preset';

export interface BaseEditorProps<T extends AnyElement, E extends Application | Preset> {
    element: T;
    onPatch: (patch: Partial<T>) => void;
    entity: E;
    onPatchEntity: (update: Partial<E>) => void;
    editable: boolean;
}

export type BaseEditor<M extends AnyElement, E extends Application | Preset> = FunctionComponent<BaseEditorProps<M, E>>;
