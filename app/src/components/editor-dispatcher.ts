import React from 'react';
import {type EditorDispatcherProps} from './editor-dispatcher-props';
import {type AnyElement} from '../models/elements/any-element';
import Editors from '../editors';
import {type BaseEditorProps} from '../editors/base-editor';
import {ElementTreeEntity} from './element-tree/element-tree-entity';

export function EditorDispatcher<T extends AnyElement, E extends ElementTreeEntity>(props: EditorDispatcherProps<T, E>): null | JSX.Element {
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
