import React from 'react';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {Paper} from '@mui/material';
import {ApplicationSettings} from '../../../pages/staff-pages/settings/components/application-settings/application-settings';

export function CommonSettingsPage() {
    return (
        <PageWrapper
            title="Allgemeine Einstellungen"
            background={true}
            toolbarActions={[]}
        >
            <GenericPageHeader
                title="Allgemeine Einstellungen"
                icon={ModuleIcons.configs}
            />

            <Paper
                sx={{
                    marginTop: 3.5,
                    padding: 2,
                }}
            >
                <ApplicationSettings />
            </Paper>
        </PageWrapper>
    );
}
