import {Box, Button, Divider, Grid, Link, Typography} from '@mui/material';
import React, {useCallback, useEffect, useRef, useState} from 'react';
import {Preamble} from '../preamble/preamble';
import ReactCanvasConfetti from 'react-canvas-confetti';
import {useSelector} from 'react-redux';
import {selectLoadedForm, showDialog} from '../../slices/app-slice';
import {validateEmail} from '../../utils/validate-email';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {InfoDialog} from '../../dialogs/info-dialog/info-dialog';
import {TextFieldComponent} from '../text-field/text-field-component';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import PictureAsPdfOutlinedIcon from '@mui/icons-material/PictureAsPdfOutlined';
import PaymentOutlinedIcon from '@mui/icons-material/PaymentOutlined';
import {Rating} from '../rating/rating';
import {useApi} from '../../hooks/use-api';
import {useFormsApi} from '../../hooks/use-forms-api';
import {SubmissionListProjection} from '../../models/entities/submission';
import {AlertComponent} from '../alert/alert-component';
import qrcode from 'qrcode';
import {XBezahldienstePaymentStatus} from '../../data/xbezahldienste-payment-status';
import {PaymentProvider, PaymentProviderLabels} from '../../data/payment-provider';
import {Form} from '../../models/entities/form';
import {HelpDialogId} from '../../dialogs/help-dialog/help.dialog';

const animationStartDelay = 200;
const animationDuration = 2000;

interface SubmittedProps {
    submission: SubmissionListProjection;
    form: Form;
}

const useSetMailErrorWithSnackbar = (setMailError: (message: string) => void) => {
    const dispatch = useAppDispatch();

    return (message: string) => {
        setMailError(message);
        dispatch(showErrorSnackbar(message));
    };
};

const useSetPrivacyErrorWithSnackbar = (setPrivacyError: (message: string) => void) => {
    const dispatch = useAppDispatch();

    return (message: string) => {
        setPrivacyError(message);
        dispatch(showErrorSnackbar(message));
    };
};

