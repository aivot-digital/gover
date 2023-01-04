import React from 'react';
import {SystemAssetsService} from '../../../services/system-assets.service';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';

export function Logo({width, height}: { width?: number; height?: number }) {
    const name = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    return (
        <img
            src={SystemAssetsService.getLogoLink()}
            alt={name}
            width={width ?? 200}
            height={height ?? 100}
            style={{objectFit: 'contain'}}
        />
    )
}
