import React, {useState} from 'react';
import {useAppSelector} from '../../hooks/use-app-selector';
import {Box, Skeleton} from '@mui/material';
import {selectSetup} from '../../slices/shell-slice';

interface LogoProps {
    width?: number;
    height?: number;
}

export function Logo(props: LogoProps) {
    const [imageStatus, setImageStatus] = useState<'loading' | 'failed' | 'present'>('loading');
    const setup = useAppSelector(selectSetup);

    if (imageStatus == 'failed') {
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
        <Box
            sx={{
                position: 'relative',
            }}
        >
            {
                imageStatus === 'loading' &&
                <Skeleton
                    sx={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        display: 'inline-block',
                        width: '100%',
                        maxWidth: props.width ?? 200,
                        height: props.height ?? 100,
                    }}
                />
            }

            <img
                src="/api/public/system/logo/"
                alt={'Logo ' + setup?.providerName}
                style={{
                    width: 'auto',
                    maxWidth: props.width ?? 200,
                    maxHeight: props.height ?? 100,
                }}
                onLoad={() => {
                    setImageStatus('present');
                }}
                onError={() => {
                    setImageStatus('failed');
                }}
            />
        </Box>
    );
}
