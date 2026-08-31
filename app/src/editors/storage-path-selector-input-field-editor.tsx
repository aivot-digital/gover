import {Grid} from '@mui/material';
import {type BaseEditorProps} from './base-editor';
import {
    type StoragePathSelectorInputElement,
    StoragePathSelectorMode,
} from '../models/elements/form/input/storage-path-selector-input-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {MultiCheckboxComponent} from '../components/multi-checkbox-field/multi-checkbox-component';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {
    StorageProviderType,
    StorageProviderTypeLabels,
    StorageProviderTypes,
} from '../modules/storage/enums/storage-provider-type';

const modeOptions = [
    {
        value: StoragePathSelectorMode.Folder,
        label: 'Ordner',
    },
    {
        value: StoragePathSelectorMode.File,
        label: 'Datei',
    },
];

function getDefaultPlaceholder(mode: StoragePathSelectorMode): string {
    return mode === StoragePathSelectorMode.File ? 'Datei auswählen' : 'Ordner auswählen';
}

export function StoragePathSelectorInputFieldEditor(props: BaseEditorProps<StoragePathSelectorInputElement>) {
    const {
        element,
        editable,
        onPatch,
        hasSummaryLayoutParent,
    } = props;

    if (hasSummaryLayoutParent) {
        return null;
    }

    const allowedTypes = element.allowedStorageProviderTypes ?? StorageProviderTypes;
    const noTypeEnabled = allowedTypes.length === 0;

    return (
        <Grid
            container
            columnSpacing={4}
            rowSpacing={2}
        >
            <Grid size={{xs: 12, lg: 6}}>
                <SelectFieldComponent
                    label="Auswahlmodus"
                    value={element.mode ?? StoragePathSelectorMode.Folder}
                    onChange={(value) => {
                        const currentMode = element.mode ?? StoragePathSelectorMode.Folder;
                        const nextMode = value === StoragePathSelectorMode.File
                            ? StoragePathSelectorMode.File
                            : StoragePathSelectorMode.Folder;
                        const placeholder = element.placeholder === getDefaultPlaceholder(currentMode)
                            ? getDefaultPlaceholder(nextMode)
                            : element.placeholder;

                        onPatch({
                            mode: nextMode,
                            placeholder,
                        });
                    }}
                    options={modeOptions}
                    includeEmptyOption={false}
                    required={true}
                    hint="Legt fest, ob ein Ordner oder eine Datei ausgewählt werden kann."
                    disabled={!editable}
                />
            </Grid>

            <Grid size={{xs: 12, lg: 6}}>
                <TextFieldComponent
                    label="Platzhalter"
                    value={element.placeholder}
                    onChange={(value) => {
                        onPatch({
                            placeholder: value,
                        });
                    }}
                    hint="Der Platzhalter wird angezeigt, solange noch kein Pfad ausgewählt wurde."
                    disabled={!editable}
                />
            </Grid>

            <Grid size={{xs: 12}}>
                <TextFieldComponent
                    label="Hinweis Speicheranbieter-Auswahl"
                    value={element.storageProviderSelectHint}
                    onChange={(value) => {
                        onPatch({
                            storageProviderSelectHint: value,
                        });
                    }}
                    hint="Dieser Hinweis wird unter der Auswahl des Speicheranbieters angezeigt."
                    disabled={!editable}
                />
            </Grid>

            <Grid size={{xs: 12}}>
                <CheckboxFieldComponent
                    label="Read-only Speicheranbieter auswählbar"
                    value={element.allowReadOnlyStorageProviders === true}
                    onChange={(value) => {
                        onPatch({
                            allowReadOnlyStorageProviders: value,
                        });
                    }}
                    hint="Wenn aktiviert, können auch nur lesende Speicheranbieter ausgewählt werden."
                    disabled={!editable}
                />
            </Grid>

            <Grid size={{xs: 12}}>
                <MultiCheckboxComponent
                    label="Auswählbare Speicheranbieter-Typen"
                    value={allowedTypes}
                    onChange={(value) => {
                        onPatch({
                            allowedStorageProviderTypes: (value ?? []) as StorageProviderType[],
                        });
                    }}
                    options={StorageProviderTypes.map((type) => ({
                        value: type,
                        label: StorageProviderTypeLabels[type],
                    }))}
                    hint={noTypeEnabled ? 'Derzeit ist kein Speicheranbieter-Typ auswählbar.' : 'Wählen Sie, welche Speicheranbieter-Typen im Feld zur Auswahl stehen.'}
                    disabled={!editable}
                />
            </Grid>
        </Grid>
    );
}
