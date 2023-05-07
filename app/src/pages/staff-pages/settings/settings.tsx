import {Box, Container, Tab, Tabs} from '@mui/material';
import React, {useState} from 'react';
import {UserList} from './components/user-list/user-list';
import {SettingsProviderConfig} from './components/application-settings/settings-provider-config';
import {useAuthGuard} from '../../../hooks/use-auth-guard';
import {MediaSettings} from './components/media-settings/media-settings';
import strings from './settings-strings.json';
import {AppToolbar} from '../../../components/app-toolbar/app-toolbar';
import {AppFooter} from '../../../components/app-footer/app-footer';
import {AppMode} from '../../../data/app-mode';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {Localization} from '../../../locale/localization';
import {SystemInformation} from './components/system-information/system-information';
import {useRoleGuard} from '../../../hooks/use-role-guard';
import {UserRole} from '../../../data/user-role';
import {SmtpTest} from "./components/smtp-test/smtp-test";

const __ = Localization(strings);

export function Settings() {
    useAuthGuard();
    useRoleGuard(UserRole.Admin);

    const [currentTab, setCurrentTab] = useState(0);

    return (
        <>
            <MetaElement title={__.title}/>

            <AppToolbar
                title={__.title}
                parentPath={'/overview'}
            />
            <Container sx={{mt: 4, mb: 10, minHeight: '100vh'}}>
                <Box sx={{borderBottom: 1, borderColor: 'divider'}}>
                    <Tabs
                        value={currentTab}
                        onChange={(_, value) => {
                            setCurrentTab(value);
                        }}
                    >
                        <Tab
                            label={__.usersTabTitle}
                            value={0}
                        />
                        <Tab
                            label={__.appSettingsTabTitle}
                            value={1}
                        />
                        <Tab
                            label={__.mediaTabTitle}
                            value={2}
                        />
                        <Tab
                            label={__.smtpTestTitle}
                            value={3}
                        />
                        <Tab
                            label={__.systemInfoTabTitle}
                            value={4}
                        />
                    </Tabs>
                </Box>

                <Box sx={{mt: 4}}>
                    {
                        currentTab === 0 &&
                        <UserList/>
                    }
                    {
                        currentTab === 1 &&
                        <SettingsProviderConfig/>
                    }
                    {
                        currentTab === 2 &&
                        <MediaSettings/>
                    }
                    {
                        currentTab === 3 &&
                        <SmtpTest/>
                    }
                    {
                        currentTab === 4 &&
                        <SystemInformation/>
                    }
                </Box>
            </Container>
            <AppFooter mode={AppMode.Staff}/>
        </>
    );
}
