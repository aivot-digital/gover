import {type NumberFieldElement} from '../models/elements/form/input/number-field-element';
import {type BaseEditor} from './base-editor';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {Grid} from '@mui/material';

export const NumberFieldEditor: BaseEditor<NumberFieldElement, ElementTreeEntity> = ({
                                                                                         element,
                                                                                         onPatch,
                                                                                         editable,
                                                                                     }) => {
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
                <TextFieldComponent
                    value={element.placeholder ?? ''}
                    label="Platzhalter"
                    onChange={(val) => {
                        onPatch({
                            placeholder: val,
                        });
                    }}
                    hint={"Ein Platzhalter zeigt ein Beispiel für die erwartete Eingabe an, z. B. „hallo@bad-musterstadt.de“ bei einer E-Mail-Adresse."}
                    disabled={!editable}
                />
            </Grid>
            <Grid
                item
                xs={12}
                lg={6}
            >
                <TextFieldComponent
                    value={element.suffix ?? ''}
                    label="Einheit"
                    onChange={(val) => {
                        onPatch({
                            suffix: val,
                        });
                    }}
                    disabled={!editable}
                    hint={"Geben Sie optional an, mit welcher Einheit die Zahl angezeigt wird, z. B. „€“, „kg“ oder „Stück“."}
                />
            </Grid>
            <Grid
                item
                xs={12}
                lg={6}
            >
                <NumberFieldComponent
                    value={element.decimalPlaces}
                    label="Anzahl der Dezimalstellen"
                    onChange={(val) => {
                        onPatch({
                            decimalPlaces: val,
                        });
                    }}
                    disabled={!editable}
                    hint={"Geben Sie an, wie viele Dezimalstellen angezeigt werden sollen. Dies ist nützlich für Währungen oder Maßeinheiten."}
                />
            </Grid>
        </Grid>
    );
};
