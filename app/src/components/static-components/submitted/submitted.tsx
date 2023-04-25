import {
    Box,
    Button,
    Checkbox,
    Divider,
    FormControl,
    FormControlLabel,
    FormHelperText,
    Grid,
    TextField,
    Typography,
    useTheme
} from '@mui/material';
import Rating, {IconContainerProps} from '@mui/material/Rating';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import React, {useCallback, useEffect, useRef, useState} from 'react';
import {
    faEnvelope,
    faFaceFrown,
    faFaceFrownSlight,
    faFaceLaugh,
    faFaceMeh,
    faFaceSmile,
    faFilePdf
} from '@fortawesome/pro-light-svg-icons';
import {Preamble} from '../preamble/preamble';
import ReactCanvasConfetti from 'react-canvas-confetti';
import {useSelector} from 'react-redux';
import {ApplicationService} from '../../../services/application.service';
import {selectLoadedApplication} from '../../../slices/app-slice';
import {validateEmail} from "../../../utils/validate-email";
import {isStringNullOrEmpty} from "../../../utils/string-utils";

const animationStartDelay = 200;
const animationDuration = 2000;

// TODO: Localization

export function Submitted({pdfLink}: { pdfLink: string }) {
    const application = useSelector(selectLoadedApplication);
    const submitStep = application?.root.submitStep;
    const theme = useTheme();

    const customIcons: {
        [index: string]: {
            icon: React.ReactElement;
            label: string;
        };
    } = {
        1: {
            icon: <FontAwesomeIcon
                icon={faFaceFrown}
                fixedWidth
                style={{fontSize: '40px', margin: '0 5px'}}
            />,
            label: 'Sehr Unzufrieden',
        },
        2: {
            icon: <FontAwesomeIcon
                icon={faFaceFrownSlight}
                fixedWidth
                style={{fontSize: '40px', margin: '0 5px'}}
            />,
            label: 'Unzufrieden',
        },
        3: {
            icon: <FontAwesomeIcon
                icon={faFaceMeh}
                fixedWidth
                style={{fontSize: '40px', margin: '0 5px'}}
            />,
            label: 'Neutral',
        },
        4: {
            icon: <FontAwesomeIcon
                icon={faFaceSmile}
                fixedWidth
                style={{fontSize: '40px', margin: '0 5px'}}
            />,
            label: 'Zufrieden',
        },
        5: {
            icon: <FontAwesomeIcon
                icon={faFaceLaugh}
                fixedWidth
                style={{fontSize: '40px', margin: '0 5px'}}
            />,
            label: 'Sehr Zufrieden',
        },
    };

    function IconContainer(props: IconContainerProps) {
        const {value, ...other} = props;
        return <span {...other}>{customIcons[value].icon}</span>;
    }

    const canvasStyles = {
        position: 'fixed',
        pointerEvents: 'none',
        width: '100%',
        height: '100%',
        top: 0,
        left: 0
    };

    // @ts-ignore
    function getAnimationSettings(angle, originX) {
        return {
            particleCount: 3,
            angle,
            spread: 55,
            origin: {x: originX},
            colors: ['#003087', '#BC082F', '#137673', '#BA3B76', '#8B596C']
        };
    }

    const refAnimationInstance = useRef(null);
    const [intervalId, setIntervalId] = useState();

    const getInstance = useCallback((instance) => {
        refAnimationInstance.current = instance;
    }, []);

    const nextTickAnimation = useCallback(() => {
        if (refAnimationInstance.current) {
            // @ts-ignore
            refAnimationInstance.current(getAnimationSettings(60, 0));
            // @ts-ignore
            refAnimationInstance.current(getAnimationSettings(120, 1));
        }
    }, []);

    const startAnimation = useCallback(() => {
        if (!intervalId) {
            // @ts-ignore
            setIntervalId(setInterval(nextTickAnimation, 16));
        }
    }, [nextTickAnimation, intervalId]);

    const pauseAnimation = useCallback(() => {
        clearInterval(intervalId);
        // @ts-ignore
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

    const [email, setEmail] = useState('');
    const [privacy, setPrivacy] = useState(false);
    const [privacyError, setPrivacyError] = useState<string>();
    const [mailInvalid, setMailInvalid] = useState(false);
    const [mailSent, setMailSent] = useState(false);

    const sendApplicationCopyMail = () => {
        if (application != null) {
            if (privacy) {
                if (validateEmail(email)) {
                    ApplicationService.sendApplicationCopy(application, pdfLink, email);
                    setMailSent(true);
                    setPrivacyError(undefined);
                    setMailInvalid(false);
                } else {
                    setMailInvalid(true);
                }
            } else {
                setPrivacyError('Sie müssen Ihr Einverständnis zum Versandt der E-Mail geben.');
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
                !isStringNullOrEmpty(submitStep?.textPostSubmit) &&
                <Preamble
                    text={submitStep?.textPostSubmit ?? ''}
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
                        sx={{mt: 1, mb: 4}}
                        variant={'body2'}
                    >
                        Über die Schaltfläche “Antrag als PDF herunterladen” können Sie sich den von Ihnen
                        eingereichten
                        Antrag als PDF herunterladen.
                    </Typography>

                    <Button
                        variant="contained"
                        startIcon={<FontAwesomeIcon
                            icon={faFilePdf}
                            style={{marginTop: '-2px'}}
                        />}
                        component="a"
                        target="_blank"
                        href={pdfLink}
                        size={'large'}
                    >
                        Antrag als PDF herunterladen
                    </Button>
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
                        sx={{mt: 1, mb: 2.4}}
                        variant={'body2'}
                    >
                        Lassen Sie sich Ihren eingereichten Antrag durch das Ausfüllen des folgenden Formulars an
                        die
                        von Ihnen angegebene E-Mail-Adresse zusenden.
                    </Typography>
                    <TextField
                        label="E-Mail-Adresse *"
                        placeholder="max.mustermann@mail.de"
                        value={email}
                        disabled={mailSent}
                        onChange={event => setEmail(event.target.value)}
                        error={mailInvalid}
                        helperText={mailInvalid ? 'Bitte geben Sie eine gültige E-Mail-Adresse ein.' : undefined}
                    />
                    <FormControl error={privacyError != null}>
                        <FormControlLabel
                            sx={{mt: 2}}
                            control={<Checkbox
                                checked={privacy}
                                onChange={(_, checked) => setPrivacy(checked)}
                                color={privacyError != null ? 'primary' : 'error'}
                            />}
                            disabled={mailSent}
                            label="Ich erteile mein Einverständnis, dass der Antrag inklusive hochgeladener Unterlagen per unverschlüsselter E-Mail versandt wird."
                        />
                        {
                            privacyError != null &&
                            <FormHelperText>
                                {privacyError}
                            </FormHelperText>
                        }
                    </FormControl>
                    <Button
                        sx={{mt: 4}}
                        variant="contained"
                        startIcon={<FontAwesomeIcon
                            icon={faEnvelope}
                            style={{marginTop: '-2px'}}
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
                sx={{textAlign: 'center', mt: 1}}
                variant={'body2'}
            >
                Ihre Rückmeldung wird anonym an uns übertragen und hilft uns <br/>
                bei der Verbesserung unserer Anträge &amp; Prozesse. Vielen Dank!
            </Typography>
            <Box sx={{display: 'flex', justifyContent: 'center', mt: 4}}>
                <Rating
                    sx={{
                        color: theme.palette.primary.main
                    }}
                    name="highlight-selected-only"
                    IconContainerComponent={IconContainer}
                    highlightSelectedOnly
                    size={'large'}
                />
            </Box>

            <ReactCanvasConfetti
                refConfetti={getInstance}
                // @ts-ignore
                style={canvasStyles}
            />
        </>
    );
}
