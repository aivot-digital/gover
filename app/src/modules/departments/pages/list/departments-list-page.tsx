import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {DescriptionOutlined, EditOutlined, GroupOutlined} from '@mui/icons-material';
import {type DepartmentResponseDTO as Department} from '../../dtos/department-response-dto';
import {selectUser} from '../../../../slices/user-slice';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {ShadowedOrganizationalUnitsApiService} from '../../shadowed-organizational-units-api-service';

export function DepartmentsListPage() {
    const user = useAppSelector(selectUser);
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    return (
        <PageWrapper
            title="Fachbereiche"
            fullWidth
            background
        >
            <GenericListPage<Department>
                header={{
                    icon: <BusinessOutlinedIcon />,
                    title: 'Fachbereiche',
                    actions: [
                        {
                            label: 'Neuer Fachbereich',
                            icon: <AddOutlinedIcon />,
                            to: '/departments/new',
                            variant: 'contained',
                            disabled: !hasAccess,
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Fachbereichen',
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
                searchLabel="Fachbereich suchen"
                searchPlaceholder="Name des Fachbereichs eingeben…"
                fetch={(options) => {
                    return new ShadowedOrganizationalUnitsApiService()
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {
                                departmentName: options.search,
                                userId: hasAccess ? undefined : user?.id,
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
                                to={`/departments/${params.id}`}
                                title={hasAccess ? 'Fachbereich bearbeiten' : 'Fachbereich ansehen'}
                            >
                                {String(params.value)}
                            </CellLink>
                        ),
                    },
                    {
                        field: 'address',
                        headerName: 'Adresse',
                        flex: 2,
                    },
                ]}
                getRowIdentifier={row => row.id.toString()}
                noDataPlaceholder="Keine Fachbereiche angelegt"
                noSearchResultsPlaceholder="Keine Fachbereiche gefunden"
                rowActionsCount={3}
                rowActions={(item: Department) => [
                    {
                        icon: hasAccess ? <EditOutlined /> : <Visibility />,
                        to: `/departments/${item.id}`,
                        tooltip: hasAccess ? 'Fachbereich bearbeiten' : 'Fachbereich ansehen',
                    },
                    {
                        icon: <GroupOutlined />,
                        to: `/departments/${item.id}/members`,
                        tooltip: hasAccess ? 'Mitarbeiter:innen verwalten' : 'Mitarbeiter:innen ansehen',
                    },
                    {
                        icon: <DescriptionOutlined />,
                        to: `/departments/${item.id}/forms`,
                        tooltip: 'Formulare des Fachbereichs ansehen',
                    },
                ]}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}