import React, {useState} from 'react';
import {Tab, Tabs} from '@mui/material';
import {ProfilePasswordChange} from './components/profile-password-change';
import {ProfileInformationChange} from './components/profile-information-change';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {UserEditPageMembershipsTab} from './components/profile-memberships-tab';

export function Profile(): JSX.Element {
    const [currentTab, setCurrentTab] = useState(0);

    return (
        <PageWrapper
            title="Profileinstellungen"
        >
            <Tabs
                value={currentTab}
                onChange={(_, newValue) => {
                    setCurrentTab(newValue);
                }}
                sx={{mb: 2}}
            >
                <Tab
                    label="Benutzerdaten"
                    value={0}
                />
                <Tab
                    label="Passwort"
                    value={1}
                />
                <Tab
                    label="Fachbereiche und Rollen"
                    value={2}
                />
            </Tabs>

            {
                currentTab === 0 &&
                <ProfileInformationChange/>
            }

            {
                currentTab === 1 &&
                <ProfilePasswordChange/>
            }

            {
                currentTab === 2 &&
                <UserEditPageMembershipsTab/>
            }
        </PageWrapper>
    );
}
