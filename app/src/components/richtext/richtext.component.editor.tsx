import React, { type JSX } from 'react';
import {type RichtextElement} from '../../models/elements/form/content/richtext-element';
import {RichTextEditorComponentView} from '../richt-text-editor/rich-text-editor.component.view';
import {type BaseEditorProps} from '../../editors/base-editor';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {Grid} from '@mui/material';

export function RichtextComponentEditor(props: BaseEditorProps<RichtextElement, ElementTreeEntity>): JSX.Element {
    return (
        <Grid
            container
            columnSpacing={4}
        >
            <Grid
                item
                xs={12}
                lg={6}
            >
                <RichTextEditorComponentView
                    value={props.element.content ?? ''}
                    onChange={(value) => {
                        props.onPatch({content: value});
                    }}
                    disabled={!props.editable}
                />
            </Grid>
        </Grid>
    );
}
