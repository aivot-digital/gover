import {Box, Button, Divider, Grid, Link, Typography} from '@mui/material';
import React, {useCallback, useEffect, useRef, useState} from 'react';
import {Preamble} from '../preamble/preamble';
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
import {AlertComponent} from '../alert/alert-component';
import qrcode from 'qrcode';
import {Form} from '../../models/entities/form';
import {HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import {FormsApiService} from '../../modules/forms/forms-api-service';
import {SubmissionStatusResponseDTO} from '../../modules/submissions/dtos/submission-status-response-dto';
import {SubmissionsApiService} from '../../modules/submissions/submissions-api-service';
import {SubmissionListResponseDTO} from '../../modules/submissions/dtos/submission-list-response-dto';
import {createApiPath} from '../../utils/url-path-utils';
import confetti from 'canvas-confetti';

const animationStartDelay = 200;
const animationDuration = 2000;

interface SubmittedProps {
    submission: SubmissionListResponseDTO;
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

export function Submitted(props: SubmittedProps) {
    const api = useApi();
    const application = useSelector(selectLoadedForm);
    const submitStep = application?.rootElement.submitStep;

    const [status, setStatus] = useState<SubmissionStatusResponseDTO>();

    const [qrCode, setQrCode] = useState<string>();
    const canvasRef = useRef<HTMLCanvasElement | null>(null);
    const animationInstance = useRef<ReturnType<typeof confetti.create> | null>(null);

    useEffect(() => {
        if (canvasRef.current && !animationInstance.current) {
            animationInstance.current = confetti.create(canvasRef.current, {
                resize: true,
                useWorker: true,
            });
        }
    }, []);

    const nextTickAnimation = useCallback(() => {
        animationInstance.current?.({
            particleCount: 2,
            startVelocity: 40,
            spread: 80,
            angle: 60,
            origin: {x: 0},
            colors: ['#fcaa67', '#b0413e'],
            disableForReducedMotion: true,
        });
        animationInstance.current?.({
            particleCount: 2,
            startVelocity: 40,
            spread: 80,
            angle: 120,
            origin: {x: 1},
            colors: ['#fcaa67', '#b0413e'],
            disableForReducedMotion: true,
        });
    }, []);

    const intervalId = useRef<NodeJS.Timeout | null>(null);

    const startAnimation = useCallback(() => {
        if (!intervalId.current) {
            intervalId.current = setInterval(nextTickAnimation, 16);
        }
    }, [nextTickAnimation]);

    const pauseAnimation = useCallback(() => {
        if (intervalId.current) {
            clearInterval(intervalId.current);
            intervalId.current = null;
        }
    }, []);

    useEffect(() => {
        const timer = setTimeout(() => {
            startAnimation();
        }, animationStartDelay);
        return () => clearTimeout(timer);
    }, [startAnimation]);

    useEffect(() => {
        const timer = setTimeout(() => {
            pauseAnimation();
        }, animationDuration);
        return () => clearTimeout(timer);
    }, [pauseAnimation]);

    useEffect(() => {
        return () => {
            if (intervalId.current) {
                clearInterval(intervalId.current);
            }
        };
    }, []);

    useEffect(() => {
        new SubmissionsApiService(api)
            .getStatus(props.submission.id)
            .then(setStatus);
    }, [api, props.submission]);

    useEffect(() => {
        if (
            status == null ||
            status.paymentProviderUrl == null
        ) {
            return;
        }
        const url = status.paymentProviderUrl;
        qrcode.toDataURL(url, function (err, url) {
            setQrCode(url);
        });
    }, [status]);

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
        if (status != null) {
            if (privacy) {
                if (validateEmail(email)) {
                    setMailSent(true);
                    setPrivacyError(undefined);
                    setMailError(undefined);

                    new FormsApiService(api)
                        .sendApplicationCopy(status.submissionId, email)
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

    return (
        <>
            {
                status != null &&
                status.paymentProviderName != null &&
                status.paymentProviderUrl != null &&
                !status.paymentDone &&
                <Box>
                    <Grid
                        container
                        columnSpacing={4}
                    >
                        <Grid
                            size={{
                                xs: 12,
                                md: 8
                            }}>
                            <AlertComponent
                                color="warning"
                                title="Bitte bezahlen Sie die für Ihren Antrag anfallenden Gebühren"
                                sx={{my: 0}}
                            >
                                <p>
                                    Um Ihren Antrag bearbeiten zu können, ist die Bezahlung der Gebühren erforderlich.
                                    Die Zahlung wird durch den Dienstleister <strong>{status.paymentProviderName}</strong> abgewickelt.
                                    Bitte achten Sie darauf, dass Sie die Zahlungs&shy;informationen korrekt eingeben und den Bezahlvorgang vollständig abschließen.
                                </p>
                                <p>
                                    <strong>Wichtig:</strong>
                                    &nbsp;Ihr Antrag wird erst nach erfolgter Zahlung bearbeitet.
                                </p>
                            </AlertComponent>
                        </Grid>

                        <Grid
                            sx={{
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'center',
                            }}
                            size={{
                                xs: 12,
                                md: 4
                            }}>
                            <Box
                                sx={{
                                    mt: {
                                        xs: 2,
                                        md: -2,
                                    },
                                }}
                            >
                                <a
                                    href={status.paymentProviderUrl}
                                    target={'_blank'}
                                >
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
                                href={status.paymentProviderUrl}
                                size={'large'}
                                startIcon={<PaymentOutlinedIcon
                                    sx={{marginTop: '-2px'}}
                                />}
                                sx={{mt: '5px'}}
                                target={'_blank'}
                            >
                                Jetzt online bezahlen
                            </Button>
                        </Grid>
                    </Grid>
                </Box>
            }
            {
                status != null &&
                status.paymentDone &&
                <AlertComponent
                    color="success"
                    title="Bezahlung erfolgreich abgeschlossen"
                    sx={{
                        my: 0,
                        mb: 4,
                    }}
                >
                    Sie haben Ihre Gebühren erfolgreich online bezahlt.
                    Der Antrag wird nach Bestätigung durch den Zahlungs&shy;dienstleister (in der Regel innerhalb weniger Minuten) für die weitere Bearbeitung freigegeben.
                    Vielen Dank!
                </AlertComponent>
            }
            {
                status != null &&
                status.paymentFailed &&
                <AlertComponent
                    color="error"
                    title="Bezahlung fehlgeschlagen"
                    sx={{
                        my: 0,
                        mb: 4,
                    }}
                >
                    Die Bezahlung der Gebühren ist fehlgeschlagen. Bitte wenden Sie sich an die zuständige Dienststelle.
                    <br />
                    Zur eindeutigen Identifizierung Ihrer Einreichung geben Sie bitte folgende Kennung an: {status.submissionId}.
                </AlertComponent>
            }
            {
                status != null &&
                status.paymentProviderName != null &&
                status.paymentProviderUrl != null &&
                <Divider sx={{my: 8}} />
            }
            {
                submitStep?.textPostSubmit != null &&
                !isStringNullOrEmpty(submitStep?.textPostSubmit) &&
                <Preamble
                    text={submitStep?.textPostSubmit}
                    logoLink={application?.rootElement.introductionStep?.initiativeLogoLink ?? undefined}
                    logoAlt={application?.rootElement.introductionStep?.initiativeName ?? undefined}
                />
            }
            {
                status != null &&
                !status.accessExpired &&
                <Grid
                    container
                    spacing={6}
                    sx={{
                        mt: 4,
                    }}
                >
                    <Grid
                        size={{
                            md: 6
                        }}>
                        <Typography
                            component="h3"
                            variant="h5"
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
                            href={createApiPath(`/api/public/prints/${status.submissionId}`)}
                            size="large"
                        >
                            Antrag als PDF herunterladen
                        </Button>
                    </Grid>
                    <Grid
                        size={{
                            md: 6
                        }}>
                        <Typography
                            component="h3"
                            variant="h5"
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
                            disabled={status.copySent || mailSent}
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
                            disabled={status.copySent || mailSent}
                        />

                        <Button
                            sx={{mt: 4}}
                            variant="contained"
                            startIcon={<EmailOutlinedIcon
                                sx={{marginTop: '-2px'}}
                            />}
                            onClick={sendApplicationCopyMail}
                            size={'large'}
                            disabled={status.copySent || mailSent}
                        >
                            Antrag per E-Mail erhalten
                        </Button>
                    </Grid>
                </Grid>
            }
            {
                status != null &&
                status.accessExpired &&
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
                        Zur eindeutigen Identifizierung Ihrer Einreichung geben Sie bitte folgende Kennung an: {status.submissionId}.
                    </AlertComponent>
                </Box>
            }
            <Divider sx={{my: 8}} />
            <Typography
                component="h3"
                variant="h5"
                sx={{textAlign: 'center'}}
            >
                Wie hat Ihnen dieser Prozess gefallen?
            </Typography>
            <Typography
                sx={{
                    textAlign: 'center',
                    mt: 1,
                    maxWidth: 500,
                    mx: "auto",
                }}
                variant={'body2'}
            >
                Ihre Rückmeldung wird anonym an uns übertragen und hilft uns
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
                        if (status != null && newValue != null) {
                            new FormsApiService(api)
                                .rateApplication(status.submissionId, newValue);
                        }
                    }}
                />
            </Box>
            <canvas
                ref={canvasRef}
                style={{
                    position: 'fixed',
                    pointerEvents: 'none',
                    width: '100%',
                    height: '100%',
                    top: 0,
                    left: 0,
                    zIndex: 9999,
                    display: 'block',
                    background: 'transparent',
                }}
                aria-hidden="true"
            />
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
