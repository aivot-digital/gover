import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {Department} from '../../models/department';
import {DepartmentsApiService} from '../../departments-api-service';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectMemberships, selectUser} from '../../../../slices/user-slice';
import {isAdmin, isDepartmentAdmin} from '../../../../utils/is-admin';

export function DepartmentsDetailsPage() {
    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);

    return (
        <PageWrapper
            title="Fachbereich bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<Department, number, undefined>
                isEditable={(item) => (
                    isAdmin(user) || isDepartmentAdmin(memberships, item?.id)
                )}
                header={{
                    icon: <BusinessOutlinedIcon />,
                    title: 'Fachbereich bearbeiten',
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
                tabs={[
                    {
                        path: '/departments/:id',
                        label: 'Allgemeine Angaben',
                    },
                    {
                        path: '/departments/:id/members',
                        label: 'Mitarbeiter:innen',
                        isDisabled: (item) => !item?.id,
                    },
                    {
                        path: '/departments/:id/forms',
                        label: 'Formulare',
                        isDisabled: (item) => !item?.id,
                    },
                ]}
                initializeItem={(api) => new DepartmentsApiService().initialize()}
                fetchData={(api, id: number) => new DepartmentsApiService().retrieve(id)}
                getTabTitle={(item: Department) => {
                    if (item.id === 0) {
                        return 'Neuer Fachbereich';
                    } else {
                        return item.name;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) {
                        return 'Fachbereich nicht gefunden';
                    }
                    if (isNewItem) {
                        return 'Neuen Fachbereich anlegen';
                    }
                    return `Fachbereich: ${item?.name ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Fachbereiche',
                    to: '/departments',
                }}
                entityType={ServerEntityType.Departments}
            />
        </PageWrapper>
    );
}