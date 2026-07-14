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
import {useCallback, useMemo} from 'react';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectPermissions} from '../../../../slices/user-slice';
import {Permission} from '../../../../data/permissions/permission';
import {
    checkDepartmentPermission,
    checkSystemPermission,
    formatMissingPermissionTooltip,
    hasDepartmentPermission,
    hasSystemPermission,
} from '../../../permissions/utils/permission-utils';

export const NewParentIdQueryParam = 'parentId';

export interface DepartmentsDetailsPageAdditionalData {
    shadowedDepartment: VDepartmentShadowedEntity;
}

export function DepartmentsDetailsPage() {
    const [searchParams, _] = useSearchParams();
    const permissions = useAppSelector(selectPermissions);
    const parentOrgUnitId = useMemo(() => {
        const parentId = searchParams.get(NewParentIdQueryParam);
        return parentId != null && !isNaN(Number(parentId)) ? Number(parentId) : undefined;
    }, [searchParams]);

    const isEditable = useCallback((item: DepartmentEntity | undefined) => {
        if (item == null) {
            return false;
        }

        if (item.id === 0) {
            return parentOrgUnitId != null
                ? checkDepartmentPermission(permissions, parentOrgUnitId, Permission.DEPARTMENT_CREATE)
                : checkSystemPermission(permissions, Permission.DEPARTMENT_CREATE);
        }

        return checkDepartmentPermission(permissions, item.id, Permission.DEPARTMENT_UPDATE);
    }, [parentOrgUnitId, permissions]);
    const hasAccess = useCallback((item: DepartmentEntity | undefined) => {
        if (item == null) {
            return;
        }

        if (item.id === 0) {
            if (parentOrgUnitId != null) {
                hasDepartmentPermission(permissions, parentOrgUnitId, Permission.DEPARTMENT_CREATE);
            } else {
                hasSystemPermission(permissions, Permission.DEPARTMENT_CREATE);
            }
            return;
        }

        hasDepartmentPermission(permissions, item.id, Permission.DEPARTMENT_READ);
    }, [parentOrgUnitId, permissions]);

    return (
        <PageWrapper
            title="Organisationseinheit bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<DepartmentEntity, number, DepartmentsDetailsPageAdditionalData>
                hasAccess={hasAccess}
                isEditable={isEditable}
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
                        isDisabled: (item) => !item?.id || !checkDepartmentPermission(permissions, item.id, Permission.DEPARTMENT_MEMBERSHIP_READ),
                        disabledTooltip: (item) => item?.id
                            ? formatMissingPermissionTooltip(Permission.DEPARTMENT_MEMBERSHIP_READ)
                            : undefined,
                    },
                    {
                        path: '/departments/:id/processes',
                        label: 'Verwaltete Prozesse',
                        isDisabled: (item) => !item?.id || !checkDepartmentPermission(permissions, item.id, Permission.PROCESS_DEFINITION_READ),
                        disabledTooltip: (item) => item?.id && !checkDepartmentPermission(permissions, item.id, Permission.PROCESS_DEFINITION_READ)
                            ? formatMissingPermissionTooltip(Permission.PROCESS_DEFINITION_READ)
                            : undefined,
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
                Eine Organisationseinheit ist eine zentrale Verwaltungseinheit in Gover und essenziell für den Betrieb der Anwendung. Sie speichert wichtige Stammdaten wie Adress- und Kontaktdaten sowie rechtliche Informationen (z.
                B. Impressum, Datenschutzerklärung), die in Formularen wiederverwendet werden können.
            </Typography>
            <Typography sx={{mt: 2}}>
                Einer Organisationseinheit sind Mitarbeiter:innen mit einer spezifischen Rolle zugeordnet, die deren Berechtigungen innerhalb der Organisationseinheit definiert.
            </Typography>
        </>
    ),
};
