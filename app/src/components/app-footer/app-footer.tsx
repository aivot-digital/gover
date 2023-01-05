import React, {useState} from 'react';
import {Box, Button, Container, SxProps, Typography} from '@mui/material';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {SystemAssetsService} from '../../services/system-assets.service';
import {AppFooterProps} from './app-footer-props';
import {AppMode} from '../../data/app-mode';
import strings from './app-footer-strings.json';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faQuestionCircle} from '@fortawesome/pro-light-svg-icons';
import {PrivacyDialog} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog} from '../../dialogs/imprint-dialog/imprint-dialog';
import {Localization} from '../../locale/localization';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {HelpDialog} from '../../dialogs/help-dialog/help.dialog';

const __ = Localization(strings);

const buttonStyle: SxProps = {
    color: '#16191F',
    textTransform: 'none',
    ml: 1,
    fontSize: '1.3125rem',
};

export function AppFooter({mode}: AppFooterProps) {
    const name = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    const [showHelp, setShowHelp] = useState(false);
    const [showPrivacy, setShowPrivacy] = useState(false);
    const [showImprint, setShowImprint] = useState(false);

    return (
        <>
            <Box sx={{boxShadow: 'inset 0px 10px 10px rgba(0, 0, 0, 0.12)'}}>
                <Container>
                    <Box sx={{display: 'flex', pt: 12, pb: 15, alignItems: 'flex-end'}}>
                        <img
                            src={SystemAssetsService.getLogoLink()}
                            alt={name}
                            width={200}
                            height={100}
                            style={{objectFit: 'contain'}}
                        />

                        <Box sx={{ml: 'auto'}}>
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
                                        onClick={() => setShowHelp(true)}
                                    >
                                        {__.helpLabel}
                                    </Button>
                                    <Button
                                        sx={buttonStyle}
                                        size="large"
                                        onClick={() => setShowPrivacy(true)}
                                    >
                                        {__.privacyLabel}
                                    </Button>
                                    <Button
                                        sx={buttonStyle}
                                        size="large"
                                        onClick={() => setShowImprint(true)}
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
                            mode === AppMode.Staff ? __.brandingStaff : __.brandingCustomer
                        }
                    </Typography>
                </Box>
            </Box>
            <HelpDialog
                onHide={() => setShowHelp(false)}
                open={showHelp}
            />
            <PrivacyDialog
                onHide={() => setShowPrivacy(false)}
                open={showPrivacy}
            />
            <ImprintDialog
                onHide={() => setShowImprint(false)}
                open={showImprint}
            />
        </>
    );
}
