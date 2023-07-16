import React from 'react';
import {Box, Button, Container, SxProps, Typography, useTheme} from '@mui/material';
import {AppFooterProps} from './app-footer-props';
import {AppMode} from '../../data/app-mode';
import {useAppDispatch} from "../../hooks/use-app-dispatch";
import {MetaDialog, showMetaDialog} from "../../slices/app-slice";
import {useAppSelector} from "../../hooks/use-app-selector";
import {selectSystemConfigValue} from "../../slices/system-config-slice";
import {SystemConfigKeys} from "../../data/system-config-keys";
import {AssetService} from "../../services/asset-service";
import HelpOutlineOutlinedIcon from "@mui/icons-material/HelpOutlineOutlined";

const buttonStyle: SxProps = {
    color: '#16191F',
    textTransform: 'none',
    ml: 1,
    fontSize: '1.3125rem',
};

export function AppFooter({mode}: AppFooterProps) {
    const dispatch = useAppDispatch();
    const theme = useTheme();
    const name = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const logo = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.logo));

    return (
        <Box sx={{boxShadow: 'inset 0px 10px 20px rgba(0, 0, 0, 0.06)'}}>
            <Container>
                <Box
                    sx={{
                        display: 'flex',
                        pt: 12,
                        pb: 15,
                        alignItems: 'flex-end',
                        justifyContent: 'space-between',
                        [theme.breakpoints.down('md')]: {
                            flexDirection: 'column',
                            alignItems: 'flex-start',
                        }
                    }}
                >
                    <img
                        src={AssetService.getLink(logo)}
                        alt={name}
                        width={200}
                        height={100}
                        style={{objectFit: 'contain'}}
                    />

                    <Box>
                        {
                            mode === AppMode.Customer &&
                            <Box sx={{mb: 1}}>
                                <Button
                                    startIcon={<HelpOutlineOutlinedIcon
                                        style={{marginTop: '-1px'}}
                                    />}
                                    sx={buttonStyle}
                                    size="large"
                                    onClick={() => dispatch(showMetaDialog(MetaDialog.Help))}
                                >
                                    Hilfe
                                </Button>
                                <Button
                                    sx={buttonStyle}
                                    size="large"
                                    onClick={() => dispatch(showMetaDialog(MetaDialog.Privacy))}
                                >
                                    Datenschutz
                                </Button>
                                <Button
                                    sx={buttonStyle}
                                    size="large"
                                    onClick={() => dispatch(showMetaDialog(MetaDialog.Imprint))}
                                >
                                    Impressum
                                </Button>
                            </Box>
                        }
                        <Box>
                            <Typography
                                variant={'h6'}
                                align="right"
                                sx={{opacity: 0.5, mr: 1}}
                            >
                                {name}
                            </Typography>
                        </Box>
                    </Box>
                </Box>
            </Container>
            <Box sx={{textAlign: 'center', backgroundColor: '#F2F2F2', p: 0.5}}>
                <Typography
                    variant="caption"
                    color="#444444"
                >
                    {
                        mode === AppMode.Staff ?
                            'Das Online-Antrags-Management wird umgesetzt mit Gover – dem Fundament für moderne digitale Verwaltungsleistungen von Aivot' :
                            (
                                mode === AppMode.Customer ?
                                    'Dieses Formular wurde umgesetzt mit Gover – dem Fundament für moderne digitale Verwaltungsleistungen von Aivot' :
                                    'Dieses Angebot wurde umgesetzt mit Gover – dem Fundament für moderne digitale Verwaltungsleistungen von Aivot'
                            )
                    }
                </Typography>
            </Box>
        </Box>
    );
}
