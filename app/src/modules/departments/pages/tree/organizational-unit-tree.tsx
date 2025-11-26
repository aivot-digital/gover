import {Box, Container, Paper, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {OrganizationalUnitWithChildren, ShadowedOrganizationalUnitsApiService} from '../../shadowed-organizational-units-api-service';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {Link} from 'react-router-dom';
import {getOrgUnitTypeIcons, getOrgUnitTypeLabel} from '../../utils/org-unit-type-utils';
import Add from '@aivot/mui-material-symbols-400-outlined/dist/add/Add';
import {Actions} from '../../../../components/actions/actions';
import SubdirectoryArrowRight from '@aivot/mui-material-symbols-400-outlined/dist/subdirectory-arrow-right/SubdirectoryArrowRight';
import {NewParentIdQueryParam} from '../details/departments-details-page';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';

export function OrganizationalUnitTree() {
    const [rootOrgs, setRootOrgs] = useState<OrganizationalUnitWithChildren[]>();

    useEffect(() => {
        new ShadowedOrganizationalUnitsApiService()
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
                                <OrgTreeItem
                                    key={orgUnit.id}
                                    orgUnit={orgUnit}
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

interface OrgTreeItemProps {
    orgUnit: OrganizationalUnitWithChildren;
    index: number;
}

function OrgTreeItem(props: OrgTreeItemProps) {
    const {
        orgUnit,
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
                    {getOrgUnitTypeIcons(orgUnit.depth)}
                </Box>


                <Box
                    sx={{
                        display: 'flex',
                        ml: orgUnit.depth * 4,
                        alignItems: 'center',
                    }}
                >
                    {
                        orgUnit.depth > 0 &&
                        <SubdirectoryArrowRight
                            sx={{
                                opacity: 0.5,
                                mr: 1,
                            }}
                        />
                    }

                    <Box
                        sx={{

                            display: 'flex',
                            flexDirection: 'column',
                        }}
                    >

                        <Link
                            to={`/departments/${orgUnit.id}`}
                        >
                            {orgUnit.name}
                        </Link>

                        <Typography
                            variant="caption"
                            color="textSecondary"
                        >
                            {getOrgUnitTypeLabel(orgUnit.depth)}
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
                    {orgUnit.address}
                </Typography>

                <Actions
                    size="small"
                    dense={true}
                    actions={[
                        {
                            tooltip: `${getOrgUnitTypeLabel(orgUnit.depth + 1)} hinzufügen`,
                            icon: <Add />,
                            to: `/departments/new?${NewParentIdQueryParam}=${orgUnit.id}`,
                            variant: 'contained',
                        },
                        {
                            tooltip: 'Bearbeiten',
                            icon: <Edit />,
                            to: `/departments/${orgUnit.id}`,
                            variant: 'contained',
                        },
                    ]}
                    sx={{
                        ml: 2,
                    }}
                />
            </Box>

            {
                orgUnit.children != null &&
                orgUnit.children.length > 0 &&
                <Box
                    key={orgUnit.id}
                >
                    {
                        orgUnit
                            .children
                            .map((child, index) => (
                                <OrgTreeItem
                                    key={child.id}
                                    orgUnit={child}
                                    index={index + 1}
                                />
                            ))
                    }
                </Box>
            }
        </Box>
    );
}