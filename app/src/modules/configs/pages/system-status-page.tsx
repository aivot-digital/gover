import React from 'react';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {Paper} from '@mui/material';
import {SystemInformation} from '../../../pages/staff-pages/settings/components/system-information/system-information';
import ReadinessScore from '@aivot/mui-material-symbols-400-outlined/dist/readiness-score/ReadinessScore';

export function SystemStatusPage() {
    return (
        <PageWrapper
            title="Systemstatus"
            background={true}
            toolbarActions={[]}
        >
            <GenericPageHeader
                title="Systemstatus"
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
