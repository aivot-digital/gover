import React from 'react';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {Paper} from '@mui/material';
import {MediaSettings} from '../../../pages/staff-pages/settings/components/media-settings/media-settings';

export function MediaSettingsPage() {
    return (
        <PageWrapper
            title="Medien-Einstellungen"
            background={true}
            toolbarActions={[]}
        >
            <GenericPageHeader
                title="Medien-Einstellungen"
                icon={ModuleIcons.configs}
            />

            <Paper
                sx={{
                    marginTop: 3.5,
                    padding: 2,
                }}
            >
                <MediaSettings />
            </Paper>
        </PageWrapper>
    );
}
