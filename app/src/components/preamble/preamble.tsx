import {Box, Grid, useTheme} from '@mui/material';
import React from 'react';
import {MarkdownContent} from '../markdown-content/markdown-content';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';
import {AssetsApiService} from '../../modules/assets/assets-api-service';

interface PreambleProps {
    text: string;
    logoLink?: string;
    logoAlt?: string;
}

function resolveLogoLink(logoLink?: string): string | undefined {
    const trimmedLogoLink = logoLink?.trim();

    if (trimmedLogoLink == null || trimmedLogoLink.length === 0) {
        return undefined;
    }

    if (/^(https?:\/\/|data:|blob:|\/)/i.test(trimmedLogoLink)) {
        return trimmedLogoLink;
    }

    return AssetsApiService.useAssetLink(trimmedLogoLink);
}

export function Preamble(props: PreambleProps) {
    const theme = useTheme();
    const showLogo = isStringNotNullOrEmpty(props.logoLink) && isStringNotNullOrEmpty(props.logoAlt);
    const logoLink = resolveLogoLink(props.logoLink);

    return (
        <>
            {
                showLogo ?
                    <Grid
                        container
                        spacing={10}
                        justifyContent={'space-between'}
                    >
                        <Grid
                            size={{
                                xs: 12,
                                md: 8,
                                lg: 7
                            }}>
                            <Box
                                sx={{maxWidth: '660px'}}
                            >
                                <MarkdownContent
                                    markdown={props.text}
                                    className={"content-without-margin-on-childs"}
                                />
                            </Box>
                        </Grid>
                        <Grid
                            sx={{
                                pr: 6,
                                textAlign: 'center',
                                [theme.breakpoints.down('md')]: {
                                    pr: 0,
                                    textAlign: 'left',
                                    pt: '40px!important',
                                    mb: 1,
                                }
                            }}
                            size={{
                                xs: 12,
                                md: 4,
                                lg: 5
                            }}>
                            <img
                                src={logoLink}
                                alt={props.logoAlt}
                                style={{
                                    maxWidth: '100%',
                                    marginTop: '-10px',
                                }}
                            />
                        </Grid>
                    </Grid> :
                    <Grid
                        container
                        spacing={10}
                        justifyContent="space-between"
                    >
                        <Grid size={12}>
                            <Box
                                sx={{
                                    maxWidth: '660px',
                                }}
                            >
                                <MarkdownContent
                                    markdown={props.text}
                                    className={"content-without-margin-on-childs"}
                                />
                            </Box>
                        </Grid>
                    </Grid>
            }
        </>
    );
}
