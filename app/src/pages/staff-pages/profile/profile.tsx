import React, {useState} from 'react';
import {Box, Tab, Tabs} from '@mui/material';
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
            <Box
                sx={{
                    mt: -4,
                    mb: 4,
                    '&::after': {
                        position: 'absolute',
                        content: '""',
                        display: 'block',
                        width: '100%',
                        left: 0,
                        right: 0,
                        height: '1px',
                        backgroundColor: 'divider',
                    },
                }}
            >
                <Tabs
                    value={currentTab}
                    onChange={(_, newValue) => {
                        setCurrentTab(newValue);
                    }}
                    variant="scrollable"
                    scrollButtons="auto"
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
            </Box>

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
