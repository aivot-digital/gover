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
import {GridColDef} from '@mui/x-data-grid';
import {AddPresetDialog} from '../../../dialogs/preset-dialogs/add-preset-dialog/add-preset-dialog';
import {useNavigate} from 'react-router-dom';
import {CellContentWrapper} from '../../../components/cell-content-wrapper/cell-content-wrapper';

const columns: Array<GridColDef<Preset>> = [
    {
        field: 'icon',
        headerName: '',
        renderCell: () => <CellContentWrapper><CopyAllOutlined /></CellContentWrapper>,
        disableColumnMenu: true,
        width: 24,
        sortable: false,
    },
    {
        field: 'title',
        headerName: 'Titel',
        renderCell: (params) => (
            <CellLink
                to={`/presets/edit/${params.id}/${params.row.draftedVersion}`}
                title={`Vorlage bearbeiten`}
            >
                {String(params.value)}
            </CellLink>
        ),
        flex: 1,
    },
    {
        field: 'draftedVersion',
        headerName: 'Arbeits-Version',
        flex: 1,
    },
    {
        field: 'publishedVersion',
        headerName: 'Veröffentlichte Version',
        flex: 1,
    },
];

export function PresetListPage() {
    const user = useSelector(selectUser);
    const navigate = useNavigate();

    const storeKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.gover.storeKey));
    const [showAddPresetDialog, setShowAddPresetDialog] = useState(false);

    const navigateTo = (preset: Preset): void => {
        navigate(`/presets/edit/${preset.key}/${preset.draftedVersion}`);
    };

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
                        to: `/presets/edit/${item.key}/${item.draftedVersion}`,
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