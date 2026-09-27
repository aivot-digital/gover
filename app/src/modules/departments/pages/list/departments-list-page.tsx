import {Button, Container, Paper, Typography} from '@mui/material';
import React, {useCallback, useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import GroupOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Group';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../../components/generic-page-header/generic-page-header';
import {type Action} from '../../../../components/actions/actions-props';
import {EmptyStateSection} from '../../../../components/empty-state-section/empty-state-section';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {DepartmentBrowser} from '../../components/department-browser';
import {NewParentIdQueryParam} from '../details/departments-details-page';
import {type VDepartmentShadowedEntityWithChildren} from '../../entities/v-department-shadowed-entity';
import {VDepartmentShadowedApiService} from '../../services/v-department-shadowed-api-service';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectPermissions} from '../../../../slices/user-slice';
import {Permission} from '../../../../data/permissions/permission';
import {
    hasDepartmentPermission,
    hasSystemPermission,
    formatMissingPermissionTooltip,
} from '../../../permissions/utils/permission-utils';
import {type PermissionSet} from '../../../permissions/models/permission-set';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';
import {getDepartmentTypeLabel, getMaxDepartmentDepth} from '../../utils/department-utils';

export function DepartmentsListPage(): React.ReactElement {
    const rootDepartmentTypeLabel = getDepartmentTypeLabel(0);
    const permissions = useAppSelector(selectPermissions);
    const canCreateRootDepartment = hasSystemPermission(permissions, Permission.DEPARTMENT_CREATE);

    const [
        rootDepartments,
        setRootDepartments,
    ] = useState<VDepartmentShadowedEntityWithChildren[]>();
    const [
        loadError,
        setLoadError,
    ] = useState(false);

    useEffect(() => {
        void new VDepartmentShadowedApiService()
            .retrieveOrgTree()
            .then((items) => {
                setRootDepartments(items);
                setLoadError(false);
            })
            .catch((err) => {
                console.error(err);
                setRootDepartments([]);
                setLoadError(true);
            });
    }, []);

    const getDepartmentHref = useCallback((department: VDepartmentShadowedEntityWithChildren) => {
        return hasDepartmentPermission(permissions, department.id, Permission.DEPARTMENT_READ)
            ? `/departments/${department.id}`
            : undefined;
    }, [permissions]);

    const getActions = useCallback((department: VDepartmentShadowedEntityWithChildren) => {
        return getDepartmentActions(department, permissions);
    }, [permissions]);

    return (
        <PageWrapper
            title="Organisationseinheiten"
            fullWidth
            background
        >
            <Container>
                <GenericPageHeader
                    icon={ModuleIcons.departments}
                    title="Organisationseinheiten"
                    actions={[
                        {
                            label: `Neue ${rootDepartmentTypeLabel}`,
                            icon: <Add />,
                            to: '/departments/new',
                            variant: 'contained',
                            disabled: !canCreateRootDepartment,
                            disabledTooltip: formatMissingPermissionTooltip(Permission.DEPARTMENT_CREATE),
                        },
                    ]}
                    helpDialog={{
                        title: 'Hilfe zu Organisationseinheiten',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Organisationseinheiten bilden die fachliche Struktur in Prosuna ab.
                                    In dieser Baumansicht sehen Sie die Hierarchie vom übergeordneten Bereich bis zu den untergeordneten Einheiten.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Jede Organisationseinheit kann Stammdaten wie Name, Typ und Adresse enthalten.
                                    Diese Informationen werden in Fachprozessen wiederverwendet und erleichtern eine konsistente Verwaltung.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Über die Aktionsbuttons können Sie direkt Untereinheiten anlegen, Einheiten bearbeiten,
                                    Mitarbeitende verwalten und die zugehörigen Prozesse einsehen.
                                    Mit der Suche finden Sie Einheiten nach Name, Adresse oder Typ.
                                </Typography>
                            </>
                        ),
                    }}
                />

                <Paper
                    sx={{
                        mt: 2.75,
                        mb: 4,
                        p: 2,
                        pb: 3.5,
                    }}
                >
                    <DepartmentBrowser
                        departments={rootDepartments}
                        loadError={loadError}
                        emptyState={<DepartmentsListEmptyState canCreateRootDepartment={canCreateRootDepartment} rootDepartmentTypeLabel={rootDepartmentTypeLabel} />}
                        getActions={getActions}
                        getDepartmentHref={getDepartmentHref}
                    />
                </Paper>
            </Container>
        </PageWrapper>
    );
}

