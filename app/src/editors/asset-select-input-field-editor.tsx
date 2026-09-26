import {Grid} from '@mui/material';
import {type BaseEditorProps} from './base-editor';
import {type AssetSelectInputElement} from '../models/elements/form/input/asset-select-input-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {ChipInputFieldComponent} from '../components/chip-input-field/chip-input-field-component';
import {AssetVisibility} from '../modules/assets/models/asset-visibility';

const visibilityOptions = [
    {value: AssetVisibility.All, label: 'Alle Dateien'},
    {value: AssetVisibility.Public, label: 'Nur öffentliche Dateien'},
    {value: AssetVisibility.Private, label: 'Nur private Dateien'},
];

export function AssetSelectInputFieldEditor(props: BaseEditorProps<AssetSelectInputElement>) {
    const {element, editable, onPatch, hasSummaryLayoutParent} = props;

    if (hasSummaryLayoutParent) {
        return null;
    }

    return (
        <Grid container columnSpacing={4} rowSpacing={2}>
            <Grid size={{xs: 12, lg: 6}}>
                <TextFieldComponent
                    label="Platzhalter"
                    value={element.placeholder}
                    onChange={(placeholder) => onPatch({placeholder})}
                    hint="Wird angezeigt, solange keine Datei ausgewählt ist."
                    disabled={!editable}
                />
            </Grid>
            <Grid size={{xs: 12, lg: 6}}>
                <TextFieldComponent
                    label="Dialogtitel"
                    value={element.dialogTitle}
                    onChange={(dialogTitle) => onPatch({dialogTitle})}
                    hint="Überschrift des Dialogs zur Dateiauswahl."
                    disabled={!editable}
                />
            </Grid>
            <Grid size={{xs: 12, lg: 6}}>
                <SelectFieldComponent
                    label="Sichtbarkeit"
                    value={element.assetVisibility ?? AssetVisibility.All}
                    onChange={(assetVisibility) => onPatch({
                        assetVisibility: (assetVisibility as AssetVisibility | null) ?? AssetVisibility.All,
                    })}
                    options={visibilityOptions}
                    includeEmptyOption={false}
                    required
                    hint="Beschränkt die Auswahl auf öffentliche oder private Dateien."
                    disabled={!editable}
                />
            </Grid>
            <Grid size={{xs: 12, lg: 6}}>
                <ChipInputFieldComponent
                    label="Erlaubte MIME-Typen"
                    value={element.allowedMimeTypes}
                    onChange={(allowedMimeTypes) => onPatch({allowedMimeTypes})}
                    placeholder="z. B. application/pdf"
                    hint="Ohne Eintrag werden alle Dateitypen angezeigt."
                    allowDuplicates={false}
                    disabled={!editable}
                />
            </Grid>
        </Grid>
    );
}
