import React, {useState} from 'react';
import {Tab, Tabs} from '@mui/material';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {UserEditPageMembershipsTab} from './components/profile-memberships-tab';
import {ProfileDataTab} from './components/profile-data-tab';

enum ProfileTab {
    DATA = 'data',
    MEMBERSHIPS = 'memberships',
}

export function Profile(): JSX.Element {
    const [currentTab, setCurrentTab] = useState<ProfileTab>(ProfileTab.DATA);

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
                    value={ProfileTab.DATA}
                />
                <Tab
                    label="Fachbereiche und Rollen"
                    value={ProfileTab.MEMBERSHIPS}
                />
            </Tabs>

            {
                currentTab === ProfileTab.DATA &&
                <ProfileDataTab />
            }

            {
                currentTab === ProfileTab.MEMBERSHIPS &&
                <UserEditPageMembershipsTab />
            }
        </PageWrapper>
    );
}
