import React, {useEffect, useMemo, useState} from 'react';
import {Box} from '@mui/material';
import {createApiPath} from '../../utils/url-path-utils';

interface LogoProps {
    updated?: string | null | undefined;
    src?: string;
    width?: number;
    height?: number;
    onStatusChange?: (status: 'loading' | 'failed' | 'present') => void;
}

export function Logo(props: LogoProps) {
    const {
        updated,
        src,
        width,
        height,
    } = props;

    const [imageStatus, setImageStatus] = useState<'loading' | 'failed' | 'present'>('loading');

    useEffect(() => {
        props.onStatusChange?.(imageStatus);
    }, [imageStatus]);

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
