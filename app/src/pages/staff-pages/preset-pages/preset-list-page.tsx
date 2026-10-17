import {
    GenericListPage,
    type GenericListPagePermissionConfig,
    type GenericListPagePermissionState,
} from '../../../components/generic-list-page/generic-list-page';
import {EmptyDataListPlaceholder} from '../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Typography} from '@mui/material';
import CopyAllOutlined from '@aivot/mui-material-symbols-400-n25-outlined/CopyAll';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import React, {useCallback, useState} from 'react';
import {CellLink} from '../../../components/cell-link/cell-link';
import {Preset} from '../../../models/entities/preset';
import {PresetsApiService} from '../../../modules/presets/presets-api-service';
import {AddPresetDialog} from '../../../dialogs/preset-dialogs/add-preset-dialog/add-preset-dialog';
import {useNavigate} from 'react-router-dom';
import {CellContentWrapper} from '../../../components/cell-content-wrapper/cell-content-wrapper';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {GenericListColDef, GenericListPropsFetchOptions} from '../../../components/generic-list/generic-list-props';
import {Permission} from '../../../data/permissions/permission';

const presetsListPermissionCheck: GenericListPagePermissionConfig<Preset> = {
    scope: {
        type: 'system',
    },
    read: Permission.PRESET_READ,
    create: Permission.PRESET_CREATE,
    update: Permission.PRESET_UPDATE,
};

export function PresetListPage() {
    const navigate = useNavigate();
    const [showAddPresetDialog, setShowAddPresetDialog] = useState(false);

    const navigateTo = useCallback((preset: Preset): void => {
        navigate(`/presets/edit/${preset.key}/${preset.draftedVersion}`);
    }, [navigate]);

    const columns = useCallback((permissions: GenericListPagePermissionState<Preset>): Array<GenericListColDef<Preset>> => [
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
                    to={`/presets/edit/${params.id}/${params.row.draftedVersion ?? params.row.publishedVersion}`}
                    title={permissions.canUpdate(params.row) && params.row.draftedVersion != null ? 'Vorlage bearbeiten' : 'Vorlage ansehen'}
                >
                    {String(params.value)}
                </CellLink>
            ),
            flex: 1,
        },
        {
            field: 'draftedVersion',
            headerName: 'Entwurf',
            renderCell: (params) => {
                return (
                    <CellContentWrapper>{params.row.draftedVersion ? `Version ${params.row.draftedVersion}` : (<Typography
                            sx={{
                                color: 'text.secondary',
                                fontStyle: 'italic'
                            }}>Kein Entwurf vorhanden</Typography>)}</CellContentWrapper>
                );
            },
            flex: 1,
        },
        {
            field: 'publishedVersion',
            headerName: 'Veröffentlichte Version',
            renderCell: (params) => {
                return (
                    <CellContentWrapper>{params.row.publishedVersion ? `Version ${params.row.publishedVersion}` : (<Typography
                            sx={{
                                color: 'text.secondary',
                                fontStyle: 'italic'
                            }}>Keine Version veröffentlicht</Typography>)}</CellContentWrapper>
                );
            },
            flex: 1,
        },
    ], []);

    const header = useCallback((permissions: GenericListPagePermissionState<Preset>) => ({
        icon: <CopyAllOutlined />,
        title: 'Vorlagen',
        actions: [
            {
                label: 'Neue Vorlage',
                icon: <AddOutlinedIcon />,
                onClick: () => {
                    setShowAddPresetDialog(true);
                },
                variant: 'contained' as const,
                disabled: !permissions.canCreate,
                disabledTooltip: permissions.createDisabledTooltip,
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
    }), []);

    const fetchPresets = useCallback((order: GenericListPropsFetchOptions<Preset>) => {
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
    }, []);

    const getRowIdentifier = useCallback((row: Preset) => row.key, []);

    const rowActions = useCallback((item: Preset, permissions: GenericListPagePermissionState<Preset>) => {
        const version = item.draftedVersion ?? item.publishedVersion;
        const canEditDraft = permissions.canUpdate(item) && item.draftedVersion != null;

        return [
            {
                icon: canEditDraft ? <EditOutlined /> : <Visibility />,
                to: `/presets/edit/${item.key}/${version}`,
                tooltip: canEditDraft ? 'Vorlage bearbeiten' : 'Vorlage ansehen',
                visible: version != null,
            },
        ];
    }, []);

    const noDataPlaceholder = useCallback((permissions: GenericListPagePermissionState<Preset>) => (
        <EmptyDataListPlaceholder
            title="Keine Vorlagen vorhanden"
            description="Vorlagen bündeln wiederverwendbare Bausteine, damit Prozesse und Formulare schneller und einheitlicher erstellt werden können."
            addText="Neue Vorlage anlegen"
            onAdd={() => setShowAddPresetDialog(true)}
            addDisabled={!permissions.canCreate}
            addDisabledTooltip={permissions.createDisabledTooltip}
        />
    ), []);

    return (
        <PageWrapper
            title="Vorlagen"
            fullWidth
            background
        >
            <GenericListPage<Preset>
                header={header}
                permissionCheck={presetsListPermissionCheck}
                searchLabel="Vorlage suchen"
                searchPlaceholder="Name der Vorlage eingeben…"
                fetch={fetchPresets}
                columnDefinitions={columns}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={noDataPlaceholder}
                noSearchResultsPlaceholder="Keine Vorlagen gefunden"
                rowActionsCount={1}
                rowActions={rowActions}
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
