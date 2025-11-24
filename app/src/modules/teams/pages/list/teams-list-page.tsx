import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined, GroupOutlined} from '@mui/icons-material';
import {selectUser} from '../../../../slices/user-slice';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {TeamResponseDTO} from '../../dtos/team-response-dto';
import {TeamsApiService} from '../../teams-api-service';

export function TeamsListPage() {
    const user = useAppSelector(selectUser);
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    return (
        <PageWrapper
            title="Teams"
            fullWidth
            background
        >
            <GenericListPage<TeamResponseDTO>
                header={{
                    icon: <BusinessOutlinedIcon />,
                    title: 'Teams',
                    actions: [
                        {
                            label: 'Neues Team',
                            icon: <AddOutlinedIcon />,
                            to: '/teams/new',
                            variant: 'contained',
                            disabled: !hasAccess,
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Teams',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Ein Fachbereich ist eine zentrale Verwaltungseinheit in Gover und essenziell für den Betrieb der Anwendung. Er speichert wichtige Stammdaten wie Adress- und Kontaktdaten sowie rechtliche Informationen (z.
                                    B. Impressum, Datenschutzerklärung), die in Formularen wiederverwendet werden können.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Jedem Fachbereich sind Mitarbeiter:innen mit einer spezifischen Rolle zugeordnet, die deren Berechtigungen innerhalb des Fachbereichs definiert.
                                </Typography>
                            </>
                        ),
                    },
                }}
                searchLabel="Team suchen"
                searchPlaceholder="Name des Teams eingeben…"
                fetch={(options) => {
                    return new TeamsApiService()
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {
                                name: options.search,
                            },
                        );
                }}
                columnDefinitions={[
                    {
                        field: 'icon',
                        headerName: '',
                        renderCell: () => <CellContentWrapper><BusinessOutlinedIcon /></CellContentWrapper>,
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
                                to={`/teams/${params.id}`}
                                title={hasAccess ? 'Team bearbeiten' : 'Team ansehen'}
                            >
                                {String(params.value)}
                            </CellLink>
                        ),
                    },
                ]}
                getRowIdentifier={row => row.id.toString()}
                noDataPlaceholder="Keine Team angelegt"
                noSearchResultsPlaceholder="Keine Teams gefunden"
                rowActionsCount={3}
                rowActions={(item: TeamResponseDTO) => [
                    {
                        icon: hasAccess ? <EditOutlined /> : <Visibility />,
                        to: `/teams/${item.id}`,
                        tooltip: hasAccess ? 'Team bearbeiten' : 'Team ansehen',
                    },
                    {
                        icon: <GroupOutlined />,
                        to: `/teams/${item.id}/members`,
                        tooltip: hasAccess ? 'Teammitglieder verwalten' : 'Teammitglieder ansehen',
                    },
                ]}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}