export function Submitted(props: SubmittedProps): JSX.Element {
    const api = useApi();
    const application = useSelector(selectLoadedForm);
    const submitStep = application?.root.submitStep;

    const [qrCode, setQrCode] = useState<string>();
    const [shouldRenderConfetti, setShouldRenderConfetti] = useState(false);

    useEffect(() => {
        if (
            props.submission == null ||
            props.submission.paymentInformation == null ||
            props.submission.paymentInformation.transactionRedirectUrl == null
        ) {
            return;
        }
        const url = props.submission.paymentInformation.transactionRedirectUrl;
        qrcode.toDataURL(url, function (err, url) {
            setQrCode(url);
        });
    }, [props.submission]);

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
            startVelocity: 40,
            particleCount: 2,
            angle,
            spread: 80,
            origin: {x: originX},
            colors: ['#fcaa67', '#b0413e'],
            disableForReducedMotion: true,
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

    // Mutation Observer to watch the dom for the Canvas element of the confetti to set ARIA attribute
    useEffect(() => {
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'childList') {
                    const canvasElement = document.querySelector('canvas');
                    if (canvasElement) {
                        canvasElement.setAttribute('aria-hidden', 'true');
                        observer.disconnect();
                    }
                }
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true,
        });

        return () => {
            observer.disconnect();
        };
    }, []);

    const dispatch = useAppDispatch();

    const [email, setEmail] = useState('');
    const [privacy, setPrivacy] = useState(false);
    const [privacyError, setPrivacyError] = useState<string>();
    const setPrivacyErrorWithSnackbar = useSetPrivacyErrorWithSnackbar(setPrivacyError);
    const [mailError, setMailError] = useState<string>();
    const setMailErrorWithSnackbar = useSetMailErrorWithSnackbar(setMailError);
    const [mailSent, setMailSent] = useState(false);
    const [showMailSentDialog, setShowMailSentDialog] = useState(false);

    const sendApplicationCopyMail = (): void => {
        if (props.submission != null) {
            if (privacy) {
                if (validateEmail(email)) {
                    setMailSent(true);
                    setPrivacyError(undefined);
                    setMailError(undefined);

                    useFormsApi(api)
                        .sendApplicationCopy(props.submission, email)
                        .then(() => {
                            setShowMailSentDialog(true);
                        })
                        .catch((err) => {
                            if (err.status === 400) {
                                setMailErrorWithSnackbar('Es konnte keine E-Mail an diese Adresse verschickt werden.');
                                setMailSent(false);
                            } else if (err.status === 403) {
                                setMailErrorWithSnackbar('Die Zugriffsfrist für diesen Antrag ist bereits abgelaufen. Bitte wenden Sie sich an die zuständige Dienststelle.');
                            } else if (err.status === 404) {
                                setMailErrorWithSnackbar('Der Antrag konnte nicht gefunden werden.');
                            } else if (err.status === 409) {
                                setMailErrorWithSnackbar('Sie hatten bereits zu viele Fehlversuche.');
                            } else if (err.status === 406) {
                                setMailErrorWithSnackbar('Sie haben bereits eine E-Mail für diesen Antrag verschickt.');
                            } else {
                                console.error(err);
                                dispatch(showErrorSnackbar('Es ist ein Fehler aufgetreten. Bitte versuchen Sie es später erneut.'));
                            }
                        });
                } else {
                    setMailErrorWithSnackbar('Bitte geben Sie eine gültige E-Mail-Adresse ein.');
                }
            } else {
                setPrivacyErrorWithSnackbar('Sie müssen Ihr Einverständnis zum Versand der E-Mail geben.');
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

    useEffect(() => {
        const mediaQuery = window.matchMedia('(prefers-reduced-motion: no-preference)');
        const handleChange = () => {
            setShouldRenderConfetti(mediaQuery.matches);
        };
        handleChange();
        mediaQuery.addEventListener('change', handleChange);
        return () => {
            mediaQuery.removeEventListener('change', handleChange);
        };
    }, []);

    return (
        <>
            {
                props.submission?.paymentInformation?.status === XBezahldienstePaymentStatus.Initial &&
                <Box>
                    <Grid
                        container
                        columnSpacing={2}
                    >
                        <Grid
                            item
                            xs={12}
                            md={8}
                        >
                            <AlertComponent
                                color="warning"
                                title="Bitte bezahlen Sie die für Ihren Antrag anfallenden Gebühren"
                                sx={{my: 0}}
                            >
                                <p>
                                    Um Ihren Antrag bearbeiten zu können, ist die Bezahlung der Gebühren erforderlich.
                                    Die Zahlung wird durch den Dienstleister <strong>{PaymentProviderLabels[props.form.paymentProvider ?? PaymentProvider.ePayBL]}</strong> abgewickelt.
                                    Bitte achten Sie darauf, dass Sie die Zahlungs&shy;informationen korrekt eingeben und den Bezahlvorgang vollständig abschließen.
                                </p>
                                <p>
                                    <strong>Wichtig:</strong>
                                    &nbsp;Ihr Antrag wird erst nach erfolgter Zahlung bearbeitet.
                                </p>
                            </AlertComponent>
                        </Grid>

                        <Grid
                            item
                            xs={12}
                            md={4}
                            sx={{
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'center',
                            }}
                        >
                            <Box
                                sx={{
                                    mt: {
                                        xs: 2,
                                        md: -2,
                                    },
                                }}
                            >
                                <a href={props.submission.paymentInformation.transactionRedirectUrl}>
                                    <img
                                        src={qrCode}
                                        width="200px"
                                        height="200px"
                                        alt="QR-Code"
                                    />
                                </a>
                            </Box>

                            <Button
                                component="a"
                                variant="contained"
                                href={props.submission.paymentInformation.transactionRedirectUrl}
                                size={'large'}
                                startIcon={<PaymentOutlinedIcon
                                    sx={{marginTop: '-2px'}}
                                />}
                                sx={{mt: '5px'}}
                            >
                                Jetzt online bezahlen
                            </Button>
                        </Grid>
                    </Grid>
                </Box>
            }

            {
                props.submission?.paymentInformation?.status === XBezahldienstePaymentStatus.Payed &&
                <AlertComponent
                    color="success"
                    title="Bezahlung erfolgreich abgeschlossen"
                    sx={{my: 0}}
                >
                    Sie haben Ihre Gebühren erfolgreich online bezahlt.
                    Der Antrag wird nach Bestätigung durch den Zahlungs&shy;dienstleister (in der Regel innerhalb weniger Minuten) für die weitere Bearbeitung freigegeben.
                    Vielen Dank!
                </AlertComponent>
            }

            {
                props.submission?.paymentInformation != null &&
                <Divider sx={{my: 8}} />
            }

            {
                submitStep?.textPostSubmit != null &&
                !isStringNullOrEmpty(submitStep?.textPostSubmit) &&
                <Preamble
                    text={submitStep?.textPostSubmit}
                    logoLink={application?.root.introductionStep.initiativeLogoLink}
                    logoAlt={application?.root.introductionStep.initiativeName}
                />
            }

            {
                props.submission.archived == null &&
                <Grid
                    container
                    spacing={6}
                    sx={{
                        mt: 4,
                    }}
                >
                    <Grid
                        item
                        md={6}
                    >
                        <Typography
                            component="h3"
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
                    </Grid>
                    <Grid
                        item
                        md={6}
                    >
                        <Typography
                            component="h3"
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
                            placeholder="name@beispiel.de"
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
            }


            {
                props.submission.archived != null &&
                <Box
                    sx={{
                        mt: 4,
                    }}
                >
                    <AlertComponent
                        color="warning"
                        title="Zugriff abgelaufen"
                    >
                        Aus Sicherheitsgründen ist der Zugriff auf Ihren eingereichten Antrag nicht mehr möglich. Dies passiert im Regelfall, wenn zu viel Zeit zwischen der Einreichung des Antrages und dem Bezahlen der Gebühren vergeht.
                        Sollten Sie die von Ihnen eingereichten Antragsunterlagen inklusive des Zahlungsbelegs für Ihre Unterlagen wünschen, wenden Sie sich bitte an den Fachlichen Support auf der <Link
                        onClick={() => {
                            dispatch(showDialog(HelpDialogId));
                        }}
                    >Hilfe-Seite</Link>.
                        <br />
                        Zur eindeutigen Identifizierung Ihrer Einreichung geben Sie bitte folgende Kennung an: {props.submission.id}.
                    </AlertComponent>
                </Box>
            }

            <Divider sx={{my: 8}} />

            <Typography
                component="h3"
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
                Ihre Rückmeldung wird anonym an uns übertragen und hilft uns <br />
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
                    onChange={(newValue) => {
                        if (props.submission != null && newValue != null) {
                            useFormsApi(api)
                                .rateApplication(props.submission, newValue);
                        }
                    }}
                />
            </Box>

            {shouldRenderConfetti && (
                <ReactCanvasConfetti
                    refConfetti={getInstance}
                    // @ts-expect-error
                    style={canvasStyles}
                />
            )}

            <InfoDialog
                title="E-Mail versendet"
                severity="success"
                open={showMailSentDialog}
                onClose={() => {
                    setShowMailSentDialog(false);
                }}
            >
                Eine E-Mail mit dem eingereichten Antrag wurde an die angegebene <span style={{whiteSpace: 'nowrap'}}>E-Mail-Adresse</span> versendet.
            </InfoDialog>
        </>
    );
}
