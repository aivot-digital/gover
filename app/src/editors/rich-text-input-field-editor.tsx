import {BaseEditorProps} from './base-editor';
import {RichTextInputElement} from '../models/elements/form/input/rich-text-input-element';
import {Grid, Typography} from '@mui/material';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {useElementTreeContext} from '../components/element-tree-2/element-tree-context';
import {ElementDisplayContext} from '../data/element-type/element-child-options';

export function RichTextInputFieldEditor(props: BaseEditorProps<RichTextInputElement>) {
    const {
        element,
        onPatch,
        editable,
    } = props;
    const {displayContext} = useElementTreeContext();
    // Applicants always get their own short toolbar, so the setting only affects UIs that staff fill out.
    const showReducedModeSwitch = displayContext !== ElementDisplayContext.CustomerFacing;

    return (
        <Grid
            container
            columnSpacing={4}
            rowSpacing={2}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 4,
                }}
            >
                <NumberFieldComponent
                    label="Maximalanzahl an Zeichen"
                    value={element.maxCharacters ?? undefined}
                    onChange={(val) => {
                        onPatch({
                            maxCharacters: val,
                        });
                    }}
                    hint="Formatierungszeichen und Link-Adressen zählen mit. Geben Sie 0 oder nichts ein, um keine Maximalanzahl zu fordern."
                    disabled={!editable}
                />
            </Grid>
            <Grid size={{xs: 12}}>
                {
                    showReducedModeSwitch &&
                    <CheckboxFieldComponent
                        label="Reduzierte Werkzeugleiste erzwingen"
                        value={element.reducedMode ?? false}
                        onChange={(checked) => {
                            onPatch({
                                reducedMode: checked,
                            });
                        }}
                        variant="switch"
                        disabled={!editable}
                        hint="Erzwingt eine reduzierte Werkzeugleiste. Wenn deaktiviert, schaltet der Editor bei Breiten unter 630px automatisch um."
                    />
                }
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
