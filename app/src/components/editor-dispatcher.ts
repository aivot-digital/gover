import React from 'react';
import { type EditorDispatcherProps } from './editor-dispatcher-props';
import { type AnyElement } from '../models/elements/any-element';
import Editors from '../editors';
import { type BaseEditorProps } from '../editors/base-editor';
import { type Application } from '../models/entities/application';
import { type Preset } from '../models/entities/preset';

export function EditorDispatcher<T extends AnyElement, E extends Application | Preset>(props: EditorDispatcherProps<T, E>): null | JSX.Element {
    const editorSet = Editors[props.props.type];
    if (editorSet == null) {
        return null;
    }

    let Component = null;
    if (props.additionalTabIndex != null && editorSet.additionalTabs != null && editorSet.additionalTabs.length > props.additionalTabIndex) {
        Component = editorSet.additionalTabs[props.additionalTabIndex].editor;
    } else {
        Component = editorSet.default;
    }

    const editorProps: BaseEditorProps<T, E> = {
        element: props.props,
        onPatch: props.onPatch,
        entity: props.entity,
        onPatchEntity: props.onPatchEntity,
        editable: props.editable,
    };

    return React.createElement(Component, editorProps);
}
