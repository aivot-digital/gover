import React from 'react';
import {type RichtextElement} from '../../models/elements/form/content/richtext-element';
import {RichTextEditorComponentView} from '../richt-text-editor/rich-text-editor.component.view';
import {type BaseEditorProps} from '../../editors/base-editor';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';

export function RichtextComponentEditor(props: BaseEditorProps<RichtextElement, ElementTreeEntity>): JSX.Element {
    return (
        <RichTextEditorComponentView
            value={props.element.content ?? ''}
            onChange={(value) => {
                props.onPatch({content: value});
            }}
            disabled={!props.editable}
        />
    );
}
