import {Box, Chip, Typography} from '@mui/material';
import {type GridColDef} from '@mui/x-data-grid';
import {CellLink} from '../../../components/cell-link/cell-link';
import {EmptyDataListPlaceholder} from '../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {GenericList} from '../../../components/generic-list/generic-list';
import {useGenericDetailsPageContext} from '../../../components/generic-details-page/generic-details-page-context';
import {Permission} from '../../../data/permissions/permission';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {IdentityProvidersApiService} from '../../identity/identity-providers-api-service';
import {type IdentityProviderListDTO} from '../../identity/models/identity-provider-list-dto';
import {requireSystemPermission} from '../../permissions/utils/permission-utils';
import {selectPermissions} from '../../../slices/user-slice';
import {type CommunicationProvider} from '../models';

const columns: GridColDef<IdentityProviderListDTO>[] = [
    {
        field: 'name',
        headerName: 'Nutzerkontenanbieter',
        flex: 1,
        renderCell: (params) => (
            <CellLink
                to={`/identity-providers/${params.row.key}`}
                title="Nutzerkontenanbieter anzeigen"
            >
                {String(params.value)}
                {params.row.isTestProvider && (
                    <Chip
                        label="Test"
                        color="warning"
                        variant="outlined"
                        size="small"
                        sx={{ml: 1}}
                    />
                )}
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
            <Chip
                label={params.row.isEnabled ? 'Aktiv' : 'Inaktiv'}
                color={params.row.isEnabled ? 'success' : 'default'}
                variant="outlined"
                size="small"
            />
        ),
    },
];

export function CommunicationProviderDetailsPageIdentityProviders() {
    const permissions = useAppSelector(selectPermissions);
    const {
        item: communicationProvider,
    } = useGenericDetailsPageContext<CommunicationProvider, void>();

    if (communicationProvider == null) {
        return null;
    }

    requireSystemPermission(permissions, Permission.IDENTITY_PROVIDER_READ);

    return (
        <Box sx={{pt: 2}}>
            <Typography variant="h5" sx={{mb: 1}}>
                Verknüpfte Nutzerkontenanbieter
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Eine Übersicht der Nutzerkontenanbieter, die diesen Kommunikationsanbieter verwenden.
            </Typography>

            <GenericList<IdentityProviderListDTO>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columns}
                fetch={(options) => new IdentityProvidersApiService().list(
                    options.page,
                    options.size,
                    options.sort,
                    options.order,
                    {
                        name: options.search,
                        communicationProviderId: communicationProvider.id,
                    },
                )}
                getRowIdentifier={(identityProvider) => identityProvider.key}
                searchLabel="Nutzerkontenanbieter suchen"
                searchPlaceholder="Name des Nutzerkontenanbieters eingeben…"
                defaultSortField="name"
                noDataPlaceholder={(
                    <EmptyDataListPlaceholder
                        title="Keine Nutzerkontenanbieter verknüpft"
                        description="Dieser Kommunikationsanbieter wird aktuell von keinem Nutzerkontenanbieter verwendet."
                    />
                )}
                loadingPlaceholder="Lade Nutzerkontenanbieter…"
                noSearchResultsPlaceholder="Keine Nutzerkontenanbieter gefunden"
            />
        </Box>
    );
}
