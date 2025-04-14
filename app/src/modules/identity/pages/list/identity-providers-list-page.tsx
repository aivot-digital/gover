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
                            title: 'Hilfe zu Nutzerkontenanbieter',
                            tooltip: 'Hilfe anzeigen',
                            content: (
                                <>
                                    <Typography
                                        variant="body1"
                                        paragraph
                                    >
                                        Konfigurieren Sie hier Nutzerkontenanbieter, die in Ihrer Gover-Instanz global verfügbar sein sollen.
                                        Die erforderlichen Konfigurationsdaten erhalten Sie vom Nutzerkontenanbieter oder finden Sie in dessen Dokumentation.
                                    </Typography>
                                    <Typography
                                        variant="body1"
                                        paragraph
                                    >
                                        Es wird empfohlen, für jeden Nutzerkontenanbieter sowohl eine produktive als auch eine vorproduktive Anbindung einzurichten, um Tests zu erleichtern.
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