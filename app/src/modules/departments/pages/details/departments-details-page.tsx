import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {GenericPageHeaderPropsHelpDialog} from '../../../../components/generic-page-header/generic-page-header-props';
import {useSearchParams} from 'react-router-dom';
import {DepartmentEntity} from '../../entities/department-entity';
import {VDepartmentShadowedEntity} from '../../entities/v-department-shadowed-entity';
import {DepartmentApiService} from '../../services/department-api-service';
import {VDepartmentShadowedApiService} from '../../services/v-department-shadowed-api-service';

export const NewParentIdQueryParam = 'parentId';

export interface DepartmentsDetailsPageAdditionalData {
    shadowedDepartment: VDepartmentShadowedEntity;
}

export function DepartmentsDetailsPage() {
    const [searchParams, _] = useSearchParams();

    return (
        <PageWrapper
            title="Organisationseinheit bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<DepartmentEntity, number, DepartmentsDetailsPageAdditionalData>
                header={{
                    icon: <BusinessOutlinedIcon />,
                    title: 'Organisationseinheit bearbeiten',
                    helpDialog: HelpDialogContent,
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
                initializeItem={(api) => DepartmentApiService.initialize()}
                fetchData={(api, id: number) => new DepartmentApiService().retrieve(id)}
                fetchAdditionalData={{
                    shadowedDepartment: (api, id) => {
                        const service = new VDepartmentShadowedApiService();

                        if (id === 0 || id === 'new') {
                            const parentId = searchParams.get(NewParentIdQueryParam);

                            if (parentId != null && !isNaN(Number(parentId))) {
                                return service
                                    .retrieve(Number(parentId));
                            } else {
                                return Promise
                                    .resolve(DepartmentApiService.initialize());
                            }
                        }

                        return service
                            .retrieve(id as any);
                    },
                }}
                getTabTitle={(item) => {
                    if (item.id === 0) {
                        return 'Neue Organisationseinheit';
                    } else {
                        return item.name;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound || item == null) {
                        return 'Organisationseinheit nicht gefunden';
                    }
                    if (isNewItem) {
                        return 'Neue Organisationseinheit anlegen';
                    }
                    return `Organisationseinheit: ${item.name ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Organisationseinheiten',
                    to: '/departments',
                }}
                entityType={ServerEntityType.Departments}
            />
        </PageWrapper>
    );
}

const HelpDialogContent: GenericPageHeaderPropsHelpDialog = {
    title: 'Hilfe zu Organisationseinheiten',
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
};