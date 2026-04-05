import {BaseEditorProps} from './base-editor';
import {RichTextInputElement} from '../models/elements/form/input/rich-text-input-element';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {Grid, Typography} from '@mui/material';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';

export function RichTextInputFieldEditor(props: BaseEditorProps<RichTextInputElement, ElementTreeEntity>) {
    const {
        element,
        onPatch,
        editable,
    } = props;

    return (
        <Grid
            container
            columnSpacing={4}
            rowSpacing={2}
        >
            <Grid size={{xs: 12}}>
                <CheckboxFieldComponent
                    label="Reduzierter Modus"
                    value={element.reducedMode ?? false}
                    onChange={(checked) => {
                        onPatch({
                            reducedMode: checked,
                        });
                    }}
                    variant="switch"
                    disabled={!editable}
                    hint="Zeigt eine reduzierte Toolbar ohne Separatoren, Durchstreichen, Hervorheben und Inline-Code."
                />
                {
                    !editable &&
                    <Typography variant="caption" color="text.secondary">
                        Das Element befindet sich im Nur-Lesen-Modus.
                    </Typography>
                }
            </Grid>
        </Grid>
    );
}
