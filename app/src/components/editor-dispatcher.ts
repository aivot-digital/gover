import React from 'react';
import { type EditorDispatcherProps } from './editor-dispatcher-props';
import { type AnyElement } from '../models/elements/any-element';
import Editors from '../editors';
import { type BaseEditorProps } from '../editors/base-editor';

export function EditorDispatcher<T extends AnyElement>(props: EditorDispatcherProps<T>): null | JSX.Element {
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

    const editorProps: BaseEditorProps<T> = {
        element: props.props,
        onPatch: props.onPatch,
        application: props.application,
        onPatchApplication: props.onPatchApplication,
        editable: props.editable,
    };

    return React.createElement(Component, editorProps);
}
