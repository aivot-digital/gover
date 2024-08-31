import React, {useEffect, useState} from 'react';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {Box, Skeleton} from '@mui/material';
import {useAssetLink} from "../../hooks/use-assets-api";

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

    if (logo == null) {
        return (
            <Skeleton
                sx={{
                    display: 'inline-block',
                    width: '100%',
                    maxWidth: props.width ?? 200,
                    height: props.height ?? 100,
                }}
            />
        );
    }

    if (imageFailed) {
        return (
            <Box
                sx={{
                    display: 'inline-block',
                    width: '100%',
                    maxWidth: props.width ?? 200,
                    maxHeight: props.height ?? 100,
                }}
            />
        );
    }

    return (
        <img
            src={useAssetLink(logo)}
            alt={"Logo " + name}
            style={{
                width: 'auto',
                maxWidth: props.width ?? 200,
                maxHeight: props.height ?? 100,
            }}
            onError={() => {
                setImageFailed(true);
            }}
        />
    );
}
