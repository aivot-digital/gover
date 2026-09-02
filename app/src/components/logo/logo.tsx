import React, {useEffect, useMemo, useState} from 'react';
import {Box, useTheme} from '@mui/material';
import {createApiPath} from '../../utils/url-path-utils';

interface LogoProps {
    updated?: string | null | undefined;
    src?: string;
    srcDark?: string;
    width?: number;
    height?: number;
    onStatusChange?: (status: 'loading' | 'failed' | 'present') => void;
}

export function Logo(props: LogoProps) {
    const {
        updated,
        src,
        srcDark,
        width,
        height,
        onStatusChange,
    } = props;
    const theme = useTheme();

    const [imageStatus, setImageStatus] = useState<'loading' | 'failed' | 'present'>('loading');

    useEffect(() => {
        onStatusChange?.(imageStatus);
    }, [imageStatus, onStatusChange]);

    const url = useMemo(() => {
        const resolvedSrc = theme.palette.mode === 'dark' ? srcDark ?? src : src;
        let url = resolvedSrc ?? createApiPath(
            `/api/public/system/logo/${theme.palette.mode === 'dark' ? '?color-scheme=dark' : ''}`,
        );

        if (updated == null) {
            return url;
        }

        const t = new Date(updated).getTime();

        if (url.includes('?')) {
            return `${url}&t=${t}`;
        }
        return `${url}?t=${t}`;
    }, [src, srcDark, theme.palette.mode, updated]);

    useEffect(() => {
        setImageStatus('loading');
    }, [url]);

    if (imageStatus === 'failed') {
        // empty Box is required so that the space is reserved in the footer
        return (
            <Box/>
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
                <Box
                    sx={{
                        display: 'inline-block',
                        width: '100%',
                        maxWidth: width ?? 200,
                        maxHeight: height ?? 100,
                    }}
                />
            }

            <img
                src={url}
                alt={'Logo ' + AppConfig.providerName}
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
