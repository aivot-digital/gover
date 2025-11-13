import React from 'react';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {Paper} from '@mui/material';
import Extension from '@aivot/mui-material-symbols-400-outlined/dist/extension/Extension';
import {Extensions} from '../../../pages/staff-pages/settings/components/extensions/extensions';

export function ExtensionsPage() {
    return (
        <PageWrapper
            title="Erweiterungen"
            background={true}
        >
            <GenericPageHeader
                title="Erweiterungen"
                icon={<Extension/>}
            />

            <Paper
                sx={{
                    marginTop: 3.5,
                    padding: 2,
                }}
            >
                <Extensions />
            </Paper>
        </PageWrapper>
    );
}
