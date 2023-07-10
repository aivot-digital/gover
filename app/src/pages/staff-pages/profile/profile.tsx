import { Box } from '@mui/material';
import React from 'react';
import { ProfilePasswordChange } from './components/profile-password-change';
import { ProfileInformationChange } from './components/profile-information-change';
import { PageWrapper } from '../../../components/page-wrapper/page-wrapper';

export function Profile(): JSX.Element {
    return (
        <PageWrapper
            title="Profileinstellungen"
        >
            <ProfileInformationChange/>

            <Box sx={ {mt: 4} }>
                <ProfilePasswordChange/>
            </Box>
        </PageWrapper>
    );
}
