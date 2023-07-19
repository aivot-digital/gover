import React, {useEffect, useState} from 'react';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {Box} from '@mui/material';
import {AssetService} from '../../../services/asset-service';

const logoPrefix = process.env.NODE_ENV === 'development' ? 'http://localhost:8080' : '';

interface LogoProps {
    width?: number;
    height?: number;
}

export function Logo(props: LogoProps): JSX.Element {
    const [imageFailed, setImageFailed] = useState(false);
    const name = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const logo = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.logo));

    useEffect(() => {
        setImageFailed(false);
    }, [logo]);

    if (imageFailed) {
        return (
            <Box
                sx={{
                    width: props.width ?? 200,
                    height: props.height ?? 100,
                }}
            />
        );
    }

    return (
        <img
            src={logoPrefix + AssetService.getLink(logo)}
            alt={name}
            width={props.width ?? 200}
            height={props.height ?? 100}
            style={{
                objectFit: 'contain',
            }}
            onError={() => {
                setImageFailed(true);
            }}
        />
    );
}
