import { type FunctionComponent } from 'react';
import { type AnyElement } from '../models/elements/any-element';
import { type Application } from '../models/entities/application';

export interface BaseEditorProps<M> {
    element: M;
    onPatch: (patch: Partial<M>) => void;
    application: Application;
    onPatchApplication: (update: Partial<Application>) => void;
}

export type BaseEditor<M extends AnyElement> = FunctionComponent<BaseEditorProps<M>>;
