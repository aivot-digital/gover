import React from 'react';
import {RichtextElement} from '../../models/elements/form/content/richtext-element';
import {
    RichTextEditorComponentView
} from '../richt-text-editor/rich-text-editor.component.view';
import {BaseEditorProps} from "../../editors/base-editor";

export function RichtextComponentEditor(props: BaseEditorProps<RichtextElement>) {
    return (
        <RichTextEditorComponentView
            value={props.element.content ?? ''}
            onChange={(value) => {
                props.onPatch({content: value});
            }}
        />
    );
}
