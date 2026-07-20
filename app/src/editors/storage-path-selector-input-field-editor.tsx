import {Grid} from '@mui/material';
import {type BaseEditorProps} from './base-editor';
import {type StoragePathSelectorInputElement} from '../models/elements/form/input/storage-path-selector-input-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {MultiCheckboxComponent} from '../components/multi-checkbox-field/multi-checkbox-component';
import {
    StorageProviderType,
    StorageProviderTypeLabels,
    StorageProviderTypes,
} from '../modules/storage/enums/storage-provider-type';

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
