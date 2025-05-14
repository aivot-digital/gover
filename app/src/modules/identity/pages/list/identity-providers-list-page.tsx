import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import {IdentityProviderListDTO} from '../../models/identity-provider-list-dto';
import Chip from "@mui/material/Chip";

export function IdentityProvidersListPage() {
    useAdminGuard();

    return (
        <>
            <PageWrapper
                title="Nutzerkontenanbieter"
                fullWidth
                background
            >
                <GenericListPage<IdentityProviderListDTO>
                    header={{
                        icon: <BadgeOutlinedIcon />,
                        title: 'Nutzerkontenanbieter',
                        actions: [
                            {
                                label: 'Neuer Nutzerkontenanbieter',
                                icon: <AddOutlinedIcon />,
                                to: '/identity-providers/new',
                                variant: 'contained',
                            },
                        ],
                        helpDialog: {
                            title: 'Hilfe zu Nutzerkontenanbietern',
                            tooltip: 'Hilfe anzeigen',
                            content: (
                                <>
                                    <Typography variant="body1" paragraph>
                                        Konfigurieren Sie hier die Nutzerkontenanbieter, die in Ihrer Gover-Instanz global verfügbar sein sollen.
                                        Die angebundenen Nutzerkonten können in Formularen als Authentifizierungsoptionen verwendet werden.
                                        Unterstützt werden alle Anbieter, die eine OpenID Connect (OIDC) kompatible Schnittstelle bereitstellen.
                                    </Typography>
                                    <Typography variant="body1" paragraph>
                                        <strong>Mögliche Szenarien:</strong>
                                    </Typography>
                                    <ul>
                                        <li>
                                            <Typography variant="body1" paragraph>
                                                <strong>Direkt OpenID Connect kompatible IDPs</strong> (z.B. BundID, BayernID, Mein Unternehmenskonto, Servicekonto SH, Keycloak, Azure AD):<br />
                                                → Sie können den Anbieter direkt anbinden, indem Sie die Verbindungsdaten hier hinterlegen.
                                            </Typography>
                                        </li>
                                        <li>
                                            <Typography variant="body1" paragraph>
                                                <strong>Systeme ohne OpenID Connect Unterstützung</strong> (z.B. LDAP/AD, andere IDPs):<br />
                                                → Die Anbindung erfolgt über den integrierten Keycloak von Gover. Tragen Sie anschließend die OpenID Connect-Daten des Keycloak-Realms hier ein.
                                            </Typography>
                                        </li>
                                        <li>
                                            <Typography variant="body1" paragraph>
                                                <strong>LDAP/AD für Gover-Mitarbeitende:</strong><br />
                                                → Nutzung der User Federation im Staff Realm des Gover-Keycloaks.<br />
                                                Diese Nutzerkonten werden nicht über die Funktion "Nutzerkontenanbieter" verwaltet.
                                            </Typography>
                                        </li>
                                    </ul>
                                    <Typography variant="body1" paragraph>
                                        Es wird empfohlen, für jeden Nutzerkontenanbieter sowohl eine produktive als auch eine vorproduktive Anbindung einzurichten, um Tests zu erleichtern.
                                    </Typography>
                                    <Typography variant="body1" paragraph>
                                        Die notwendigen Konfigurationsdaten erhalten Sie in der Dokumentation des Anbieters oder direkt vom Anbieter selbst.
                                    </Typography>
                                </>
                            ),
                        },
                    }}
                    searchLabel="Nutzerkontenanbieter suchen"
                    searchPlaceholder="Name der Konfiguration eingeben…"
                    fetch={(options) => {
                        return new IdentityProvidersApiService(options.api)
                            .list(options.page, options.size, options.sort, options.order, {name: options.search});
                    }}
                    columnDefinitions={[
                        {
                            field: 'icon',
                            headerName: '',
                            renderCell: () => <BadgeOutlinedIcon />,
                            disableColumnMenu: true,
                            width: 24,
                            sortable: false,
                        },
                        {
                            field: 'name',
                            headerName: 'Name der Konfiguration',
                            flex: 1,
                            renderCell: (params) => (
                                <CellLink
                                    to={`/identity-providers/${params.id}`}
                                    title={`Konfiguration bearbeiten`}
                                >
                                    {String(params.value)}
                                    {params.row.isTestProvider && <Chip label="Test" color="warning" variant="outlined" size={"small"} sx={{ml:1}}/>}
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
                            renderCell: (params) => (
                                <>
                                    {params.row.isEnabled ?
                                        <Chip label="Aktiv" color="success" variant="outlined" size={"small"}/>
                                        :
                                        <Chip label="Inaktiv" color="default" variant="outlined" size={"small"}/>
                                    }
                                </>
                            ),
                        },
                    ]}
                    getRowIdentifier={row => row.key}
                    noDataPlaceholder="Keine Nutzerkontenanbieter angelegt"
                    noSearchResultsPlaceholder="Keine Nutzerkontenanbieter gefunden"
                    rowActionsCount={2}
                    rowActions={(item: IdentityProviderListDTO) => [
                        {
                            icon: <EditOutlined />,
                            to: `/identity-providers/${item.key}`,
                            tooltip: 'Konfiguration bearbeiten',
                        },
                        {
                            icon: <ScienceOutlinedIcon />,
                            to: `/identity-providers/${item.key}/test`,
                            tooltip: 'Konfiguration testen',
                        }]}
                    defaultSortField="name"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>
        </>
    );
}