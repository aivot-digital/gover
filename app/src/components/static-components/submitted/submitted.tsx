import {Box, Button, Divider, Grid, Typography, useTheme} from '@mui/material';
import Rating, {type IconContainerProps} from '@mui/material/Rating';
import React, {useCallback, useEffect, useRef, useState} from 'react';
import {Preamble} from '../preamble/preamble';
import ReactCanvasConfetti from 'react-canvas-confetti';
import {useSelector} from 'react-redux';
import {ApplicationService} from '../../../services/application-service';
import {selectLoadedApplication} from '../../../slices/app-slice';
import {validateEmail} from '../../../utils/validate-email';
import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {InfoDialog} from '../../../dialogs/info-dialog/info-dialog';
import {type SubmissionListDto} from '../../../models/entities/submission-list-dto';
import {TextFieldComponent} from '../../text-field/text-field-component';
import {CheckboxFieldComponent} from '../../checkbox-field/checkbox-field-component';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import PictureAsPdfOutlinedIcon from '@mui/icons-material/PictureAsPdfOutlined';
import MoodBadOutlinedIcon from '@mui/icons-material/MoodBadOutlined';
import SentimentDissatisfiedOutlinedIcon from '@mui/icons-material/SentimentDissatisfiedOutlined';
import SentimentNeutralOutlinedIcon from '@mui/icons-material/SentimentNeutralOutlined';
import SentimentSatisfiedAltOutlinedIcon from '@mui/icons-material/SentimentSatisfiedAltOutlined';
import EmojiEmotionsOutlinedIcon from '@mui/icons-material/EmojiEmotionsOutlined';

const animationStartDelay = 200;
const animationDuration = 2000;

interface SubmittedProps {
    submission?: SubmissionListDto;
}

