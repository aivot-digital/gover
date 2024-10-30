import {Box, Tab, Tabs} from '@mui/material';
import React, {useState} from 'react';
import {MediaSettings} from './components/media-settings/media-settings';
import {SystemInformation} from './components/system-information/system-information';
import {SmtpTest} from './components/smtp-test/smtp-test';
import {ApplicationSettings} from './components/application-settings/application-settings';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {useAdminGuard} from '../../../hooks/use-admin-guard';
import {NutzerkontenSettings} from './components/nutzerkonten-settings/nutzerkonten-settings';

export function Settings(): JSX.Element {
    useAdminGuard();

    const [currentTab, setCurrentTab] = useState(0);

    return (
        <PageWrapper
            title="Systemeinstellungen"
        >
            <Box
                sx={{
                    mt: -4,
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
                    variant="scrollable"
                    scrollButtons="auto"
                    value={currentTab}
                    onChange={(_, value) => {
                        setCurrentTab(value);
                    }}
                >
                    <Tab
                        label="Anwendungseinstellungen"
                        value={0}
                    />
                    <Tab
                        label="Medien"
                        value={1}
                    />
                    <Tab
                        label="E-Mail-Versand"
                        value={2}
                    />
                    <Tab
                        label="Systeminformationen"
                        value={3}
                    />
                    <Tab
                        label="Zentrale Konten"
                        value={4}
                    />
                </Tabs>
            </Box>

            <Box
                sx={{
                    mt: 4,
                }}
            >
                {
                    currentTab === 0 &&
                    <ApplicationSettings/>
                }
                {
                    currentTab === 1 &&
                    <MediaSettings/>
                }
                {
                    currentTab === 2 &&
                    <SmtpTest/>
                }
                {
                    currentTab === 3 &&
                    <SystemInformation/>
                }
                {
                    currentTab === 4 &&
                    <NutzerkontenSettings/>
                }
            </Box>
        </PageWrapper>
    );
}
