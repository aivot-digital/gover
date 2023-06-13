import React from 'react';
import {Box, Button, Container, SxProps, Typography, useTheme} from '@mui/material';
import {SystemAssetsService} from '../../services/system-assets.service';
import {AppFooterProps} from './app-footer-props';
import {AppMode} from '../../data/app-mode';
import strings from './app-footer-strings.json';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faQuestionCircle} from '@fortawesome/pro-light-svg-icons';
import {Localization} from '../../locale/localization';
import {useAppDispatch} from "../../hooks/use-app-dispatch";
import {MetaDialog, showMetaDialog} from "../../slices/app-slice";
import {useAppSelector} from "../../hooks/use-app-selector";
import {selectSystemConfigValue} from "../../slices/system-config-slice";
import {SystemConfigKeys} from "../../data/system-config-keys";

const __ = Localization(strings);

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

    return (
        <Box sx={{boxShadow: 'inset 0px 10px 10px rgba(0, 0, 0, 0.12)'}}>
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
                        src={SystemAssetsService.getLogoLink()}
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
                                    startIcon={<FontAwesomeIcon
                                        style={{marginTop: '-6px'}}
                                        icon={faQuestionCircle}
                                    />}
                                    sx={buttonStyle}
                                    size="large"
                                    onClick={() => dispatch(showMetaDialog(MetaDialog.Help))}
                                >
                                    {__.helpLabel}
                                </Button>
                                <Button
                                    sx={buttonStyle}
                                    size="large"
                                    onClick={() => dispatch(showMetaDialog(MetaDialog.Privacy))}
                                >
                                    {__.privacyLabel}
                                </Button>
                                <Button
                                    sx={buttonStyle}
                                    size="large"
                                    onClick={() => dispatch(showMetaDialog(MetaDialog.Imprint))}
                                >
                                    {__.imprintLabel}
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
                        mode === AppMode.Staff ? __.brandingStaff : (mode === AppMode.Customer ? __.brandingCustomer : __.brandingCustomerDisplay)
                    }
                </Typography>
            </Box>
        </Box>
    );
}
