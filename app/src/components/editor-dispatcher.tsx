import React from 'react';
import {EditorDispatcherProps} from './editor-dispatcher-props';
import {AnyElement} from '../models/elements/any-element';
import Editors from "../editors";
import {BaseEditorProps} from "../editors/base-editor";

export function EditorDispatcher<T extends AnyElement>({onPatch, props, additionalTabIndex}: EditorDispatcherProps<T>) {
    let editorSet = Editors[props.type];
    if (editorSet == null) {
        return null;
    }

    let Component = null;
    if (additionalTabIndex != null && editorSet.additionalTabs != null && editorSet.additionalTabs.length > additionalTabIndex) {
        Component = editorSet.additionalTabs[additionalTabIndex].editor;
    } else {
        Component = editorSet.default;
    }

    const editorProps: BaseEditorProps<T> = {
        element: props,
        onPatch,
    }

    return React.createElement(Component, editorProps);
}
