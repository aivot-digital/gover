import React from 'react';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {Paper} from '@mui/material';
import {SmtpTest} from '../../../pages/staff-pages/settings/components/smtp-test/smtp-test';

export function SmtpTestPage() {
    return (
        <PageWrapper
            title="SMTP-Test"
            background={true}
            toolbarActions={[]}
        >
            <GenericPageHeader
                title="SMTP-Test"
                icon={ModuleIcons.configs}
            />

            <Paper
                sx={{
                    marginTop: 3.5,
                    padding: 2,
                }}
            >
                <SmtpTest />
            </Paper>
        </PageWrapper>
    );
}
