import {Box, Button, Container, Divider, Grid, Link, Typography, useTheme} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {Preamble} from '../preamble/preamble';
import {showDialog} from '../../slices/app-slice';
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
import CheckCircleTwoToneIcon from '@mui/icons-material/CheckCircleTwoTone';
import {Rating} from '../rating/rating';
import {AlertComponent} from '../alert/alert-component';
import qrcode from 'qrcode';
import {HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import {SubmissionStatusResponseDTO} from '../../modules/submissions/dtos/submission-status-response-dto';
import {createApiPath} from '../../utils/url-path-utils';
import {ElementType} from '../../data/element-type/element-type';
import {SubmitStepElement} from '../../models/elements/steps/submit-step-element';
import type {IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {CanvasConfettiOverlay} from '../confetti/canvas-confetti-overlay';
import {FormLayoutElement} from '../../models/elements/form-layout-element';
import {ProcessNodeEntity} from '../../modules/process/entities/process-node-entity';
import {ProcessEntity} from '../../modules/process/entities/process-entity';
import {ProcessVersionEntity} from '../../modules/process/entities/process-version-entity';
import {FormDepartmentAddresses} from '../form-department-addresses/form-department-addresses';

interface SubmittedProps {
    startedProcessAccessKey: string;
    formElement: FormLayoutElement;
    node: ProcessNodeEntity;
    process: ProcessEntity;
    version: ProcessVersionEntity;
}

const submittedConfettiColors = ['#fcaa67', '#b0413e'];
const handledConfettiAccessKeys = new Set<string>();

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
    const {
        formElement,
        startedProcessAccessKey,
    } = props;

    const theme = useTheme();

    const submitStep = formElement.children?.find(c => c.type === ElementType.SubmitStep) as SubmitStepElement;
    const confettiDisabled = submitStep?.disableConfetti === true;

    const [status, setStatus] = useState<SubmissionStatusResponseDTO>();

    const [qrCode, setQrCode] = useState<string>();
    const [confettiPlayKey, setConfettiPlayKey] = useState<number | null>(null);

    useEffect(() => {
        const trimmedAccessKey = startedProcessAccessKey.trim();

        if (trimmedAccessKey.length === 0 || handledConfettiAccessKeys.has(trimmedAccessKey)) {
            setConfettiPlayKey(null);
            return;
        }

        handledConfettiAccessKeys.add(trimmedAccessKey);

        if (confettiDisabled || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            setConfettiPlayKey(null);
            return;
        }

        setConfettiPlayKey((currentValue) => (currentValue ?? 0) + 1);
    }, [confettiDisabled, startedProcessAccessKey]);

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

                    /* TODO: Implement Send Mail Copy
                    new FormApiService()
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
                     */
                } else {
                    setMailErrorWithSnackbar('Bitte geben Sie eine gültige E-Mail-Adresse ein.');
                }
            } else {
                setPrivacyErrorWithSnackbar('Sie müssen Ihr Einverständnis zum Versand der E-Mail geben.');
            }
        }
    };

    return (
        <Container
            component="main"
            role="main"
            sx={{
                pt: 8,
                pb: 16,
            }}
        >
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    mb: 4,
                }}
            >
                <Typography
                    component="h2"
                    sx={{
                        fontFamily: theme.typography.h2.fontFamily,
                        fontWeight: 500,
                        fontSize: '1.3125rem',
                        lineHeight: 1.2,
                        color: 'text.primary',
                        pt: '4px',
                        m: 0,
                    }}
                >
                    Angaben erfolgreich übermittelt
                </Typography>
                <CheckCircleTwoToneIcon
                    sx={{
                        color: theme.palette.primary.main,
                        flexShrink: 0,
                        mt: -0.75,
                        ml: 0.75,
                        transform: 'translateY(5px)',
                    }}
                />
            </Box>

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
                                md: 8,
                            }}
                        >
                            <AlertComponent
                                color="warning"
                                title="Bitte bezahlen Sie die für Ihren Antrag anfallenden Gebühren"
                                sx={{my: 0}}
                            >
                                <p>
                                    Um Ihren Antrag bearbeiten zu können, ist die Bezahlung der Gebühren erforderlich.
                                    Die Zahlung wird durch den
                                    Dienstleister <strong>{status.paymentProviderName}</strong> abgewickelt.
                                    Bitte achten Sie darauf, dass Sie die Zahlungs&shy;informationen korrekt eingeben
                                    und den Bezahlvorgang vollständig abschließen.
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
                                md: 4,
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
                    Der Antrag wird nach Bestätigung durch den Zahlungs&shy;dienstleister (in der Regel innerhalb
                    weniger Minuten) für die weitere Bearbeitung freigegeben.
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
                    <br/>
                    Zur eindeutigen Identifizierung Ihrer Einreichung geben Sie bitte folgende Kennung
                    an: {status.submissionId}.
                </AlertComponent>
            }
            {
                status != null &&
                status.paymentProviderName != null &&
                status.paymentProviderUrl != null &&
                <Divider sx={{my: 8, maxWidth: 800}}/>
            }
            {
                submitStep?.textPostSubmit != null &&
                !isStringNullOrEmpty(submitStep?.textPostSubmit) &&
                <Preamble
                    text={submitStep?.textPostSubmit}
                    logoLink={(formElement.children?.find(c => c.type === ElementType.IntroductionStep) as IntroductionStepElement)?.initiativeLogoLink ?? undefined}
                    logoAlt={(formElement.children?.find(c => c.type === ElementType.IntroductionStep) as IntroductionStepElement)?.initiativeName ?? undefined}
                />
            }
            <FormDepartmentAddresses
                formElement={formElement}
                variant="grid"
            />
            {
                status != null &&
                !status.accessExpired &&
                <Grid
                    container
                    columnSpacing={6}
                    rowSpacing={6}
                    sx={{
                        mt: 4,
                    }}
                >
                    <Grid
                        size={{
                            xs: 12,
                            md: 6,
                        }}
                    >
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
                            xs: 12,
                            md: 6,
                        }}
                    >
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
                        Aus Sicherheitsgründen ist der Zugriff auf Ihren eingereichten Antrag nicht mehr möglich. Dies
                        passiert im Regelfall, wenn zu viel Zeit zwischen der Einreichung des Antrages und dem Bezahlen
                        der Gebühren vergeht.
                        Sollten Sie die von Ihnen eingereichten Antragsunterlagen inklusive des Zahlungsbelegs für Ihre
                        Unterlagen wünschen, wenden Sie sich bitte an den Fachlichen Support auf der <Link
                        onClick={() => {
                            dispatch(showDialog(HelpDialogId));
                        }}
                    >Hilfe-Seite</Link>.
                        <br/>
                        Zur eindeutigen Identifizierung Ihrer Einreichung geben Sie bitte folgende Kennung
                        an: {status.submissionId}.
                    </AlertComponent>
                </Box>
            }
            <Divider sx={{my: 8, maxWidth: 800}}/>
            <Typography
                component="h3"
                variant="h5"
            >
                Wie hat Ihnen dieser Prozess gefallen?
            </Typography>
            <Typography
                sx={{
                    mt: 1,
                    maxWidth: 500,
                }}
                variant={'body2'}
            >
                Ihre Rückmeldung wird anonym an uns übertragen und hilft uns
                bei der Verbesserung unserer Anträge &amp; Prozesse. Vielen Dank!
            </Typography>
            <Box
                sx={{
                    display: 'flex',
                    justifyContent: 'flex-start',
                    mt: 4,
                }}
            >
                <Rating
                    onChange={(newValue) => {
                        if (status != null && newValue != null) {
                            /* TODO: Implement Rating
                            new FormApiService()
                                .rateApplication(status.submissionId, newValue);
                             */
                        }
                    }}
                />
            </Box>
            <CanvasConfettiOverlay
                playKey={confettiPlayKey}
                colors={submittedConfettiColors}
            />

            <InfoDialog
                title="E-Mail versendet"
                severity="success"
                open={showMailSentDialog}
                onClose={() => {
                    setShowMailSentDialog(false);
                }}
            >
                Eine E-Mail mit dem eingereichten Antrag wurde an die
                angegebene <span style={{whiteSpace: 'nowrap'}}>E-Mail-Adresse</span> versendet.
            </InfoDialog>
        </Container>
    );
}
