import React from 'react';
import {EditorMap, EditorType, EditorTypeSet, isEditorTypeSet} from './editor.map';
import {BaseEditorProps} from './_lib/base-editor-props';
import {EditorDispatcherProps} from './editor-dispatcher-props';
import {AnyElement} from '../models/elements/any-element';

export function EditorDispatcher<T extends AnyElement>({onPatch, props, additionalTabIndex}: EditorDispatcherProps<T>) {
    let Component: EditorType | EditorTypeSet = EditorMap[props.type];
    if (Component == null) {
        return null;
    }

    if (isEditorTypeSet(Component)) {
        if (additionalTabIndex != null && Component.additionalTabs.length > additionalTabIndex) {
            Component = Component.additionalTabs[additionalTabIndex].editor;
        } else {
            Component = Component.root;
        }
    }

    const editorProps: BaseEditorProps<T> = {
        component: props,
        onPatch,
    }

    return React.createElement(Component, editorProps);
}
