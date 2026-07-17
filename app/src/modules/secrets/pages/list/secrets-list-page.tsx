import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import KeyOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Key';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Typography} from '@mui/material';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import {SecretsApiService} from '../../secrets-api-service';
import {SecretEntityResponseDTO} from '../../dtos/secret-entity-response-dto';
import React, {useCallback, useMemo} from 'react';
import ContentPasteOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ContentPaste';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {copyToClipboardText} from '../../../../utils/copy-to-clipboard';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';

export function SecretsListPage() {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();

    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    const header = useMemo(() => ({
        icon: <KeyOutlinedIcon />,
        title: 'Geheimnisse',
        actions: [
            {
                label: 'Neues Geheimnis',
                icon: <AddOutlinedIcon />,
                to: '/secrets/new',
                variant: 'contained' as const,
                disabled: !hasAccess,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Geheimnissen',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography
                        variant="body1"
                        paragraph
                    >
                        Verwalten Sie hier sicher die Geheimnisse Ihrer Webanwendung, wie API-Schlüssel, Passwörter oder andere vertrauliche Daten.
                        Diese werden getrennt vom Code gespeichert, um Sicherheitsrisiken zu minimieren und eine einfache Aktualisierung ohne Anpassung der Anwendung zu ermöglichen.
                    </Typography>
                    <Typography
                        variant="body1"
                        paragraph
                    >
                        Alle Geheimnisse sind verschlüsselt und nur für autorisierte Nutzer:innen oder Dienste mit entsprechender Berechtigung zugänglich.
                    </Typography>
                </>
            ),
        },
    }), [hasAccess]);

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

    const columnDefinitions = useMemo(() => [
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
                    title={hasAccess ? 'Geheimnis bearbeiten' : 'Geheimnis anzeigen'}
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
    ], [hasAccess]);

    const getRowIdentifier = useCallback((row: SecretEntityResponseDTO) => row.key, []);

    const rowActions = useCallback((item: SecretEntityResponseDTO) => [
        {
            icon: hasAccess ? <EditOutlined /> : <Visibility/>,
            to: `/secrets/${item.key}`,
            tooltip: hasAccess ? 'Geheimnis bearbeiten' : 'Geheimnis anzeigen',
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
    ], [dispatch, hasAccess]);

    return (
        <PageWrapper
            title="Geheimnisse"
            fullWidth
            background
        >
            <GenericListPage<SecretEntityResponseDTO>
                header={header}
                searchLabel="Geheimnis suchen"
                searchPlaceholder="Name des Geheimnisses eingeben…"
                fetch={fetchSecrets}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Noch keine Geheimnisse angelegt"
                        description="Geheimnisse speichern vertrauliche Konfigurationswerte wie API-Schlüssel, Passwörter oder Tokens verschlüsselt."
                        addText={hasAccess ? "Neues Geheimnis anlegen" : undefined}
                        onAdd={hasAccess ? () => navigate('/secrets/new') : undefined}
                    />
                }
                noSearchResultsPlaceholder="Keine Geheimnisse gefunden"
                rowActionsCount={2}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
