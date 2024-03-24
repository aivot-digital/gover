import React from 'react';
import {type ImageElement} from '../../models/elements/form/content/image-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';

export function ImageEditor(props: BaseEditorProps<ImageElement, ElementTreeEntity>): JSX.Element {
    return (
        <>
            <TextFieldComponent
                value={props.element.src ?? ''}
                label="Url"
                onChange={(val) => {
                    props.onPatch({
                        src: val,
                    });
                }}
                disabled={!props.editable}
            />


            <TextFieldComponent
                value={props.element.alt ?? ''}
                label="Alt-Text"
                onChange={(val) => {
                    props.onPatch({
                        alt: val,
                    });
                }}
                disabled={!props.editable}
            />
        </>
    );
}
