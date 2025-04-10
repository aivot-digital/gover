import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {DescriptionOutlined, EditOutlined, GroupOutlined} from '@mui/icons-material';
import {DepartmentsApiService} from '../../departments-api-service';
import {Department} from '../../models/department';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import {useMemo} from 'react';
import {isAdmin} from '../../../../utils/is-admin';
import {useAdminMembershipGuard} from '../../../../hooks/use-admin-membership-guard';
import {CellLink} from "../../../../components/cell-link/cell-link";

export function DepartmentsListPage() {
    const user = useSelector(selectUser);
    const userIsAdmin = useMemo(() => isAdmin(user), [user]);

    useAdminMembershipGuard();

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
                            disabled: !userIsAdmin,
                            tooltip: userIsAdmin ? undefined : 'Sie müssen globale Administrator:in sein, um diese Aktion durchführen zu können.',
                            to: '/departments/new',
                            variant: 'contained',
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
                    return new DepartmentsApiService(options.api)
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {
                                departmentName: options.search,
                                userId: isAdmin(user) ? undefined : user?.id,
                                membershipRole: isAdmin(user) ? undefined : 'Admin',
                            },
                        );
                }}
                columnDefinitions={[
                    {
                        field: 'icon',
                        headerName: '',
                        renderCell: () => <BusinessOutlinedIcon />,
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
                                title={`Fachbereich bearbeiten`}
                            >
                                {String(params.value)}
                            </CellLink>
                        )
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
                        icon: <EditOutlined />,
                        to: `/departments/${item.id}`,
                        tooltip: 'Fachbereich bearbeiten',
                    },
                    {
                        icon: <GroupOutlined />,
                        to: `/departments/${item.id}/members`,
                        tooltip: 'Mitarbeiter:innen verwalten',
                    },
                    {
                        icon: <DescriptionOutlined />,
                        to: `/departments/${item.id}/forms`,
                        tooltip: 'Formulare des Fachbereichs ansehen',
                    }
                ]}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}