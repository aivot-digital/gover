import React from 'react';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {Paper} from '@mui/material';
import {SystemInformation} from '../../../pages/staff-pages/settings/components/system-information/system-information';
import ReadinessScore from '@aivot/mui-material-symbols-400-n25-outlined/ReadinessScore';

export function SystemStatusPage() {
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
                    marginTop: 2.75,
                    padding: 2,
                }}
            >
                <SystemInformation />
            </Paper>
        </PageWrapper>
    );
}
