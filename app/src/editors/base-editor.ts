import {FunctionComponent} from "react";
import {AnyElement} from "../models/elements/any-element";

// TODO: Make private
export interface BaseEditorProps<M> {
    element: M;
    onPatch: (patch: Partial<M>) => void;
}

export type BaseEditor<M extends AnyElement> = FunctionComponent<BaseEditorProps<M>>;
