import {Alert, AlertTitle, Box, Button, Typography} from '@mui/material';
import React, {useState} from 'react';
import {ApiConfig} from '../../../../../api-config';
import {SystemAssetKeys} from '../../../../../data/system-asset-keys';
import axios from 'axios';
import {CrudService} from '../../../../../services/crud.service';
import {SystemAssetsService} from '../../../../../services/system-assets.service';
import strings from './media-settings-strings.json';
import {FileUpload} from '../../../../../components/file-upload/file-upload';
import {Localization} from '../../../../../locale/localization';

const __ = Localization(strings);

export function MediaSettings() {
    const [faviconToUpload, setFaviconToUpload] = useState<File[]>([]);
    const [faviconKey, setFaviconKey] = useState(SystemAssetKeys.provider.favicon);
    const [faviconUploadSuccessful, setFaviconUploadSuccessful] = useState(false);

    const [logoToUpload, setLogoToUpload] = useState<File[]>([]);
    const [logoKey, setLogoKey] = useState(SystemAssetKeys.provider.favicon);
    const [logoUploadSuccessful, setLogoUploadSuccessful] = useState(false);

    const upload = (assetKey: string) => {
        let files: File[] = [];
        switch (assetKey) {
            case SystemAssetKeys.provider.favicon:
                files = faviconToUpload;
                break;
            case SystemAssetKeys.provider.logo:
                files = logoToUpload;
                break;
        }

        if (files.length > 0) {
            const file = files[0];

            if (file != null) {
                const formData = new FormData();
                formData.set('file', file);
                axios.post(ApiConfig.address + '/system-assets/' + assetKey, formData, CrudService.getConfig())
                    .then(() => {
                        switch (assetKey) {
                            case SystemAssetKeys.provider.favicon:
                                setFaviconUploadSuccessful(true);
                                setFaviconToUpload([]);
                                setFaviconKey(Date.now().toString());
                                break;
                            case SystemAssetKeys.provider.logo:
                                setLogoUploadSuccessful(true);
                                setLogoToUpload([]);
                                setLogoKey(Date.now().toString());
                                break;
                        }
                    });
            }
        }
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

                <Box sx={{mb: 2}}>
                    <img
                        src={SystemAssetsService.getFaviconLink() + '?' + faviconKey}
                        alt="Favicon"
                    />
                </Box>

                <FileUpload
                    extensions={['ico']}
                    value={faviconToUpload}
                    onChange={setFaviconToUpload}
                />

                {
                    faviconUploadSuccessful &&
                    <Alert
                        sx={{mt: 2}}
                        severity="success"
                        onClose={() => setFaviconUploadSuccessful(false)}
                    >
                        <AlertTitle>{__.uploadSuccessTitle}</AlertTitle>
                        {__.uploadSuccessMessage}
                    </Alert>
                }

                <Button
                    sx={{mt: 2}}
                    onClick={() => upload(SystemAssetKeys.provider.favicon)}
                    disabled={faviconToUpload.length === 0}
                >
                    {__.upload}
                </Button>
            </Box>

            <Box sx={{mt: 2}}>
                <Typography
                    variant="subtitle1"
                    sx={{mb: 2}}
                >
                    Logo
                </Typography>

                <Box sx={{mb: 2}}>
                    <img
                        src={SystemAssetsService.getLogoLink() + '?' + logoKey}
                        alt="Logo"
                    />
                </Box>

                <FileUpload
                    extensions={['png']}
                    value={logoToUpload}
                    onChange={setLogoToUpload}
                />

                {
                    logoUploadSuccessful &&
                    <Alert
                        sx={{mt: 2}}
                        severity="success"
                        onClose={() => setLogoUploadSuccessful(false)}
                    >
                        <AlertTitle>{__.uploadSuccessTitle}</AlertTitle>
                        {__.uploadSuccessMessage}
                    </Alert>
                }

                <Button
                    sx={{mt: 2}}
                    onClick={() => upload(SystemAssetKeys.provider.logo)}
                    disabled={logoToUpload.length === 0}
                >
                    {__.upload}
                </Button>
            </Box>
        </>
    );
}
