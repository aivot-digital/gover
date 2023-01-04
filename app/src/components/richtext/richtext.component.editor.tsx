import React from 'react';
import {RichtextElement} from '../../models/elements/form-elements/content-elements/richtext-element';
import {BaseEditorProps} from '../_lib/base-editor-props';
import {
    RichTextEditorComponentView
} from '../richt-text-editor/rich-text-editor.component.view';

export function RichtextComponentEditor(props: BaseEditorProps<RichtextElement>) {
    return (
        <RichTextEditorComponentView
            value={props.component.content ?? ''}
            onChange={(value) => {
                props.onPatch({content: value});
            }}
        />
    );
}
