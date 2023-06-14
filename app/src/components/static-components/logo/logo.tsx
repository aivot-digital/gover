import React, {useState} from 'react';
import {SystemAssetsService} from '../../../services/system-assets.service';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {Box} from "@mui/material";

export function Logo({width, height}: { width?: number; height?: number }) {
    const [imageFailed, setImageFailed] = useState(false);
    const name = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    if (imageFailed) {
        return (
            <Box
                sx={{
                    width: width ?? 200,
                    height: height ?? 100,
                }}
            />
        );
    }

    return (
        <img
            src={SystemAssetsService.getLogoLink()}
            alt={name}
            width={width ?? 200}
            height={height ?? 100}
            style={{objectFit: 'contain'}}
            onError={() => setImageFailed(true)}
        />
    );
}
