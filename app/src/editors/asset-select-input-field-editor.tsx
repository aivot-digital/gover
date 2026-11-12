import {Grid} from '@mui/material';
import {type BaseEditorProps} from './base-editor';
import {type AssetSelectInputElement} from '../models/elements/form/input/asset-select-input-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {ChipInputFieldComponent} from '../components/chip-input-field/chip-input-field-component';
import {AssetVisibility} from '../modules/assets/models/asset-visibility';
import {useEffect, useState} from 'react';
import {AssetsApiService} from '../modules/assets/assets-api-service';
import {type AssetStorageProvider} from '../modules/assets/models/asset-storage-provider';

const visibilityOptions = [
    {value: AssetVisibility.All, label: 'Alle Dateien'},
    {value: AssetVisibility.Public, label: 'Nur öffentliche Dateien'},
    {value: AssetVisibility.Private, label: 'Nur private Dateien'},
];

export function AssetSelectInputFieldEditor(props: BaseEditorProps<AssetSelectInputElement>) {
    const {element, editable, onPatch, hasSummaryLayoutParent} = props;
    const [providers, setProviders] = useState<AssetStorageProvider[]>([]);
    const [loadingProviders, setLoadingProviders] = useState(true);
    const [providerError, setProviderError] = useState<string>();

    useEffect(() => {
        if (hasSummaryLayoutParent) return;
        let active = true;
        setLoadingProviders(true);
        setProviderError(undefined);
        new AssetsApiService().listStorageProviders()
            .then(result => {
                if (active) setProviders([...result].sort((a, b) => a.name.localeCompare(b.name, 'de')));
            })
            .catch(() => {
                if (active) setProviderError('Die Speicheranbieter konnten nicht geladen werden.');
            })
            .finally(() => {
                if (active) setLoadingProviders(false);
            });
        return () => { active = false; };
    }, [hasSummaryLayoutParent]);

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
            <Grid size={{xs: 12, lg: 6}}>
                <SelectFieldComponent<number>
                    label="Speicheranbieter"
                    value={element.storageProviderId}
                    onChange={storageProviderId => onPatch({storageProviderId: storageProviderId ?? null})}
                    options={providers.map(provider => ({value: provider.id, label: provider.name}))}
                    emptyOptionLabel="Alle verfügbaren Speicheranbieter"
                    hint="Beschränkt die Dateiauswahl auf diesen Speicheranbieter einschließlich aller Ordner."
                    error={providerError}
                    disabled={!editable}
                    busy={loadingProviders}
                />
            </Grid>
        </Grid>
    );
}