export function Submitted(props: SubmittedProps): JSX.Element {
    const application = useSelector(selectLoadedApplication);
    const submitStep = application?.root.submitStep;
    const theme = useTheme();

    const customIcons: Record<string, {
        icon: React.ReactElement;
        label: string;
    }> = {
        1: {
            icon: <MoodBadOutlinedIcon
                sx={{
                    fontSize: '40px',
                    margin: '0 5px',
                }}
            />,
            label: 'Sehr Unzufrieden',
        },
        2: {
            icon: <SentimentDissatisfiedOutlinedIcon
                sx={{
                    fontSize: '40px',
                    margin: '0 5px',
                }}
            />,
            label: 'Unzufrieden',
        },
        3: {
            icon: <SentimentNeutralOutlinedIcon
                sx={{
                    fontSize: '40px',
                    margin: '0 5px',
                }}
            />,
            label: 'Neutral',
        },
        4: {
            icon: <SentimentSatisfiedAltOutlinedIcon
                sx={{
                    fontSize: '40px',
                    margin: '0 5px',
                }}
            />,
            label: 'Zufrieden',
        },
        5: {
            icon: <EmojiEmotionsOutlinedIcon
                sx={{
                    fontSize: '40px',
                    margin: '0 5px',
                }}
            />,
            label: 'Sehr Zufrieden',
        },
    };

    function IconContainer(props: IconContainerProps): JSX.Element {
        const {
            value,
            ...other
        } = props;
        return <span {...other}>{customIcons[value].icon}</span>;
    }

    const canvasStyles = {
        position: 'fixed',
        pointerEvents: 'none',
        width: '100%',
        height: '100%',
        top: 0,
        left: 0,
    };

    // @ts-expect-error
    function getAnimationSettings(angle, originX) {
        return {
            particleCount: 3,
            angle,
            spread: 55,
            origin: {x: originX},
            colors: ['#003087', '#BC082F', '#137673', '#BA3B76', '#8B596C'],
        };
    }

    const refAnimationInstance = useRef(null);
    const [intervalId, setIntervalId] = useState();

    const getInstance = useCallback((instance) => {
        refAnimationInstance.current = instance;
    }, []);

    const nextTickAnimation = useCallback(() => {
        if (refAnimationInstance.current) {
            // @ts-expect-error
            refAnimationInstance.current(getAnimationSettings(60, 0));
            // @ts-expect-error
            refAnimationInstance.current(getAnimationSettings(120, 1));
        }
    }, []);

    const startAnimation = useCallback(() => {
        if (!intervalId) {
            // @ts-expect-error
            setIntervalId(setInterval(nextTickAnimation, 16));
        }
    }, [nextTickAnimation, intervalId]);

    const pauseAnimation = useCallback(() => {
        clearInterval(intervalId);
        // @ts-expect-error
        setIntervalId(null);
    }, [intervalId]);

    /* TODO: This function will be used some time in the future. Do not remove or ask Daniel first.
    const stopAnimation = useCallback(() => {
        clearInterval(intervalId);
        // @ts-ignore
        setIntervalId(null);
        // @ts-ignore
        refAnimationInstance.current && refAnimationInstance.current.reset();
    }, [intervalId]);
     */

    const dispatch = useAppDispatch();

    const [email, setEmail] = useState('');
    const [privacy, setPrivacy] = useState(false);
    const [privacyError, setPrivacyError] = useState<string>();
    const [mailError, setMailError] = useState<string>();
    const [mailSent, setMailSent] = useState(false);
    const [showMailSentDialog, setShowMailSentDialog] = useState(false);

    const sendApplicationCopyMail = (): void => {
        if (props.submission != null) {
            if (privacy) {
                if (validateEmail(email)) {
                    setMailSent(true);
                    setPrivacyError(undefined);
                    setMailError(undefined);

                    ApplicationService
                        .sendApplicationCopy(props.submission, email)
                        .then(() => {
                            setShowMailSentDialog(true);
                        })
                        .catch((err) => {
                            if (err.status === 400) {
                                setMailError('Es konnte keine E-Mail an diese Adresse verschickt werden.');
                                setMailSent(false);
                            } else if (err.status === 409) {
                                setMailError('Sie hatten bereits zu viele Fehlversuche.');
                            } else if (err.status === 406) {
                                setMailError('Sie haben bereits eine E-Mail für diesen Antrag verschickt.');
                            } else {
                                console.error(err);
                                dispatch(showErrorSnackbar('Es ist ein Fehler aufgetreten. Bitte versuchen Sie es später erneut.'));
                            }
                        });
                } else {
                    setMailError('Bitte geben Sie eine gültige E-Mail-Adresse ein.');
                }
            } else {
                setPrivacyError('Sie müssen Ihr Einverständnis zum Versand der E-Mail geben.');
            }
        }
    };

    useEffect(() => {
        return () => {
            clearInterval(intervalId);
        };
    }, [intervalId]);

    useEffect(() => {
        setTimeout(() => {
            startAnimation();
        }, animationStartDelay);
    }, []);

    useEffect(() => {
        setTimeout(() => {
            pauseAnimation();
        }, animationDuration);
    }, []);

    return (
        <>
            {
                submitStep?.textPostSubmit != null &&
                !isStringNullOrEmpty(submitStep?.textPostSubmit) &&
                <Preamble
                    text={submitStep?.textPostSubmit}
                    logoLink={application?.root.introductionStep.initiativeLogoLink}
                    logoAlt={application?.root.introductionStep.initiativeName}
                />
            }

            <Divider sx={{my: 8}}/>

            <Grid
                container
                spacing={6}
            >
                <Grid
                    item
                    md={6}
                >
                    <Typography
                        variant="h6"
                        color="primary"
                    >
                        Antrag als PDF herunterladen
                    </Typography>
                    <Typography
                        sx={{
                            mt: 1,
                            mb: 4,
                        }}
                        variant={'body2'}
                    >
                        Über die Schaltfläche “Antrag als PDF herunterladen” können Sie sich den von Ihnen
                        eingereichten
                        Antrag als PDF herunterladen.
                    </Typography>

                    {
                        props.submission != null &&
                        <Button
                            variant="contained"
                            startIcon={<PictureAsPdfOutlinedIcon
                                sx={{marginTop: '-2px'}}
                            />}
                            component="a"
                            target="_blank"
                            href={`/api/public/prints/${props.submission.id}`}
                            size={'large'}
                        >
                            Antrag als PDF herunterladen
                        </Button>
                    }
                </Grid>
                <Grid
                    item
                    md={6}
                >
                    <Typography
                        variant="h6"
                        color="primary"
                    >
                        Antrag per E-Mail erhalten
                    </Typography>

                    <Typography
                        sx={{
                            mt: 1,
                            mb: 2.4,
                        }}
                        variant={'body2'}
                    >
                        Lassen Sie sich Ihren eingereichten Antrag durch das Ausfüllen des folgenden Formulars an
                        die
                        von Ihnen angegebene E-Mail-Adresse zusenden.
                    </Typography>

                    <TextFieldComponent
                        label="E-Mail-Adresse"
                        placeholder="max.mustermann@mail.de"
                        value={email}
                        disabled={mailSent}
                        onChange={(val) => {
                            setEmail(val ?? '');
                        }}
                        required
                        error={mailError}
                    />

                    <CheckboxFieldComponent
                        label="Ich erteile mein Einverständnis, dass der Antrag per unverschlüsselter E-Mail versandt wird."
                        value={privacy}
                        onChange={setPrivacy}
                        error={privacyError}
                        disabled={mailSent}
                    />

                    <Button
                        sx={{mt: 4}}
                        variant="contained"
                        startIcon={<EmailOutlinedIcon
                            sx={{marginTop: '-2px'}}
                        />}
                        onClick={sendApplicationCopyMail}
                        size={'large'}
                        disabled={mailSent}
                    >
                        Antrag per E-Mail erhalten
                    </Button>
                </Grid>
            </Grid>

            <Divider sx={{my: 8}}/>

            <Typography
                variant="h6"
                sx={{textAlign: 'center'}}
                color="primary"
            >
                Wie hat Ihnen dieser Prozess gefallen?
            </Typography>
            <Typography
                sx={{
                    textAlign: 'center',
                    mt: 1,
                }}
                variant={'body2'}
            >
                Ihre Rückmeldung wird anonym an uns übertragen und hilft uns <br/>
                bei der Verbesserung unserer Anträge &amp; Prozesse. Vielen Dank!
            </Typography>
            <Box
                sx={{
                    display: 'flex',
                    justifyContent: 'center',
                    mt: 4,
                }}
            >
                <Rating
                    sx={{
                        color: theme.palette.primary.main,
                    }}
                    name="highlight-selected-only"
                    IconContainerComponent={IconContainer}
                    highlightSelectedOnly
                    size="large"
                />
            </Box>

            <ReactCanvasConfetti
                refConfetti={getInstance}
                // @ts-expect-error
                style={canvasStyles}
            />

            <InfoDialog
                title="E-Mail versendet"
                severity="success"
                open={showMailSentDialog}
                onClose={() => {
                    setShowMailSentDialog(false);
                }}
            >
                Eine E-Mail mit dem eingereichten Antrag wurde an die angegebene E-Mail-Adresse versendet.
            </InfoDialog>
        </>
    );
}
