import {type HeadlineElement} from '../../models/elements/form/content/headline-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';

export function HeadlineComponentEditor(props: BaseEditorProps<HeadlineElement, ElementTreeEntity>): JSX.Element {
    return (
        <>
            <TextFieldComponent
                value={props.element.content ?? ''}
                label="Überschrift"
                onChange={(val) => {
                    props.onPatch({
                        content: val,
                    });
                }}
                disabled={!props.editable}
            />

            <CheckboxFieldComponent
                label="Kompakte Überschrift"
                value={props.element.small}
                onChange={(val) => {
                    props.onPatch({
                        small: val,
                    });
                }}
                disabled={!props.editable}
            />
        </>
    );
}
