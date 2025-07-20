import React, { type JSX } from 'react';
import {Box, Typography} from '@mui/material';
import Balancer from 'react-wrap-balancer';
import {SxProps, Theme} from "@mui/material";

interface ElementEditorSectionHeaderProps {
    title: string;
    titleSuffix?: string;
    variant?: 'h4' | 'h5' | 'h6';
    disableMarginTop?: boolean;
    disableMarginBottom?: boolean;
    maxWidth?: string | number;
    children?: React.ReactNode;
    sx?: SxProps<Theme>;
}

export function ElementEditorSectionHeader({
                                     title,
                                     titleSuffix,
                                     variant = 'h4',
                                     disableMarginTop = false,
                                     disableMarginBottom = false,
                                     maxWidth = '900px',
                                     children,
                                     sx = {},
                                 }: ElementEditorSectionHeaderProps): JSX.Element {
    return (
        <Box
            sx={{
                mt: disableMarginTop ? 0 : 4,
                mb: disableMarginBottom ? 0 : 2,
                maxWidth,
                ...sx,
            }}
        >
            <Typography variant={variant}>
                <Balancer>{title} <Typography component="span" color={"text.secondary"} sx={{fontSize: 'inherit', pl: 0.5}}>{titleSuffix}</Typography></Balancer>
            </Typography>
            {children && (
                <Typography sx={{mt: 1}}>
                    <Balancer ratio={0.5} preferNative={false}>{children}</Balancer>
                </Typography>
            )}
        </Box>
    );
}
