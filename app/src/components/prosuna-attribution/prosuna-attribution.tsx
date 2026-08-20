import React from 'react';
import {Box, Divider, Link, Typography, useTheme} from '@mui/material';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import {ProsunaLogo} from '../prosuna-logo/prosuna-logo';

export interface ProsunaAttributionProps {
    placement: 'form' | 'listing';
}

function createProsunaUrl(placement: ProsunaAttributionProps['placement']): string {
    const url = new URL('https://prosuna.de/');
    url.search = new URLSearchParams({
        utm_source: 'prosuna_instance',
        utm_medium: 'referral',
        utm_campaign: 'footer_attribution',
        utm_content: placement === 'form' ? 'form_footer' : 'listing_footer',
    }).toString();
    return url.toString();
}

export function ProsunaAttribution({placement}: ProsunaAttributionProps): React.ReactElement {
    const theme = useTheme();

    return (
        <Box
            sx={{
                backgroundColor: 'action.hover',
                px: 2,
                py: 1,
            }}
        >
            <Link
                href={createProsunaUrl(placement)}
                target="_blank"
                rel="noopener noreferrer"
                underline="none"
                title="Prosuna-Webseite öffnet in einem neuen Tab"
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 1.25,
                    width: 'fit-content',
                    maxWidth: 960,
                    minHeight: 32,
                    mx: 'auto',
                    color: 'text.secondary',
                    transition: theme.transitions.create('color'),
                    '&:hover': {
                        color: 'text.primary',
                    },
                    '&:focus-visible': {
                        outline: '2px solid',
                        outlineColor: 'primary.main',
                        outlineOffset: 2,
                        borderRadius: 1,
                    },
                }}
            >
                <ProsunaLogo
                    colorVariant="monochrome"
                    style={{
                        width: 86,
                        height: 'auto',
                        flex: '0 0 auto',
                        color: 'inherit',
                        transform: 'translateY(-1px)',
                    }}
                />
                <Divider
                    orientation="vertical"
                    flexItem
                    sx={{
                        my: 0.25,
                        borderColor: 'divider',
                    }}
                />
                <Typography
                    component="span"
                    variant="caption"
                    sx={{
                        color: 'inherit',
                        lineHeight: 1.45,
                    }}
                >
                    Realisiert mit Prosuna – der quelloffenen Plattform für Ende-zu-Ende digitalisierte
                    Verwaltungsprozesse.
                    <OpenInNew
                        aria-hidden="true"
                        sx={{
                            ml: 0.5,
                            fontSize: '0.9rem',
                            verticalAlign: '-0.2em',
                            color: 'text.disabled',
                        }}
                    />
                </Typography>
            </Link>
        </Box>
    );
}
