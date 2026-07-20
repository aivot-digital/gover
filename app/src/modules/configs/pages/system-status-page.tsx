import React from 'react';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {Paper} from '@mui/material';
import {SystemInformation} from '../../../pages/staff-pages/settings/components/system-information/system-information';
import ReadinessScore from '@aivot/mui-material-symbols-400-n25-outlined/ReadinessScore';
import {useRequireSystemPermission} from '../../permissions/hooks/use-permissions';
import {Permission} from '../../../data/permissions/permission';

export function SystemStatusPage() {
    useRequireSystemPermission(Permission.SYSTEM_CONFIG_READ);

    return (
        <PageWrapper
            title="Systeminformationen"
            background={true}
        >
            <GenericPageHeader
                title="Systeminformationen"
                icon={<ReadinessScore/>}
            />

            <Paper
                sx={{
                    marginTop: 3.5,
                    padding: 2,
                }}
            >
                <SystemInformation />
            </Paper>
        </PageWrapper>
    );
}