function getDepartmentActions(
    department: VDepartmentShadowedEntityWithChildren,
    permissions: PermissionSet | undefined,
): Action[] {
    const maxDepartmentDepth = getMaxDepartmentDepth();
    const canAddChildDepartment = department.depth < maxDepartmentDepth;
    const canCreateChildDepartment = hasSystemPermission(permissions, Permission.DEPARTMENT_CREATE) && canAddChildDepartment;
    const canReadDepartment = hasDepartmentPermission(permissions, department.id, Permission.DEPARTMENT_READ);
    const canUpdateDepartment = hasDepartmentPermission(permissions, department.id, Permission.DEPARTMENT_UPDATE);
    const canReadMemberships = hasDepartmentPermission(permissions, department.id, Permission.DEPARTMENT_MEMBERSHIP_READ);
    const canReadProcesses = hasDepartmentPermission(permissions, department.id, Permission.PROCESS_DEFINITION_READ);

    return [
        {
            tooltip: `${getDepartmentTypeLabel(department.depth + 1)} hinzufügen`,
            disabledTooltip: canAddChildDepartment
                ? formatMissingPermissionTooltip(Permission.DEPARTMENT_CREATE)
                : `Organisationseinheiten sind auf ${maxDepartmentDepth + 1} Ebenen beschränkt.`,
            icon: <Add />,
            to: `/departments/new?${NewParentIdQueryParam}=${department.id}`,
            variant: 'contained',
            disabled: !canCreateChildDepartment,
        },
        {
            tooltip: canUpdateDepartment ? 'Bearbeiten' : 'Ansehen',
            disabledTooltip: formatMissingPermissionTooltip(Permission.DEPARTMENT_READ),
            icon: canUpdateDepartment ? <EditOutlined /> : <Visibility />,
            to: `/departments/${department.id}`,
            variant: 'contained',
            disabled: !canReadDepartment,
        },
        {
            tooltip: 'Mitarbeiter:innen ansehen',
            disabledTooltip: formatMissingPermissionTooltip(Permission.DEPARTMENT_MEMBERSHIP_READ),
            icon: <GroupOutlined />,
            to: `/departments/${department.id}/members`,
            disabled: !canReadMemberships,
        },
        {
            tooltip: 'Prozesse der Organisationseinheit ansehen',
            disabledTooltip: !canReadDepartment
                ? formatMissingPermissionTooltip(Permission.DEPARTMENT_READ)
                : formatMissingPermissionTooltip(Permission.PROCESS_DEFINITION_READ),
            icon: ModuleIcons.processes,
            to: `/departments/${department.id}/processes`,
            disabled: !canReadDepartment || !canReadProcesses,
        },
    ];
}

function DepartmentsListEmptyState(props: {
    canCreateRootDepartment: boolean;
    rootDepartmentTypeLabel: string;
}): React.ReactElement {
    const {
        canCreateRootDepartment,
        rootDepartmentTypeLabel,
    } = props;

    return (
        <EmptyStateSection
            title="Keine Organisationseinheiten im Zugriff"
            description={(
                <>
                    Es wurden keine Organisationseinheiten gefunden, auf die Sie Zugriff haben.
                    Möglicherweise wurden noch keine Organisationseinheiten angelegt oder Ihnen fehlen
                    die erforderlichen Leseberechtigungen.
                </>
            )}
            actions={(
                <DisabledTooltip
                    title={!canCreateRootDepartment ? formatMissingPermissionTooltip(Permission.DEPARTMENT_CREATE) : ''}
                    disabled={!canCreateRootDepartment}
                >
                    <Button
                        component={Link}
                        to="/departments/new"
                        variant="contained"
                        size="small"
                        startIcon={<Add />}
                        disabled={!canCreateRootDepartment}
                    >
                        Erste {rootDepartmentTypeLabel} anlegen
                    </Button>
                </DisabledTooltip>
            )}
        />
    );
}
