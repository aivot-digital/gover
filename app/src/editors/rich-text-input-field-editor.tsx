import {BaseEditorProps} from './base-editor';
import {RichTextInputElement} from '../models/elements/form/input/rich-text-input-element';
import {Grid, Typography} from '@mui/material';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';

export function RichTextInputFieldEditor(props: BaseEditorProps<RichTextInputElement>) {
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
                    label="Reduzierte Toolbar erzwingen"
                    value={element.reducedMode ?? false}
                    onChange={(checked) => {
                        onPatch({
                            reducedMode: checked,
                        });
                    }}
                    variant="switch"
                    disabled={!editable}
                    hint="Erzwingt eine reduzierte Toolbar. Wenn deaktiviert, schaltet der Editor bei Breiten unter 630px automatisch um."
                />
                {
                    !editable &&
                    <Typography variant="caption"
                                sx={{
                                    color: "text.secondary"
                                }}>
                        Das Element befindet sich im Nur-Lesen-Modus.
                    </Typography>
                }
            </Grid>
        </Grid>
    );
}
