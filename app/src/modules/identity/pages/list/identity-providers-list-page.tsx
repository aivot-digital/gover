import {
    GenericListPage,
    type GenericListPagePermissionConfig,
    type GenericListPagePermissionState,
} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Typography} from '@mui/material';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import ScienceOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Science';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';
import BadgeOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Badge';
import {IdentityProviderListDTO} from '../../models/identity-provider-list-dto';
import Chip from '@mui/material/Chip';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import React, {useCallback} from 'react';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {Permission} from '../../../../data/permissions/permission';

const identityProvidersListPermissionCheck: GenericListPagePermissionConfig<IdentityProviderListDTO> = {
    scope: {
        type: 'system',
    },
    read: Permission.IDENTITY_PROVIDER_READ,
    create: Permission.IDENTITY_PROVIDER_CREATE,
    update: Permission.IDENTITY_PROVIDER_UPDATE,
};

export function IdentityProvidersListPage() {
    const navigate = useNavigate();

    const header = useCallback((permissions: GenericListPagePermissionState<IdentityProviderListDTO>) => ({
        icon: <BadgeOutlinedIcon />,
        title: 'Nutzerkontenanbieter',
        actions: [
            {
                label: 'Neuer Nutzerkontenanbieter',
                icon: <AddOutlinedIcon />,
                to: '/identity-providers/new',
                variant: 'contained' as const,
                disabled: !permissions.canCreate,
                disabledTooltip: permissions.createDisabledTooltip,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Nutzerkontenanbietern',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography
                        variant="body1"
                        paragraph
                    >
                        Konfigurieren Sie hier die Nutzerkontenanbieter, die in Ihrer Gover-Instanz global verfügbar sein sollen.
                        Die angebundenen Nutzerkonten können in Formularen als Authentifizierungsoptionen verwendet werden.
                        Unterstützt werden alle Anbieter, die eine OpenID Connect (OIDC) kompatible Schnittstelle bereitstellen.
                    </Typography>
                    <Typography
                        variant="body1"
                        paragraph
                    >
                        <strong>Mögliche Szenarien:</strong>
                    </Typography>
                    <ul>
                        <li>
                            <Typography
                                variant="body1"
                                paragraph
                            >
                                <strong>Direkt OpenID Connect kompatible IDPs</strong>
                                (z.B. BundID, BayernID, Mein Unternehmenskonto, Servicekonto SH, Keycloak, Azure AD):
                                <br />
                                → Sie können den Anbieter direkt anbinden, indem Sie die Verbindungsdaten hier hinterlegen.
                            </Typography>
                        </li>
                        <li>
                            <Typography
                                variant="body1"
                                paragraph
                            >
                                <strong>Systeme ohne OpenID Connect Unterstützung</strong>
                                (z.B. LDAP/AD, andere IDPs):
                                <br />
                                → Die Anbindung erfolgt über den integrierten Keycloak von Gover. Tragen Sie anschließend die OpenID Connect-Daten des Keycloak-Realms hier ein.
                            </Typography>
                        </li>
                        <li>
                            <Typography
                                variant="body1"
                                paragraph
                            >
                                <strong>LDAP/AD für Gover-Mitarbeitende:</strong>
                                <br />
                                → Nutzung der User Federation im Staff Realm des Gover-Keycloaks.
                                <br />
                                Diese Nutzerkonten werden nicht über die Funktion "Nutzerkontenanbieter" verwaltet.
                            </Typography>
                        </li>
                    </ul>
                    <Typography
                        variant="body1"
                        paragraph
                    >
                        Es wird empfohlen, für jeden Nutzerkontenanbieter sowohl eine produktive als auch eine vorproduktive Anbindung einzurichten, um Tests zu erleichtern.
                    </Typography>
                    <Typography
                        variant="body1"
                        paragraph
                    >
                        Die notwendigen Konfigurationsdaten erhalten Sie in der Dokumentation des Anbieters oder direkt vom Anbieter selbst.
                    </Typography>
                </>
            ),
        },
    }), []);

    const fetchIdentityProviders = useCallback((options: GenericListPropsFetchOptions<IdentityProviderListDTO>) => {
        return new IdentityProvidersApiService()
            .list(options.page, options.size, options.sort, options.order, {name: options.search});
    }, []);

    const columnDefinitions = useCallback((permissions: GenericListPagePermissionState<IdentityProviderListDTO>) => [
        {
            field: 'icon',
            headerName: '',
            renderCell: () => <CellContentWrapper><BadgeOutlinedIcon /></CellContentWrapper>,
            disableColumnMenu: true,
            width: 24,
            sortable: false,
        },
        {
            field: 'name',
            headerName: 'Name der Konfiguration',
            flex: 1,
            renderCell: (params: any) => (
                <CellLink
                    to={`/identity-providers/${params.id}`}
                    title={permissions.canUpdate(params.row) ? 'Konfiguration bearbeiten' : 'Konfiguration anzeigen'}
                >
                    {String(params.value)}
                    {params.row.isTestProvider && <Chip
                        label="Test"
                        color="warning"
                        variant="outlined"
                        size={'small'}
                        sx={{ml: 1}}
                    />}
                </CellLink>
            ),
        },
        {
            field: 'description',
            headerName: 'Beschreibung',
            flex: 2,
        },
        {
            field: 'isEnabled',
            headerName: 'Status',
            renderCell: (params: any) => (
                <>
                    {params.row.isEnabled ?
                        <Chip
                            label="Aktiv"
                            color="success"
                            variant="outlined"
                            size={'small'}
                        />
                        :
                        <Chip
                            label="Inaktiv"
                            color="default"
                            variant="outlined"
                            size={'small'}
                        />
                    }
                </>
            ),
        },
    ], []);

    const getRowIdentifier = useCallback((row: IdentityProviderListDTO) => row.key, []);

    const rowActions = useCallback((item: IdentityProviderListDTO, permissions: GenericListPagePermissionState<IdentityProviderListDTO>) => {
        const canUpdateIdentityProvider = permissions.canUpdate(item);

        return [
            {
                icon: canUpdateIdentityProvider ? <EditOutlined /> : <Visibility />,
                to: `/identity-providers/${item.key}`,
                tooltip: canUpdateIdentityProvider ? 'Konfiguration bearbeiten' : 'Konfiguration anzeigen',
            },
            {
                icon: <ScienceOutlinedIcon />,
                to: `/identity-providers/${item.key}/test`,
                tooltip: 'Konfiguration testen',
            },
        ];
    }, []);

    const noDataPlaceholder = useCallback((permissions: GenericListPagePermissionState<IdentityProviderListDTO>) => (
        <EmptyDataListPlaceholder
            title="Keine Nutzerkontenanbieter vorhanden"
            description="Nutzerkontenanbieter verbinden Gover mit Anmeldeverfahren oder Benutzerquellen wie Verzeichnisdiensten."
            addText="Neuen Nutzerkontenanbieter anlegen"
            onAdd={() => navigate('/identity-providers/new')}
            addDisabled={!permissions.canCreate}
            addDisabledTooltip={permissions.createDisabledTooltip}
        />
    ), [navigate]);

    return (
        <>
            <PageWrapper
                title="Nutzerkontenanbieter"
                fullWidth
                background
            >
                <GenericListPage<IdentityProviderListDTO>
                    header={header}
                    permissionCheck={identityProvidersListPermissionCheck}
                    searchLabel="Nutzerkontenanbieter suchen"
                    searchPlaceholder="Name der Konfiguration eingeben…"
                    fetch={fetchIdentityProviders}
                    columnDefinitions={columnDefinitions}
                    getRowIdentifier={getRowIdentifier}
                    noDataPlaceholder={noDataPlaceholder}
                    noSearchResultsPlaceholder="Keine Nutzerkontenanbieter gefunden"
                    rowActionsCount={2}
                    rowActions={rowActions}
                    defaultSortField="name"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>
        </>
    );
}
