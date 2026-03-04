import {BaseEditorProps} from './base-editor';
import {RichTextInputElement} from '../models/elements/form/input/rich-text-input-element';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {Grid, Typography} from '@mui/material';

export function RichTextInputFieldEditor(props: BaseEditorProps<RichTextInputElement, ElementTreeEntity>) {
    const {
        editable,
    } = props;

    return (
        <Grid
            container
            columnSpacing={4}
            rowSpacing={2}
        >
            <Grid size={{xs: 12}}>
                <Typography variant="body2" color="text.secondary">
                    Für dieses Element sind derzeit keine elementspezifischen Einstellungen verfügbar.
                </Typography>
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
