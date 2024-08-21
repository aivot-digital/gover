import React, {useEffect, useState} from 'react';
import {type Preset} from '../../../models/entities/preset';
import {TablePageWrapper} from '../../../components/table-page-wrapper/table-page-wrapper';
import {type GridColDef, GridValueGetterParams} from '@mui/x-data-grid';
import {useNavigate} from 'react-router-dom';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {AddPresetDialog} from '../../../dialogs/preset-dialogs/add-preset-dialog/add-preset-dialog';
import {filterItems} from '../../../utils/filter-items';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {isStringNotNullOrEmpty} from '../../../utils/string-utils';
import {useApi} from "../../../hooks/use-api";
import {usePresetsApi} from "../../../hooks/use-presets-api";

const _columns: Array<GridColDef<Preset>> = [
    {
        field: 'title',
        headerName: 'Titel',
        valueGetter: (params) => params.row.title,
        flex: 1,
    },
    {
        field: 'currentVersion',
        headerName: 'Arbeits-Version',
        flex: 1,
    },
    {
        field: 'currentPublishedVersion',
        headerName: 'Veröffentlichte Version',
        valueGetter: (params) => params.row.currentPublishedVersion ?? 'Unveröffentlicht',
        flex: 1,
    },
];

export function PresetListPage(): JSX.Element {
    const api = useApi();
    const navigate = useNavigate();

    const storeKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.gover.storeKey));

    const [presets, setPresets] = useState<Preset[]>();
    const [search, setSearch] = useState('');

    const [isLoading, setIsLoading] = useState(false);
    const [loadingError, setLoadingError] = useState<string>();
    const [showAddPresetDialog, setShowAddPresetDialog] = useState(false);

    const columns = storeKey != null && isStringNotNullOrEmpty(storeKey) ?
        [
            ..._columns,
            {
                field: 'currentStoreVersion',
                headerName: 'Store-Version',
                valueGetter: (params: GridValueGetterParams<any, Preset>) => params.row.currentStoreVersion ?? 'Nicht im Store',
                flex: 1,
            },
        ] :
        _columns;

    useEffect(() => {
        setIsLoading(true);
        setLoadingError(undefined);

        usePresetsApi(api)
            .list()
            .then(setPresets)
            .catch((err) => {
                console.error(err);
                setLoadingError('Die Liste der Vorlagen konnte nicht geladen werden.');
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, []);

    const navigateTo = (preset: Preset): void => {
        navigate(`/presets/edit/${preset.key}/${preset.currentVersion}`);
    };

    const filtered = filterItems(presets, 'title', search);

    return (
        <>
            <TablePageWrapper
                title="Vorlagen"
                isLoading={isLoading}
                error={loadingError}

                hint={{
                    text: 'Hier können Sie Vorlagen anlegen, die Sie zum Bauen Ihrer Formulare wiederverwenden können.',
                    moreLink: 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/home' /* TODO: Link anpassen */,
                }}

                actions={[
                    {
                        label: 'Neue Vorlage',
                        onClick: () => {
                            setShowAddPresetDialog(true);
                        },
                        icon: <AddOutlinedIcon/>,
                        tooltip: 'Neue Vorlage hinzufügen',
                    },
                ]}

                rows={filtered}
                columns={columns}
                getRowId={(row) => row.key}

                search={search}
                onSearchChange={setSearch}
                searchPlaceholder="Vorlage suchen..."

                onRowClick={navigateTo}
            />

            <AddPresetDialog
                onClose={() => {
                    setShowAddPresetDialog(false);
                }}
                onAdded={(preset) => {
                    setShowAddPresetDialog(false);
                    navigateTo(preset);
                }}
                open={showAddPresetDialog}
            />
        </>
    );
}
