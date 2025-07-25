import {type SpacerElement} from '../../models/elements/form/content/spacer-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {NumberFieldComponent} from '../number-field/number-field-component';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {Grid} from '@mui/material';

export function SpacerComponentEditor(props: BaseEditorProps<SpacerElement, ElementTreeEntity>) {
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
            </Grid>
        </Grid>
    );
}
