import React, {useEffect, useState} from 'react';
import {Box, Container, Divider, IconButton, Tooltip, Typography, useTheme} from '@mui/material';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faArrowRight, faArrowUpLeft} from '@fortawesome/pro-solid-svg-icons';
import {showNotImplementedMessage} from '../../utils/show-not-implemented-message';
import strings from './introductory-strings.json';
import {useAppSelector} from '../../hooks/use-app-selector';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {faXmarkCircle} from '@fortawesome/pro-light-svg-icons';
import {Localization} from '../../locale/localization';
import {AppMode} from "../../data/app-mode";

const __ = Localization(strings);

const dismissIntroStorageKey = '__dismiss_intro__'

interface IntroductoryProps {
    mode: AppMode;
}

export function Introductory({mode}: IntroductoryProps) {
    const theme = useTheme();
    const systemConfig = useAppSelector(state => state.systemConfig);

    const [show, setShow] = useState(false);

    useEffect(() => {
        setShow(localStorage.getItem(dismissIntroStorageKey) == null);
    }, []);

    const onDismiss = () => {
        setShow(false);
        localStorage.setItem(dismissIntroStorageKey, 'true');
    }

    if (!show) {
        return null;
    }

    return (
        <Container>
            <Box>
                <Box
                    sx={{
                        position: 'relative',
                        px: 11,
                        py: 9,
                        mt: 5,
                        mb: 5,
                        backgroundColor: theme.palette.primary.dark,
                    }}
                >
                    {
                        mode === AppMode.Staff &&
                        <Box sx={{position: 'absolute', right: theme.spacing(11)}}>
                            <Tooltip
                                title={__.dismissTooltip}
                            >
                                <IconButton
                                    sx={{color: theme.palette.secondary.main}}
                                    onClick={onDismiss}
                                >
                                    <FontAwesomeIcon icon={faXmarkCircle}/>
                                </IconButton>
                            </Tooltip>
                        </Box>
                    }

                    <Box>
                        <Typography
                            variant="h2"
                            fontSize={'1.75rem'}
                            color={'white'}
                        >
                            {__.caption}
                        </Typography>
                        <Typography
                            variant="h2"
                            fontSize={'2.5rem'}
                            fontWeight={800}
                            lineHeight={'2.625rem'}
                            color={'white'}
                            sx={{mt: 2}}
                        >
                            <span style={{color: theme.palette.secondary.main}}>{__.title}</span><br/>
                            {systemConfig[SystemConfigKeys.provider.name]}
                        </Typography>

                        {
                            mode === AppMode.Staff &&
                            <>

                                <Divider
                                    sx={{
                                        mt: 4,
                                        mb: 4,
                                        borderColor: 'var(--hw-secondary)',
                                    }}
                                />
                                <Typography
                                    variant={'h4'}
                                    fontSize={'1.125rem'}
                                    lineHeight={'1.25rem'}
                                    color={'white'}
                                    fontWeight={'normal'}
                                    onClick={showNotImplementedMessage}
                                    sx={[
                                        {
                                            maxWidth: '480px',
                                            display: 'flex',
                                            justifyContent: 'flex-general-information',
                                            mt: 2,
                                            mb: 2,
                                            transition: '200ms all ease-in-out',
                                            cursor: 'pointer',
                                        },
                                        {
                                            '&:hover': {
                                                color: 'var(--hw-secondary)',
                                            },
                                        }]}
                                >
                                    <FontAwesomeIcon
                                        icon={faArrowRight}
                                        fixedWidth
                                        size={'xs'}
                                        style={{marginRight: '6px', flexShrink: 0}}
                                    />
                                    <span>{__.gettingStartedLink}</span>
                                </Typography>

                                <Typography
                                    variant={'h4'}
                                    fontSize={'1.125rem'}
                                    lineHeight={'1.25rem'}
                                    color={'white'}
                                    fontWeight={'normal'}
                                    onClick={showNotImplementedMessage}
                                    sx={[
                                        {
                                            maxWidth: '480px',
                                            display: 'flex',
                                            justifyContent: 'flex-general-information',
                                            transition: '200ms all ease-in-out',
                                            cursor: 'pointer',
                                        },
                                        {
                                            '&:hover': {
                                                color: 'var(--hw-secondary)',
                                            },
                                        }]}
                                >
                                    <FontAwesomeIcon
                                        icon={faArrowUpLeft}
                                        fixedWidth
                                        size={'xs'}
                                        style={{marginRight: '6px', flexShrink: 0}}
                                    />
                                    <span>{__.newProcessLink}</span>
                                </Typography>
                            </>
                        }
                    </Box>
                </Box>
            </Box>
        </Container>
    );
}
