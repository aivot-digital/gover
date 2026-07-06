import {Button, Container, Paper, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import Add from '@aivot/mui-material-symbols-400-outlined/dist/add/Add';
import EditOutlined from '@mui/icons-material/EditOutlined';
import GroupOutlined from '@mui/icons-material/GroupOutlined';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../../components/generic-page-header/generic-page-header';
import {type Action} from '../../../../components/actions/actions-props';
import {EmptyStateSection} from '../../../../components/empty-state-section/empty-state-section';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import {DepartmentBrowser} from '../../components/department-browser';
import {NewParentIdQueryParam} from '../details/departments-details-page';
import {type VDepartmentShadowedEntityWithChildren} from '../../entities/v-department-shadowed-entity';
import {VDepartmentShadowedApiService} from '../../services/v-department-shadowed-api-service';
import {getDepartmentTypeLabel, getMaxDepartmentDepth} from '../../utils/department-utils';

export function DepartmentsListPage(): React.ReactElement {
    const rootDepartmentTypeLabel = getDepartmentTypeLabel(0);
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

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
                            disabled: !hasAccess,
                        },
                    ]}
                    helpDialog={{
                        title: 'Hilfe zu Organisationseinheiten',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Organisationseinheiten bilden die fachliche Struktur in Gover ab.
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
                        mt: 3.5,
                        mb: 4,
                        p: 2,
                        pb: 3.5,
                    }}
                >
                    <DepartmentBrowser
                        departments={rootDepartments}
                        loadError={loadError}
                        emptyState={<DepartmentsListEmptyState rootDepartmentTypeLabel={rootDepartmentTypeLabel} hasAccess={hasAccess} />}
                        getActions={(department) => getDepartmentActions(department, hasAccess)}
                        getDepartmentHref={(department) => `/departments/${department.id}`}
                    />
                </Paper>
            </Container>
        </PageWrapper>
    );
}

function getDepartmentActions(department: VDepartmentShadowedEntityWithChildren, hasAccess: boolean): Action[] {
    const maxDepartmentDepth = getMaxDepartmentDepth();
    const canAddChildDepartment = department.depth < maxDepartmentDepth;
    const canCreateChildDepartment = hasAccess && canAddChildDepartment;

    return [
        {
            tooltip: `${getDepartmentTypeLabel(department.depth + 1)} hinzufügen`,
            disabledTooltip: hasAccess
                ? `Organisationseinheiten sind auf ${maxDepartmentDepth + 1} Ebenen beschränkt.`
                : 'Dieser Bereich kann nur von Administrator:innen bearbeitet werden.',
            icon: <Add />,
            to: `/departments/new?${NewParentIdQueryParam}=${department.id}`,
            variant: 'contained',
            disabled: !canCreateChildDepartment,
        },
        {
            tooltip: hasAccess ? 'Bearbeiten' : 'Ansehen',
            icon: hasAccess ? <EditOutlined /> : <Visibility />,
            to: `/departments/${department.id}`,
            variant: 'contained',
        },
        {
            tooltip: hasAccess ? 'Mitarbeiter:innen verwalten' : 'Mitarbeiter:innen ansehen',
            icon: <GroupOutlined />,
            to: `/departments/${department.id}/members`,
        },
        {
            tooltip: 'Prozesse der Organisationseinheit ansehen',
            icon: ModuleIcons.processes,
            to: `/departments/${department.id}/processes`,
        },
    ];
}

function DepartmentsListEmptyState(props: {
    rootDepartmentTypeLabel: string;
    hasAccess: boolean;
}): React.ReactElement {
    const {
        rootDepartmentTypeLabel,
        hasAccess,
    } = props;

    return (
        <EmptyStateSection
            title="Noch keine Organisationseinheiten angelegt"
            description={(
                <>
                    Organisationseinheiten bilden die fachliche Struktur in Gover ab.
                    Legen Sie zuerst Ihre oberste Organisationseinheit an, um Hierarchien und Zugehörigkeiten zentral zu verwalten.
                </>
            )}
            actions={
                hasAccess ? (
                    <Button
                        component={Link}
                        to="/departments/new"
                        variant="contained"
                        size="small"
                        startIcon={<Add />}
                    >
                        Erste {rootDepartmentTypeLabel} anlegen
                    </Button>
                ) : undefined
            }
        />
    );
}
