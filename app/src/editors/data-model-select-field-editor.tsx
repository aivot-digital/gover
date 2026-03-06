import {BaseEditorProps} from './base-editor';
import {DataModelSelectFieldElement} from '../models/elements/form/input/data-model-select-field-element';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {Grid} from '@mui/material';
import {TextFieldComponent} from '../components/text-field/text-field-component';

export function DataModelSelectFieldEditor(props: BaseEditorProps<DataModelSelectFieldElement, ElementTreeEntity>) {
    const {
        element,
        editable,
        onPatch,
    } = props;

    return (
        <Grid
            container
            columnSpacing={4}
            rowSpacing={2}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}
            >
                <TextFieldComponent
                    label="Platzhalter"
                    value={element.placeholder}
                    onChange={(value) => {
                        onPatch({
                            placeholder: value,
                        });
                    }}
                    hint="Der Platzhalter wird angezeigt, solange noch kein Datenmodell ausgewählt wurde."
                    disabled={!editable}
                />
            </Grid>
        </Grid>
    );
}
