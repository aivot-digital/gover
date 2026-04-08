import React from 'react';
import {type RichtextElement} from '../../models/elements/form/content/richtext-element';
import {RichTextInputComponent} from '../rich-text-input-component/rich-text-input-component';
import {type BaseEditorProps} from '../../editors/base-editor';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {Grid} from '@mui/material';

export function RichtextComponentEditor(props: BaseEditorProps<RichtextElement, ElementTreeEntity>) {
    return (
        <Grid
            container
            columnSpacing={4}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 6
                }}>
                <RichTextInputComponent
                    label={'Anzuzeigender Text'}
                    value={props.element.content ?? ''}
                    onChange={(value) => {
                        props.onPatch({content: value ?? undefined});
                    }}
                    disabled={!props.editable}
                />
            </Grid>
        </Grid>
    );
}
