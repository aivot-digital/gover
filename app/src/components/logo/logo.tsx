import React, {useEffect, useMemo, useState} from 'react';
import {Box, useTheme} from '@mui/material';

interface LogoProps {
    updated?: string | null | undefined;
    src?: string | null;
    srcDark?: string | null;
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

    const url = useMemo(() => {
        const useSystemTheme = src === undefined && srcDark === undefined;
        const lightSrc = useSystemTheme ? AppConfig.logoUrl : src ?? null;
        const darkSrc = useSystemTheme ? AppConfig.logoUrlDark : srcDark ?? lightSrc;
        const resolvedSrc = theme.palette.mode === 'dark' ? darkSrc ?? lightSrc : lightSrc;

        if (resolvedSrc == null || updated == null) {
            return resolvedSrc;
        }

        const t = new Date(updated).getTime();

        if (resolvedSrc.includes('?')) {
            return `${resolvedSrc}&t=${t}`;
        }
        return `${resolvedSrc}?t=${t}`;
    }, [src, srcDark, theme.palette.mode, updated]);

    const resolvedImageStatus = url == null ? 'failed' : imageStatus;

    useEffect(() => {
        onStatusChange?.(resolvedImageStatus);
    }, [onStatusChange, resolvedImageStatus]);

    useEffect(() => {
        if (url != null) {
            setImageStatus('loading');
        }
    }, [url]);

    if (url == null) {
        return null;
    }

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
