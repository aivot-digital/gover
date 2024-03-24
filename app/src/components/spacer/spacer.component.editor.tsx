import {type SpacerElement} from '../../models/elements/form/content/spacer-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {NumberFieldComponent} from '../number-field/number-field-component';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';

export function SpacerComponentEditor(props: BaseEditorProps<SpacerElement, ElementTreeEntity>): JSX.Element {
    return (
        <>
            <NumberFieldComponent
                value={props.element.height != null ? parseInt(props.element.height) : undefined}
                label="Abstand"
                hint="Die Angabe erfolgt in Pixeln (px)."
                suffix="px"
                onChange={(val) => {
                    props.onPatch({
                        height: val != null ? val.toString() : undefined,
                    });
                }}
                disabled={!props.editable}
            />
        </>
    );
}
