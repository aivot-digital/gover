import React from 'react';
import {Box, Button, Container, type SxProps, Typography, useTheme} from '@mui/material';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showDialog} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {Logo} from '../logo/logo';
import HelpOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Help';
import {HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import {PrivacyDialogId} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialogId} from '../../dialogs/imprint-dialog/imprint-dialog';
import OpenInNewOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import {FormLayoutElement} from '../../models/elements/form-layout-element';
import {ProcessNodeEntity} from '../../modules/process/entities/process-node-entity';
import {ProcessEntity} from '../../modules/process/entities/process-entity';
import {ProcessVersionEntity} from '../../modules/process/entities/process-version-entity';
import {ProsunaAttribution} from '../prosuna-attribution/prosuna-attribution';

const buttonStyle: SxProps = {
    color: 'text.primary',
    textTransform: 'none',
    ml: 1,
    fontSize: '1.225rem',
};

interface RootComponentFooterProps {
    form: FormLayoutElement;
    node: ProcessNodeEntity;
    process: ProcessEntity;
    version: ProcessVersionEntity;
    logoUrl: string | null;
    logoUrlDark: string | null;
}

export function RootComponentFooter(props: RootComponentFooterProps) {
    const {
        form,
        node,
        process,
        version,
        logoUrl,
        logoUrlDark,
    } = props;

    const dispatch = useAppDispatch();
    const theme = useTheme();
    const name = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const disableListingPageLink = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.disableListingPageLink));
    const customListingPageLink = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.customListingPageLink));
    const resolvedLogoUrl = theme.palette.mode === 'dark' ? logoUrlDark ?? logoUrl : logoUrl;

    return (
        <Box
            component="footer"
            role="contentinfo"
            sx={{
                boxShadow: 'inset 0px 10px 20px rgba(0, 0, 0, 0.06)',
                backgroundColor: 'background.paper',
            }}
        >
            <Container>
                <Box
                    sx={{
                        display: 'flex',
                        pt: 8,
                        pb: 10,
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        [theme.breakpoints.down('md')]: {
                            flexDirection: 'column',
                            alignItems: 'flex-start',
                            pt: 4,
                            pb: 7,
                        },
                    }}
                >
                    {
                        resolvedLogoUrl != null ?
                            <Logo
                                key={'logo-' + resolvedLogoUrl}
                                updated={version.updated}
                                src={logoUrl ?? undefined}
                                srcDark={logoUrlDark ?? undefined}
                                width={200}
                                height={100}
                            /> :
                            <Box/>
                    }

                    <Box
                        component="nav"
                        role="navigation"
                        aria-label="Rechtliche Informationen"
                        sx={{
                            [theme.breakpoints.down('md')]: {
                                ml: -2,
                            },
                        }}
                    >
                        <Box
                            sx={{
                                mb: 1,
                                display: 'flex',
                                justifyContent: 'flex-end',
                                flexWrap: 'wrap',
                                [theme.breakpoints.down('md')]: {
                                    mt: 2,
                                    justifyContent: 'flex-start',
                                },
                            }}
                        >
                            <Button
                                startIcon={
                                    <HelpOutlineOutlinedIcon/>
                                }
                                sx={buttonStyle}
                                size="medium"
                                onClick={() => dispatch(showDialog(HelpDialogId))}
                            >
                                Hilfe
                            </Button>

                            <Button
                                sx={buttonStyle}
                                size="medium"
                                onClick={() => dispatch(showDialog(PrivacyDialogId))}
                            >
                                Datenschutz
                            </Button>

                            <Button
                                sx={buttonStyle}
                                size="medium"
                                onClick={() => dispatch(showDialog(ImprintDialogId))}
                            >
                                Impressum
                            </Button>

                            {
                                disableListingPageLink !== 'true' &&
                                <Button
                                    sx={buttonStyle}
                                    size="medium"
                                    href={customListingPageLink && customListingPageLink.length > 0 ? customListingPageLink : '/'}
                                    target="_blank"
                                    endIcon={
                                        <OpenInNewOutlinedIcon/>
                                    }
                                    title="Link öffnet in einem neuen Tab"
                                >
                                    Weitere Formulare
                                </Button>
                            }
                        </Box>

                        <Box>
                            <Typography
                                component={'p'}
                                variant="h6"
                                sx={{
                                    color: 'text.secondary',
                                    mt: 2,
                                    ml: 2.5,
                                    textAlign: 'left',
                                    [theme.breakpoints.up('md')]: {
                                        textAlign: 'right',
                                        mt: 0,
                                        mr: 1,
                                        ml: 0,
                                    },
                                }}
                            >
                                {name} &bull; Alle Rechte vorbehalten.
                            </Typography>
                        </Box>
                    </Box>
                </Box>
            </Container>
            <ProsunaAttribution placement="form"/>
        </Box>
    );
}
