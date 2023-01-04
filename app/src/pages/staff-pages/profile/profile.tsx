import {Box, Container} from '@mui/material';
import React from 'react';
import {useAuthGuard} from '../../../hooks/use-auth-guard';
import {ProfilePasswordChange} from './components/profile-password-change';
import {ProfileInformationChange} from './components/profile-information-change';
import {AppToolbar} from '../../../components/app-toolbar/app-toolbar';
import strings from './profile-strings.json';
import {AppFooter} from '../../../components/app-footer/app-footer';
import {AppMode} from '../../../data/app-mode';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {Localization} from '../../../locale/localization';

const __ = Localization(strings);

export function Profile() {
    useAuthGuard();

    return (
        <>
            <MetaElement title={__.title}/>
            <AppToolbar
                title={__.title}
                parentPath="/overview"
            />
            <Container sx={{my: 8, minHeight: '100vh'}}>
                <ProfileInformationChange/>
                <Box sx={{mt: 4}}>
                    <ProfilePasswordChange/>
                </Box>
            </Container>
            <AppFooter mode={AppMode.Staff}/>
        </>
    );
}
