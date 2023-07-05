import {Box, Button, Typography} from '@mui/material';
import React, {useReducer} from 'react';
import {useAppSelector} from "../../../../../hooks/use-app-selector";
import {fetchSystemConfig, selectSystemConfigValue} from "../../../../../slices/system-config-slice";
import {SystemConfigKeys} from "../../../../../data/system-config-keys";
import {AssetService} from "../../../../../services/asset-service";
import {SelectAssetDialog} from "../../../../../dialogs/select-asset-dialog/select-asset-dialog";
import {SystemConfigsService} from "../../../../../services/system-configs-service";
import {useAppDispatch} from "../../../../../hooks/use-app-dispatch";


export function MediaSettings() {
    const dispatch = useAppDispatch();

    const [showFaviconSelect, toggleFaviconSelect] = useReducer(p => !p, false);
    const [showLogoSelect, toggleLogoSelect] = useReducer(p => !p, false);

    const faviconConfigKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.favicon));
    const logoConfigKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.logo));

    const handleSetFavicon = (favicon: string) => {
        SystemConfigsService.update(SystemConfigKeys.system.favicon, {
            key: SystemConfigKeys.system.favicon,
            value: favicon,
            publicConfig: true,
            created: '',
            updated: '',
        });
        toggleFaviconSelect();
        dispatch(fetchSystemConfig());
    };

    const handleSetLogo = (logo: string) => {
        SystemConfigsService.update(SystemConfigKeys.system.logo, {
            key: SystemConfigKeys.system.logo,
            value: logo,
            publicConfig: true,
            created: '',
            updated: '',
        });
        toggleLogoSelect();
        dispatch(fetchSystemConfig());
    };

    return (
        <>
            <Box sx={{mt: 2}}>
                <Typography
                    variant="subtitle1"
                    sx={{mb: 2}}
                >
                    Favicon
                </Typography>

                {
                    faviconConfigKey != null &&
                    <Box sx={{mb: 2}}>
                        <img
                            src={AssetService.getLink(faviconConfigKey)}
                            alt="Favicon"
                        />
                    </Box>
                }


                <Button
                    sx={{mt: 2}}
                    onClick={toggleFaviconSelect}
                >
                    Auswählen
                </Button>
            </Box>

            <Box sx={{mt: 2}}>
                <Typography
                    variant="subtitle1"
                    sx={{mb: 2}}
                >
                    Logo
                </Typography>

                {
                    logoConfigKey != null &&
                    <Box sx={{mb: 2}}>
                        <img
                            src={AssetService.getLink(logoConfigKey)}
                            alt="Logo"
                        />
                    </Box>
                }

                <Button
                    sx={{mt: 2}}
                    onClick={toggleLogoSelect}
                >
                    Hochladen
                </Button>
            </Box>

            <SelectAssetDialog
                title="Favicon auswählen"
                show={showFaviconSelect}
                onSelect={handleSetFavicon}
                onCancel={toggleFaviconSelect}
            />

            <SelectAssetDialog
                title="Logo auswählen"
                show={showLogoSelect}
                onSelect={handleSetLogo}
                onCancel={toggleLogoSelect}
            />
        </>
    );
}
