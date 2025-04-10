import {GenericListPage} from '../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {CopyAllOutlined, EditOutlined} from '@mui/icons-material';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../slices/user-slice';
import React, {useState} from 'react';
import {CellLink} from '../../../components/cell-link/cell-link';
import {Preset} from '../../../models/entities/preset';
import {PresetsApiService} from '../../../modules/presets/presets-api-service';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {GridColDef, GridRenderCellParams} from '@mui/x-data-grid';
import {isStringNotNullOrEmpty} from '../../../utils/string-utils';
import {AddPresetDialog} from '../../../dialogs/preset-dialogs/add-preset-dialog/add-preset-dialog';
import {useNavigate} from 'react-router-dom';

const _columns: Array<GridColDef<Preset>> = [
    {
        field: 'icon',
        headerName: '',
        renderCell: () => <CopyAllOutlined />,
        disableColumnMenu: true,
        width: 24,
        sortable: false,
    },
    {
        field: 'title',
        headerName: 'Titel',
        renderCell: (params) => (
            <CellLink
                to={`/presets/edit/${params.id}/${params.row.currentVersion}`}
                title={`Vorlage bearbeiten`}
            >
                {String(params.value)}
            </CellLink>
        ),
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
        renderCell: (params) => params.row.currentPublishedVersion ?? 'Unveröffentlicht',
        flex: 1,
    },
];

export function PresetListPage() {
    const user = useSelector(selectUser);
    const navigate = useNavigate();

    const storeKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.gover.storeKey));
    const [showAddPresetDialog, setShowAddPresetDialog] = useState(false);

    const navigateTo = (preset: Preset): void => {
        navigate(`/presets/edit/${preset.key}/${preset.currentVersion}`);
    };

    const columns = storeKey != null && isStringNotNullOrEmpty(storeKey) ?
        [
            ..._columns,
            {
                field: 'currentStoreVersion',
                headerName: 'Store-Version',
                renderCell: (params: GridRenderCellParams<any, Preset>) => params.row.currentStoreVersion ?? 'Nicht im Store verfügbar',
                flex: 1,
            },
        ] :
        _columns;

    return (
        <PageWrapper
            title="Vorlagen"
            fullWidth
            background
        >
            <GenericListPage<Preset>
                header={{
                    icon: <CopyAllOutlined />,
                    title: 'Vorlagen',
                    actions: [
                        {
                            label: 'Neue Vorlage',
                            icon: <AddOutlinedIcon />,
                            onClick: () => {
                                setShowAddPresetDialog(true);
                            },
                            variant: 'contained',
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Vorlagen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Vorlagen dienen als Bausteine, die in Formularen verbaut werden können.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Mit Vorlagen können Sie Formularelemente kombinieren und als wiederverwendbare Vorlage abspeichern.
                                    So können Sie z.B. Standardtexte oder Formularabschnitte zentral verwalten und in mehreren Formularen verwenden.
                                </Typography>
                            </>
                        ),
                    },
                }}
                searchLabel="Vorlage suchen"
                searchPlaceholder="Name der Vorlage eingeben…"
                fetch={(order) => {
                    return new PresetsApiService(order.api)
                        .list(
                            order.page,
                            order.size,
                            order.sort,
                            order.order,
                            {
                                title: order.search,
                            },
                        );
                }}
                columnDefinitions={columns}
                getRowIdentifier={row => row.key}
                noDataPlaceholder="Keine Vorlagen angelegt"
                noSearchResultsPlaceholder="Keine Vorlagen gefunden"
                rowActionsCount={3}
                rowActions={(item: Preset) => [
                    {
                        icon: <EditOutlined />,
                        to: `/presets/edit/${item.key}/${item.currentVersion}`,
                        tooltip: 'Vorlage bearbeiten',
                    },
                ]}
                defaultSortField="title"
                disableFullWidthToggle={true}
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
        </PageWrapper>
    );
}