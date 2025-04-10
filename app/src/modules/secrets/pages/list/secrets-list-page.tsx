import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import KeyOutlinedIcon from '@mui/icons-material/KeyOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import {SecretsApiService} from '../../secrets-api-service';
import {SecretEntityResponseDTO} from '../../dtos/secret-entity-response-dto';
import React from 'react';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';

export function SecretsListPage() {
    useAdminGuard();
    const dispatch = useAppDispatch();

    return (
        <PageWrapper
            title="Geheimnisse"
            fullWidth
            background
        >
            <GenericListPage<SecretEntityResponseDTO>
                header={{
                    icon: <KeyOutlinedIcon />,
                    title: 'Geheimnisse',
                    actions: [
                        {
                            label: 'Neues Geheimnis',
                            icon: <AddOutlinedIcon />,
                            to: '/secrets/new',
                            variant: 'contained',
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
                }}
                searchLabel="Geheimnis suchen"
                searchPlaceholder="Name des Geheimnisses eingeben…"
                fetch={(options) => {
                    return new SecretsApiService(options.api)
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {name: options.search}
                        );
                }}
                columnDefinitions={[
                    {
                        field: 'icon',
                        headerName: '',
                        renderCell: () => <KeyOutlinedIcon />,
                        disableColumnMenu: true,
                        width: 24,
                        sortable: false,
                    },
                    {
                        field: 'name',
                        headerName: 'Name',
                        flex: 1,
                        renderCell: (params) => (
                            <CellLink
                                to={`/secrets/${params.id}`}
                                title={`Geheimnis bearbeiten`}
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
                ]}
                getRowIdentifier={row => row.key}
                noDataPlaceholder="Keine Geheimnisse angelegt"
                noSearchResultsPlaceholder="Keine Geheimnisse gefunden"
                rowActionsCount={2}
                rowActions={(item: SecretEntityResponseDTO) => [
                    {
                        icon: <EditOutlined />,
                        to: `/secrets/${item.key}`,
                        tooltip: 'Geheimnis bearbeiten',
                    },
                    {
                        icon: <ContentPasteOutlinedIcon />,
                        onClick: () => {
                            navigator
                                .clipboard
                                .writeText(item.key)
                                .then(() => {
                                    dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                                })
                                .catch((err) => {
                                    console.error(err);
                                    dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
                                });
                        },
                        tooltip: `Schlüssel (ID) in Zwischenablage kopieren (${item.key})`,
                    }
                ]}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}