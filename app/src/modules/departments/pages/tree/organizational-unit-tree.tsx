import {Box, Container, Paper} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {OrganizationalUnitWithChildren, ShadowedOrganizationalUnitsApiService} from '../../shadowed-organizational-units-api-service';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {Link} from 'react-router-dom';

export function OrganizationalUnitTree() {
    const [rootOrgs, setRootOrgs] = useState<OrganizationalUnitWithChildren[]>();

    useEffect(() => {
        new ShadowedOrganizationalUnitsApiService()
            .retrieveOrgTree()
            .then(setRootOrgs);
    }, []);

    return (
        <PageWrapper
            title="Übersicht"
            fullWidth
            background
        >
            <Container>
                <GenericPageHeader
                    icon={ModuleIcons.departments}
                    title="Übersicht"
                    actions={[]}
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
                            rootOrgs.map((orgUnit) => (
                                <OrgTreeItem
                                    key={orgUnit.id}
                                    orgUnit={orgUnit}
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
}

function OrgTreeItem(props: OrgTreeItemProps) {
    const {
        orgUnit,
    } = props;

    return (
        <Box>
            <Link
                to={`/departments/${orgUnit.id}`}
            >
                {orgUnit.name}
            </Link>

            {
                orgUnit.children != null &&
                orgUnit.children.length > 0 &&
                <Box
                    sx={{
                        pl: 4,
                    }}
                >
                    {
                        orgUnit
                            .children
                            .map((child) => (
                                <OrgTreeItem
                                    key={child.id}
                                    orgUnit={child}
                                />
                            ))
                    }
                </Box>
            }
        </Box>
    );
}