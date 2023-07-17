import {type HeadlineElement} from '../../models/elements/form/content/headline-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {Application} from '../../models/entities/application';
import {Preset} from '../../models/entities/preset';

export function HeadlineComponentEditor(props: BaseEditorProps<HeadlineElement, Application | Preset>): JSX.Element {
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
