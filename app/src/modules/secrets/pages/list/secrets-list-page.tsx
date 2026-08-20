import {
    GenericListPage,
    type GenericListPagePermissionConfig,
    type GenericListPagePermissionState,
} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import KeyOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Key';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Typography} from '@mui/material';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import {SecretsApiService} from '../../secrets-api-service';
import {SecretEntityResponseDTO} from '../../dtos/secret-entity-response-dto';
import React, {useCallback} from 'react';
import ContentPasteOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ContentPaste';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {copyToClipboardText} from '../../../../utils/copy-to-clipboard';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {Permission} from '../../../../data/permissions/permission';

const secretsListPermissionCheck: GenericListPagePermissionConfig<SecretEntityResponseDTO> = {
    scope: {
        type: 'system',
    },
    read: Permission.SECRET_READ,
    create: Permission.SECRET_CREATE,
    update: Permission.SECRET_UPDATE,
};

export function SecretsListPage() {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();

    const header = useCallback((permissions: GenericListPagePermissionState<SecretEntityResponseDTO>) => ({
        icon: <KeyOutlinedIcon />,
        title: 'Geheimnisse',
        actions: [
            {
                label: 'Neues Geheimnis',
                icon: <AddOutlinedIcon />,
                to: '/secrets/new',
                variant: 'contained' as const,
                disabled: !permissions.canCreate,
                disabledTooltip: permissions.createDisabledTooltip,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Geheimnissen',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography
                        variant="body1"
                        sx={{
                            marginBottom: "16px"
                        }}
                    >
                        Verwalten Sie hier sicher die Geheimnisse Ihrer Webanwendung, wie API-Schlüssel, Passwörter oder andere vertrauliche Daten.
                        Diese werden getrennt vom Code gespeichert, um Sicherheitsrisiken zu minimieren und eine einfache Aktualisierung ohne Anpassung der Anwendung zu ermöglichen.
                    </Typography>
                    <Typography
                        variant="body1"
                        sx={{
                            marginBottom: "16px"
                        }}
                    >
                        Alle Geheimnisse sind verschlüsselt und nur für autorisierte Nutzer:innen oder Dienste mit entsprechender Berechtigung zugänglich.
                    </Typography>
                </>
            ),
        },
    }), []);

    const fetchSecrets = useCallback((options: GenericListPropsFetchOptions<SecretEntityResponseDTO>) => {
        return new SecretsApiService(options.api)
            .list(
                options.page,
                options.size,
                options.sort,
                options.order,
                {name: options.search},
            );
    }, []);

    const columnDefinitions = useCallback((permissions: GenericListPagePermissionState<SecretEntityResponseDTO>) => [
        {
            field: 'icon',
            headerName: '',
            renderCell: () => <CellContentWrapper><KeyOutlinedIcon /></CellContentWrapper>,
            disableColumnMenu: true,
            width: 24,
            sortable: false,
        },
        {
            field: 'name',
            headerName: 'Name',
            flex: 1,
            renderCell: (params: any) => (
                <CellLink
                    to={`/secrets/${params.id}`}
                    title={permissions.canUpdate(params.row) ? 'Geheimnis bearbeiten' : 'Geheimnis anzeigen'}
                >
                    {String(params.value)}
                </CellLink>
            ),
        },
        {
            field: 'description',
            headerName: 'Beschreibung',
            flex: 2,
        },
    ], []);

    const getRowIdentifier = useCallback((row: SecretEntityResponseDTO) => row.key, []);

    const rowActions = useCallback((item: SecretEntityResponseDTO, permissions: GenericListPagePermissionState<SecretEntityResponseDTO>) => {
        const canUpdateSecret = permissions.canUpdate(item);

        return [
            {
                icon: canUpdateSecret ? <EditOutlined /> : <Visibility/>,
                to: `/secrets/${item.key}`,
                tooltip: canUpdateSecret ? 'Geheimnis bearbeiten' : 'Geheimnis anzeigen',
            },
            {
                icon: <ContentPasteOutlinedIcon />,
                onClick: async () => {
                    const success = await copyToClipboardText(item.key);
                    if (success) {
                        dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                    } else {
                        dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
                    }
                },
                tooltip: `Schlüssel (ID) in Zwischenablage kopieren (${item.key})`,
            },
        ];
    }, [dispatch]);

    const noDataPlaceholder = useCallback((permissions: GenericListPagePermissionState<SecretEntityResponseDTO>) => (
        <EmptyDataListPlaceholder
            title="Keine Geheimnisse vorhanden"
            description="Geheimnisse speichern vertrauliche Konfigurationswerte wie API-Schlüssel, Passwörter oder Tokens verschlüsselt."
            addText="Neues Geheimnis anlegen"
            onAdd={() => navigate('/secrets/new')}
            addDisabled={!permissions.canCreate}
            addDisabledTooltip={permissions.createDisabledTooltip}
        />
    ), [navigate]);

    return (
        <PageWrapper
            title="Geheimnisse"
            fullWidth
            background
        >
            <GenericListPage<SecretEntityResponseDTO>
                header={header}
                permissionCheck={secretsListPermissionCheck}
                searchLabel="Geheimnis suchen"
                searchPlaceholder="Name des Geheimnisses eingeben…"
                fetch={fetchSecrets}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={noDataPlaceholder}
                noSearchResultsPlaceholder="Keine Geheimnisse gefunden"
                rowActionsCount={2}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
