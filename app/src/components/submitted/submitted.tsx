import {Box, Button, CircularProgress, Container, Divider, Grid, Link, Typography, useTheme} from '@mui/material';
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
import EmailOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Mail';
import PictureAsPdfOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/PictureAsPdf';
import PaymentOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CreditCard';
import CheckCircleTwoToneIcon from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import ErrorOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Error';
import {Rating} from '../rating/rating';
import {AlertComponent} from '../alert/alert-component';
import qrcode from 'qrcode';
import {HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import {SubmissionStatusResponseDTO} from '../../modules/submissions/dtos/submission-status-response-dto';
import {ElementType} from '../../data/element-type/element-type';
import {SubmitStepElement} from '../../models/elements/steps/submit-step-element';
import type {IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {FormLayoutElement} from '../../models/elements/form-layout-element';
import {FormDepartmentAddresses} from '../form-department-addresses/form-department-addresses';
import {
    CustomerTaskViewApiService,
} from '../../pages/customer-pages/customer-instance-view/customer-task-view-api-service';
import {FormTriggerApiService} from '../../modules/forms/services/form-trigger-api-service';
import {downloadBlobFile} from '../../utils/download-utils';
import {ProcessTaskStatus} from '../../modules/process/enums/process-task-status';
import {ProcessInstanceStatus} from '../../modules/process/enums/process-instance-status';

interface SubmittedProps {
    startedProcessAccessKey: string;
    paymentRequired: boolean;
    formElement: FormLayoutElement;
    processSlug: string;
    formSlug: string | null;
    processVersion: number;
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
    const {
        formElement,
        formSlug,
        paymentRequired,
        processSlug,
        processVersion,
        startedProcessAccessKey,
    } = props;

    const theme = useTheme();

    const submitStep = formElement.children?.find(c => c.type === ElementType.SubmitStep) as SubmitStepElement;

    const [status, setStatus] = useState<SubmissionStatusResponseDTO>();

    const [qrCode, setQrCode] = useState<string>();
    const [formTaskAccessKey, setFormTaskAccessKey] = useState<string>();
    const [processFailed, setProcessFailed] = useState(false);
    const [isPrintDownloadPending, setIsPrintDownloadPending] = useState(false);
    const [preparationError, setPreparationError] = useState<string>();
    const [preparationDelayed, setPreparationDelayed] = useState(false);
    const [preparationAttempt, setPreparationAttempt] = useState(0);
    const [paymentReady, setPaymentReady] = useState(false);
    const [paymentCompleted, setPaymentCompleted] = useState(false);
    const SubmittedStatusIcon = processFailed ? ErrorOutlineOutlinedIcon : CheckCircleTwoToneIcon;

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

    useEffect(() => {
        const accessKey = startedProcessAccessKey.trim();
        setFormTaskAccessKey(undefined);
        setProcessFailed(false);
        setPreparationError(undefined);
        setPreparationDelayed(false);
        setPaymentReady(false);
        setPaymentCompleted(false);

        if (accessKey.length === 0) {
            setPreparationError('Der Status Ihrer Einreichung konnte nicht abgerufen werden.');
            return;
        }

        let cancelled = false;
        let pollTimer: ReturnType<typeof setTimeout> | undefined;
        // A slow preparation is not proof of a failed submission or payment.
        const preparationTimer = setTimeout(() => {
            cancelled = true;
            clearTimeout(pollTimer);
            setPreparationDelayed(true);
        }, 30_000);

        const fetchFirstTask = async () => {
            try {
                const response = await new CustomerTaskViewApiService().getInstanceStatus(accessKey);
                if (cancelled) {
                    return;
                }

                const task = response.tasks?.[0];
                const failed = response.status === ProcessInstanceStatus.Failed ||
                    response.status === ProcessInstanceStatus.Aborted ||
                    task?.status === ProcessTaskStatus.Failed ||
                    task?.status === ProcessTaskStatus.Aborted ||
                    task?.status === ProcessTaskStatus.DeadlineExceeded;
                const ready = task?.status === ProcessTaskStatus.AwaitingPayment || task?.status === ProcessTaskStatus.Completed;
                if (failed || ready) {
                    setProcessFailed(failed);
                    // Keep the submitted PDF accessible even if later processing has failed.
                    setFormTaskAccessKey(task?.accessKey);
                    setPaymentReady(!failed && task?.status === ProcessTaskStatus.AwaitingPayment);
                    // A completed form task can have a successful payment even when a later node fails.
                    setPaymentCompleted(task?.status === ProcessTaskStatus.Completed);
                    clearTimeout(preparationTimer);
                    return;
                }
                // Schedule after the response so slow requests cannot overlap and overwrite newer results.
                pollTimer = setTimeout(fetchFirstTask, 1000);
            } catch {
                if (cancelled) {
                    return;
                }
                clearTimeout(preparationTimer);
                setPreparationError('Der Status Ihrer Einreichung konnte nicht abgerufen werden. Bitte prüfen Sie den Status erneut.');
            }
        };

        void fetchFirstTask();
        return () => {
            cancelled = true;
            clearTimeout(pollTimer);
            clearTimeout(preparationTimer);
        };
    }, [startedProcessAccessKey, preparationAttempt]);

    const [email, setEmail] = useState('');
    const [privacy, setPrivacy] = useState(false);
    const [privacyError, setPrivacyError] = useState<string>();
    const setPrivacyErrorWithSnackbar = useSetPrivacyErrorWithSnackbar(setPrivacyError);
    const [mailError, setMailError] = useState<string>();
    const setMailErrorWithSnackbar = useSetMailErrorWithSnackbar(setMailError);
    const [mailSent, setMailSent] = useState(false);
    const [showMailSentDialog, setShowMailSentDialog] = useState(false);
    const paymentTaskPath = !paymentReady || formTaskAccessKey == null ?
        undefined :
        `/process/${encodeURIComponent(startedProcessAccessKey.trim())}/tasks/${encodeURIComponent(formTaskAccessKey)}`;

    const downloadSubmittedPrint = (): void => {
        if (formSlug == null || formSlug.trim().length === 0 || formTaskAccessKey == null) {
            return;
        }

        setIsPrintDownloadPending(true);
        new FormTriggerApiService()
            .downloadSubmittedSummaryPdf(
                processSlug,
                formSlug.trim(),
                startedProcessAccessKey,
                formTaskAccessKey,
                processVersion,
            )
            .then((blob) => {
                downloadBlobFile('Einreichung.pdf', blob);
            })
            .catch(() => {
                dispatch(showErrorSnackbar('Ihre Einreichung konnte nicht als PDF heruntergeladen werden. Bitte versuchen Sie es später erneut.'));
            })
            .finally(() => {
                setIsPrintDownloadPending(false);
            });
    };

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
                    {processFailed ? 'Verarbeitung fehlgeschlagen' : 'Angaben erfolgreich übermittelt'}
                </Typography>
                <SubmittedStatusIcon
                    sx={{
                        color: processFailed ? theme.palette.error.main : theme.palette.primary.main,
                        flexShrink: 0,
                        mt: -0.75,
                        ml: 0.75,
                        transform: 'translateY(5px)',
                    }}
                />
            </Box>

            {
                processFailed &&
                <AlertComponent
                    color="error"
                    title="Ihre Einreichung konnte nicht weiterverarbeitet werden"
                    sx={{my: 0, mb: 4}}
                >
                    Bei der weiteren Verarbeitung Ihrer Einreichung ist ein Fehler aufgetreten. Bitte wenden Sie sich an die
                    zuständige Stelle. Die Kontaktdaten finden Sie unten.
                </AlertComponent>
            }

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
                                    href={processFailed ? undefined : status.paymentProviderUrl}
                                    aria-disabled={processFailed}
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
                                href={processFailed ? undefined : status.paymentProviderUrl}
                                disabled={processFailed}
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
            {(preparationError != null || preparationDelayed) && (
                <AlertComponent
                    color={preparationError != null ? 'error' : 'info'}
                    title={preparationError != null ? 'Status nicht verfügbar' : 'Die Vorbereitung dauert länger'}
                    sx={{mt: 4, maxWidth: 800}}
                >
                    <Typography variant="body2">
                        {preparationError ?? 'Die Vorbereitung ist noch nicht abgeschlossen. Sie können den Status erneut prüfen. Ihre Einreichung muss dafür nicht erneut abgesendet werden.'}
                    </Typography>
                    <Button sx={{mt: 2}} onClick={() => setPreparationAttempt((attempt) => attempt + 1)}>
                        Status erneut prüfen
                    </Button>
                </AlertComponent>
            )}
            {paymentRequired && (
                <Box sx={{mt: 4, maxWidth: 800}}>
                    <Typography component="h3" variant="h5">
                        {paymentCompleted ? 'Zahlung abgeschlossen' : 'Zahlung'}
                    </Typography>
                    <Typography variant="body2" sx={{mt: 1, mb: 2}}>
                        {paymentCompleted
                            ? 'Die Zahlung für Ihre Einreichung wurde abgeschlossen.'
                            : processFailed
                                ? 'Die Zahlung steht wegen des Verarbeitungsfehlers derzeit nicht zur Verfügung.'
                                : paymentReady
                                    ? 'Für Ihre Einreichung ist eine Zahlung erforderlich. Sie können jetzt zur Zahlung wechseln.'
                                    : preparationError != null || preparationDelayed
                                        ? 'Bitte prüfen Sie den Status, um festzustellen, ob die Zahlung bereitsteht.'
                                        : 'Die Zahlung für Ihre Einreichung wird vorbereitet. Sobald die Schaltfläche aktiv ist, können Sie zur Zahlung wechseln.'}
                    </Typography>
                    {!paymentCompleted && (paymentTaskPath != null ? (
                        <Button component="a" variant="contained" href={paymentTaskPath} startIcon={<PaymentOutlinedIcon/>}>
                            Zur Zahlung
                        </Button>
                    ) : (
                        <Button
                            variant="contained"
                            disabled
                            startIcon={!processFailed && preparationError == null && !preparationDelayed
                                ? <CircularProgress color="inherit" size={18} aria-label="Zahlung wird vorbereitet"/>
                                : <PaymentOutlinedIcon/>}
                        >
                            {processFailed || preparationError != null || preparationDelayed ? 'Zahlung nicht verfügbar' : 'Zahlung wird vorbereitet'}
                        </Button>
                    ))}
                </Box>
            )}
            {
                (status == null || !status.accessExpired) &&
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
                            md: status == null ? 12 : 6,
                        }}
                    >
                        <Typography
                            component="h3"
                            variant="h5"
                        >
                            Einreichung als PDF herunterladen
                        </Typography>
                        <Typography
                            sx={{
                                mt: 1,
                                mb: 4,
                            }}
                            variant={'body2'}
                        >
                            Hier können Sie Ihre eingereichten Angaben als PDF herunterladen.
                        </Typography>

                        <Button
                            variant="contained"
                            startIcon={
                                isPrintDownloadPending || (formTaskAccessKey == null && !processFailed && preparationError == null && !preparationDelayed) ?
                                    <CircularProgress
                                        color="inherit"
                                        size={18}
                                    /> :
                                    <PictureAsPdfOutlinedIcon
                                        sx={{marginTop: '-2px'}}
                                    />
                            }
                            onClick={downloadSubmittedPrint}
                            size="large"
                            disabled={formTaskAccessKey == null || isPrintDownloadPending}
                        >
                            {
                                formTaskAccessKey == null ?
                                    processFailed || preparationError != null || preparationDelayed ? 'PDF nicht verfügbar' : 'PDF wird vorbereitet' :
                                    'Einreichung als PDF herunterladen'
                            }
                        </Button>
                    </Grid>
                    {
                        status != null &&
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
                    }
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
