import {BaseEditorProps} from './base-editor';
import {DataObjectSelectFieldElement} from '../models/elements/form/input/data-object-select-field-element';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {Grid} from '@mui/material';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {DataModelSelectFieldComponent} from '../components/data-model-select-field/data-model-select-field-component';

export function DataObjectSelectFieldEditor(props: BaseEditorProps<DataObjectSelectFieldElement, ElementTreeEntity>) {
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
            <Grid size={{xs: 12}}>
                <DataModelSelectFieldComponent
                    label="Datenmodell"
                    value={element.dataModelKey ?? undefined}
                    onChange={(value) => {
                        onPatch({
                            dataModelKey: value,
                        });
                    }}
                    hint="Definiert das Datenmodell, aus dem die auswählbaren Datenobjekte geladen werden."
                    disabled={!editable}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}
            >
                <TextFieldComponent
                    label="Label-Attribut (Key)"
                    value={element.dataLabelAttributeKey}
                    onChange={(value) => {
                        onPatch({
                            dataLabelAttributeKey: value,
                        });
                    }}
                    hint="Key des Datenobjekt-Attributes, dessen Wert als primäres Label angezeigt wird (z. B. name)."
                    disabled={!editable}
                />
            </Grid>

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
                    hint="Der Platzhalter wird angezeigt, solange noch kein Datenobjekt ausgewählt wurde."
                    disabled={!editable}
                />
            </Grid>
        </Grid>
    );
}
