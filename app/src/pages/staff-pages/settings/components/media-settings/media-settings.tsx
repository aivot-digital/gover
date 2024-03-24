import {Box, Button, Typography} from '@mui/material';
import React, {useReducer} from 'react';
import {useAppSelector} from '../../../../../hooks/use-app-selector';
import {selectSystemConfigValue, setSystemConfig} from '../../../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../../../data/system-config-keys';
import {SelectAssetDialog} from '../../../../../dialogs/select-asset-dialog/select-asset-dialog';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../../slices/snackbar-slice';
import {useApi} from '../../../../../hooks/use-api';
import {useSystemConfigsApi} from '../../../../../hooks/use-system-configs-api';
import {useAssetLink} from '../../../../../hooks/use-assets-api';

export function MediaSettings(): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();

    const [showFaviconSelect, toggleFaviconSelect] = useReducer((p) => !p, false);
    const [showLogoSelect, toggleLogoSelect] = useReducer((p) => !p, false);

    const faviconConfigKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.favicon));
    const logoConfigKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.logo));

    const handleSetFavicon = (favicon: string): void => {
        useSystemConfigsApi(api)
            .save(SystemConfigKeys.system.favicon, favicon)
            .then((config) => {
                dispatch(setSystemConfig(config));
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Favicon konnte nicht gespeichert werden'));
            });
        toggleFaviconSelect();
    };

    const handleSetLogo = (logo: string): void => {
        useSystemConfigsApi(api)
            .save(SystemConfigKeys.system.logo, logo)
            .then((config) => {
                dispatch(setSystemConfig(config));
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Logo konnte nicht gespeichert werden'));
            });
        toggleLogoSelect();
    };

    return (
        <>
            <Box
                sx={{
                    mt: 2,
                }}
            >
                <Typography
                    variant="subtitle1"
                    sx={{
                        mb: 2,
                    }}
                >
                    Favicon
                </Typography>

                {
                    faviconConfigKey != null &&
                    <Box
                        sx={{
                            mb: 2,
                        }}
                    >
                        <img
                            src={useAssetLink(faviconConfigKey)}
                            alt="Favicon"
                        />
                    </Box>
                }

                <Button
                    sx={{
                        mt: 2,
                    }}
                    onClick={toggleFaviconSelect}
                >
                    Auswählen
                </Button>
            </Box>

            <Box
                sx={{
                    mt: 8,
                }}
            >
                <Typography
                    variant="subtitle1"
                    sx={{
                        mb: 2,
                    }}
                >
                    Logo
                </Typography>

                {
                    logoConfigKey != null &&
                    <Box
                        sx={{
                            mb: 2,
                        }}
                    >
                        <img
                            src={useAssetLink(logoConfigKey)}
                            alt="Logo"
                        />
                    </Box>
                }

                <Button
                    sx={{
                        mt: 2,
                    }}
                    onClick={toggleLogoSelect}
                >
                    Auswählen
                </Button>
            </Box>

            <SelectAssetDialog
                title="Favicon auswählen"
                show={showFaviconSelect}
                onSelect={handleSetFavicon}
                onCancel={toggleFaviconSelect}
                mimetype="image/*"
            />

            <SelectAssetDialog
                title="Logo auswählen"
                show={showLogoSelect}
                onSelect={handleSetLogo}
                onCancel={toggleLogoSelect}
                mimetype="image/*"
            />
        </>
    );
}
