import {Box, Container, Paper, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {Link} from 'react-router-dom';
import Add from '@aivot/mui-material-symbols-400-outlined/dist/add/Add';
import {Actions} from '../../../../components/actions/actions';
import SubdirectoryArrowRight from '@aivot/mui-material-symbols-400-outlined/dist/subdirectory-arrow-right/SubdirectoryArrowRight';
import {NewParentIdQueryParam} from '../details/departments-details-page';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import {VDepartmentShadowedApiService} from '../../services/v-department-shadowed-api-service';
import {VDepartmentShadowedEntityWithChildren} from '../../entities/v-department-shadowed-entity';
import {getDepartmentTypeIcons, getDepartmentTypeLabel} from '../../utils/department-utils';

export function DepartmentTree() {
    const [rootOrgs, setRootOrgs] = useState<VDepartmentShadowedEntityWithChildren[]>();

    useEffect(() => {
        new VDepartmentShadowedApiService()
            .retrieveOrgTree()
            .then(setRootOrgs);
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
                            label: 'Neue Organisation',
                            icon: <Add />,
                            to: '/departments/new',
                            variant: 'contained',
                        },
                    ]}
                />

                <Paper
                    sx={{
                        mt: 2,
                        mb: 4,
                        p: 2,
                    }}
                >
                    {
                        rootOrgs != null ?
                            rootOrgs.map((orgUnit, index) => (
                                <DepartmentTreeItem
                                    key={orgUnit.id}
                                    department={orgUnit}
                                    index={index}
                                />
                            ))
                            :
                            'Lade...'
                    }
                </Paper>

            </Container>
        </PageWrapper>
    );
}

interface DepartmentTreeItemProps {
    department: VDepartmentShadowedEntityWithChildren;
    index: number;
}

function DepartmentTreeItem(props: DepartmentTreeItemProps) {
    const {
        department,
        index,
    } = props;

    return (
        <Box>
            <Box
                sx={{
                    display: 'flex',
                    borderTop: index === 0 ? undefined : '1px solid #eee',
                    alignItems: 'center',
                    py: 1,
                }}
            >
                <Box
                    sx={{
                        mr: 2,
                        display: 'flex',
                        alignItems: 'center',
                    }}
                >
                    {getDepartmentTypeIcons(department.depth)}
                </Box>


                <Box
                    sx={{
                        display: 'flex',
                        ml: department.depth * 4,
                        alignItems: 'center',
                    }}
                >
                    {
                        department.depth > 0 &&
                        <SubdirectoryArrowRight
                            sx={{
                                opacity: 0.5,
                                mr: 1,
                            }}
                        />
                    }

                    <Box
                        component={Link}
                        to={`/departments/${department.id}`}
                        sx={{
                            display: 'flex',
                            flexDirection: 'column',
                            textDecoration: 'none',
                        }}
                    >

                        <Typography
                            variant="body1"
                            color="textPrimary"
                        >
                            {department.name}
                        </Typography>

                        <Typography
                            variant="caption"
                            color="textSecondary"
                        >
                            {getDepartmentTypeLabel(department.depth)}
                        </Typography>
                    </Box>
                </Box>

                <Typography
                    sx={{
                        ml: 'auto',
                        display: 'flex',
                        alignItems: 'center',
                        whiteSpace: 'nowrap',
                    }}
                    color="textSecondary"
                >
                    {department.address}
                </Typography>

                <Actions
                    size="small"
                    dense={true}
                    actions={[
                        {
                            tooltip: `${getDepartmentTypeLabel(department.depth + 1)} hinzufügen`,
                            icon: <Add />,
                            to: `/departments/new?${NewParentIdQueryParam}=${department.id}`,
                            variant: 'contained',
                        },
                        {
                            tooltip: 'Bearbeiten',
                            icon: <Edit />,
                            to: `/departments/${department.id}`,
                            variant: 'contained',
                        },
                    ]}
                    sx={{
                        ml: 2,
                    }}
                />
            </Box>

            {
                department.children != null &&
                department.children.length > 0 &&
                <Box
                    key={department.id}
                >
                    {
                        department
                            .children
                            .map((child, index) => (
                                <DepartmentTreeItem
                                    key={child.id}
                                    department={child}
                                    index={index + 1}
                                />
                            ))
                    }
                </Box>
            }
        </Box>
    );
}