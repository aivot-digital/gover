import React, {useEffect, useMemo, useRef, useState} from 'react';
import {Box, useTheme} from '@mui/material';

type LogoStatus = 'loading' | 'failed' | 'present';

interface ImageResult {
    url: string;
    status: Exclude<LogoStatus, 'loading'>;
}

interface LogoProps {
    updated?: string | null | undefined;
    src?: string | null;
    srcDark?: string | null;
    width?: number;
    height?: number;
    onStatusChange?: (status: LogoStatus) => void;
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

    const imageRef = useRef<HTMLImageElement>(null);
    const [imageResult, setImageResult] = useState<ImageResult | null>(null);

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

    const imageStatus: LogoStatus = url == null ?
        'failed' :
        imageResult?.url === url ? imageResult.status : 'loading';

    useEffect(() => {
        onStatusChange?.(imageStatus);
    }, [imageStatus, onStatusChange]);

    useEffect(() => {
        const image = imageRef.current;
        if (url != null && image?.complete) {
            setImageResult({
                url,
                status: image.naturalWidth > 0 ? 'present' : 'failed',
            });
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
                key={url}
                ref={imageRef}
                src={url}
                alt={'Logo ' + AppConfig.providerName}
                style={{
                    width: 'auto',
                    maxWidth: width ?? 200,
                    maxHeight: height ?? 100,
                }}
                onLoad={() => {
                    setImageResult({url, status: 'present'});
                }}
                onError={() => {
                    setImageResult({url, status: 'failed'});
                }}
            />
        </Box>
    );
}
