import React, {useMemo, useState} from 'react';
import {useAppSelector} from '../../hooks/use-app-selector';
import {Box, Skeleton} from '@mui/material';
import {selectSetup} from '../../slices/shell-slice';
import {createApiPath} from '../../utils/url-path-utils';

interface LogoProps {
    updated?: string | null | undefined;
    src?: string;
    width?: number;
    height?: number;
}

export function Logo(props: LogoProps) {
    const {
        updated,
        src,
        width,
        height,
    } = props;

    const [imageStatus, setImageStatus] = useState<'loading' | 'failed' | 'present'>('loading');
    const setup = useAppSelector(selectSetup);

    const url = useMemo(() => {
        let url = src ?? createApiPath('/api/public/system/logo/');

        if (updated == null) {
            return url;
        }

        const t = new Date(updated).getTime();

        if (url.includes('?')) {
            return `${url}&t=${t}`;
        }
        return `${url}?t=${t}`;
    }, [src, updated]);

    if (imageStatus == 'failed') {
        return (
            <Box
                sx={{
                    display: 'inline-block',
                    width: '100%',
                    maxWidth: width ?? 200,
                    maxHeight: height ?? 100,
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
                        maxWidth: width ?? 200,
                        height: height ?? 100,
                    }}
                />
            }

            <img
                src={url}
                alt={'Logo ' + setup?.providerName}
                style={{
                    width: 'auto',
                    maxWidth: width ?? 200,
                    maxHeight: height ?? 100,
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
