import { Box, Tab, Tabs } from '@mui/material';
import React, { useState } from 'react';
import { useAuthGuard } from '../../../hooks/use-auth-guard';
import { MediaSettings } from './components/media-settings/media-settings';
import { SystemInformation } from './components/system-information/system-information';
import { SmtpTest } from './components/smtp-test/smtp-test';
import { ApplicationSettings } from './components/application-settings/application-settings';
import { useUserGuard } from '../../../hooks/use-user-guard';
import { PageWrapper } from '../../../components/page-wrapper/page-wrapper';

export function Settings(): JSX.Element {
    useAuthGuard();
    useUserGuard((user) => user?.admin ?? false);

    const [currentTab, setCurrentTab] = useState(0);

    return (
        <PageWrapper
            title="Systemeinstellungen"
        >
            <Box
                sx={ {
                    borderBottom: 1,
                    borderColor: 'divider',
                } }
            >
                <Tabs
                    value={ currentTab }
                    onChange={ (_, value) => {
                        setCurrentTab(value);
                    } }
                >
                    <Tab
                        label="Anwendungseinstellungen"
                        value={ 0 }
                    />
                    <Tab
                        label="Medien"
                        value={ 1 }
                    />
                    <Tab
                        label="E-Mail-Versand"
                        value={ 2 }
                    />
                    <Tab
                        label="Systeminformationen"
                        value={ 3 }
                    />
                </Tabs>
            </Box>

            <Box
                sx={ {
                    mt: 4,
                } }
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
            </Box>
        </PageWrapper>
    );
}
