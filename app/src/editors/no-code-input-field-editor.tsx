import {BaseEditorProps} from './base-editor';
import {
    NoCodeInputFieldElement,
    NoCodeInputFieldReturnType
} from '../models/elements/form/input/no-code-input-field-element';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {Grid} from '@mui/material';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {SelectFieldComponentOption} from '../components/select-field/select-field-component-option';

const returnTypeOptions: SelectFieldComponentOption[] = [
    {
        value: NoCodeInputFieldReturnType.BOOLEAN,
        label: 'Wahrheitswert',
    },
    {
        value: NoCodeInputFieldReturnType.STRING,
        label: 'Text',
    },
    {
        value: NoCodeInputFieldReturnType.NUMBER,
        label: 'Zahl',
    },
    {
        value: NoCodeInputFieldReturnType.DATE,
        label: 'Datum',
    },
    {
        value: NoCodeInputFieldReturnType.DATETIME,
        label: 'Datum und Uhrzeit',
    },
];

export function NoCodeInputFieldEditor(props: BaseEditorProps<NoCodeInputFieldElement, ElementTreeEntity>) {
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
                }}
            >
                <SelectFieldComponent
                    label="Rückgabetyp"
                    value={element.returnType ?? NoCodeInputFieldReturnType.BOOLEAN}
                    onChange={(value) => {
                        onPatch({
                            returnType: value as NoCodeInputFieldReturnType,
                        });
                    }}
                    options={returnTypeOptions}
                    required
                    disabled={!editable}
                    hint="Definiert den erwarteten Ergebnistyp des modellierten No-Code-Ausdrucks."
                />
            </Grid>
        </Grid>
    );
}
