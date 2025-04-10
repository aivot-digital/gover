import {Box, Button, Typography} from '@mui/material';
import React, {useReducer} from 'react';
import {useAppSelector} from '../../../../../hooks/use-app-selector';
import {selectSystemConfigValue, setSystemConfig} from '../../../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../../../data/system-config-keys';
import {SelectAssetDialog} from '../../../../../dialogs/select-asset-dialog/select-asset-dialog';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../../slices/snackbar-slice';
import {useApi} from '../../../../../hooks/use-api';
import ImageSearchOutlinedIcon from '@mui/icons-material/ImageSearchOutlined';
import {AssetsApiService} from '../../../../../modules/assets/assets-api-service';
import {SystemConfigsApiService} from '../../../../../modules/configs/system-configs-api-service';

export function MediaSettings() {
    const api = useApi();
    const dispatch = useAppDispatch();

    const [showFaviconSelect, toggleFaviconSelect] = useReducer((p) => !p, false);
    const [showLogoSelect, toggleLogoSelect] = useReducer((p) => !p, false);

    const faviconConfigKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.favicon));
    const logoConfigKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.logo));

    const handleSetFavicon = (favicon: string): void => {
        new SystemConfigsApiService(api)
            .update(SystemConfigKeys.system.favicon, {value: favicon})
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
        new SystemConfigsApiService(api)
            .update(SystemConfigKeys.system.logo, {value: logo})
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
                >
                    Favicon
                </Typography>

                <Typography
                    sx={{
                        maxWidth: 900,
                        mb: 1.6,
                    }}
                >
                    Ein Favicon ist ein kleines Symbol, das im Browser-Tab erscheint, um eine Website visuell zu identifizieren. Es sollte das Logo oder ein markantes Element der Marke zeigen.
                </Typography>

                {
                    faviconConfigKey != null &&
                    <Box
                        sx={{
                            mb: 1.6,
                        }}
                    >
                        <img
                            src={AssetsApiService.useAssetLink(faviconConfigKey)}
                            alt="Favicon"
                            style={{
                                width: 'auto',
                                maxWidth: '100px',
                                maxHeight: '100px'
                            }}
                        />
                    </Box>
                }

                <Button
                    sx={{
                        mt: 2,
                    }}
                    onClick={toggleFaviconSelect}
                    variant={'outlined'}
                    startIcon={<ImageSearchOutlinedIcon
                        sx={{
                            marginTop: '-2px',
                        }}
                    />}
                >
                    Datei auswählen
                </Button>
            </Box>

            <Box
                sx={{
                    mt: 5,
                }}
            >
                <Typography
                    variant="subtitle1"
                >
                    Logo oder Wappen
                </Typography>

                <Typography
                    sx={{
                        maxWidth: 900,
                        mb: 1.6,
                    }}
                >
                    Das Logo oder Wappen dient in Ihren Formularen als visuelles Erkennungsmerkmal.
                    Bitte achten Sie auf eine gute grafische Qualität und nutzen Sie am besten
                    eine <abbr
                        title="Eine digitale Grafikdatei, die geometrische Formeln verwendet, um Bilder darzustellen, die ohne Qualitätsverlust skaliert werden können"
                    >
                        Vektordatei
                    </abbr> (z.B. SVG) um eine optimale Darstellung zu gewährleisten.
                </Typography>

                {
                    logoConfigKey != null &&
                    <Box
                        sx={{
                            mb: 1.6,
                        }}
                    >
                        <img
                            src={AssetsApiService.useAssetLink(logoConfigKey)}
                            alt="Logo"
                            style={{
                                width: 'auto',
                                maxWidth: '200px',
                                maxHeight: '100px'
                            }}
                        />
                    </Box>
                }

                <Button
                    sx={{
                        mt: 2,
                    }}
                    variant={'outlined'}
                    onClick={toggleLogoSelect}
                    startIcon={<ImageSearchOutlinedIcon
                        sx={{
                            marginTop: '-2px',
                        }}
                    />}
                >
                    Datei auswählen
                </Button>
            </Box>

            <SelectAssetDialog
                title="Favicon auswählen"
                show={showFaviconSelect}
                onSelect={handleSetFavicon}
                onCancel={toggleFaviconSelect}
                mimetype="image"
                mode="public"
            />

            <SelectAssetDialog
                title="Logo auswählen"
                show={showLogoSelect}
                onSelect={handleSetLogo}
                onCancel={toggleLogoSelect}
                mimetype="image"
                mode="public"
            />
        </>
    );
}
