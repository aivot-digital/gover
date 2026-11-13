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
    const [formTaskCompleted, setFormTaskCompleted] = useState(false);
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
        setFormTaskCompleted(false);

        if (accessKey.length === 0) {
            setPreparationError('Der Status Ihres Vorgangs konnte nicht abgerufen werden.');
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
                    // Completion also covers submissions whose calculated payment has no positive positions.
                    // It does not prove that money was paid.
                    setFormTaskCompleted(task?.status === ProcessTaskStatus.Completed);
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
                setPreparationError('Der Status Ihres Vorgangs konnte nicht abgerufen werden. Bitte prüfen Sie den Status erneut.');
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
                downloadBlobFile('Angaben.pdf', blob);
            })
            .catch(() => {
                dispatch(showErrorSnackbar('Die Kopie Ihrer Angaben konnte nicht als PDF heruntergeladen werden. Bitte versuchen Sie es später erneut.'));
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
                                setMailErrorWithSnackbar('Die Zugriffsfrist für diesen Vorgang ist bereits abgelaufen. Bitte wenden Sie sich an die verantwortliche Stelle. Öffnen Sie „Hilfe“ am Ende der Seite. Dort finden Sie die Kontaktinformationen.');
                            } else if (err.status === 404) {
                                setMailErrorWithSnackbar('Der Vorgang konnte nicht gefunden werden.');
                            } else if (err.status === 409) {
                                setMailErrorWithSnackbar('Sie hatten bereits zu viele Fehlversuche.');
                            } else if (err.status === 406) {
                                setMailErrorWithSnackbar('Sie haben bereits eine E-Mail für diesen Vorgang verschickt.');
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
                !formTaskCompleted &&
                <AlertComponent
                    color="error"
                    title="Ihre Angaben konnten nicht weiterverarbeitet werden"
                    sx={{my: 0, mb: 4}}
                >
                    Bei der weiteren Verarbeitung Ihrer Angaben ist ein Fehler aufgetreten. Bitte wenden Sie sich an die
                    verantwortliche Stelle. Öffnen Sie „Hilfe“ am Ende der Seite. Dort finden Sie die Kontaktinformationen.
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
                                title="Bitte schließen Sie die Zahlung ab"
                                sx={{my: 0}}
                            >
                                <p>
                                    Für diesen Vorgang ist eine Zahlung erforderlich.
                                    Die Zahlung wird durch den
                                    Dienstleister <strong>{status.paymentProviderName}</strong> abgewickelt.
                                    Bitte achten Sie darauf, dass Sie die Zahlungs&shy;informationen korrekt eingeben
                                    und den Bezahlvorgang vollständig abschließen.
                                </p>
                                <p>
                                    <strong>Wichtig:</strong>
                                    &nbsp;Die weitere Verarbeitung wartet auf die Bestätigung der Zahlung.
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
                    title="Zahlung abgeschlossen"
                    sx={{
                        my: 0,
                        mb: 4,
                    }}
                >
                    Die Zahlung wurde erfolgreich abgeschlossen.
                </AlertComponent>
            }
            {
                status != null &&
                status.paymentFailed &&
                <AlertComponent
                    color="error"
                    title="Zahlung fehlgeschlagen"
                    sx={{
                        my: 0,
                        mb: 4,
                    }}
                >
                    Die Zahlung ist fehlgeschlagen. Bitte wenden Sie sich an die verantwortliche Stelle.
                    Öffnen Sie „Hilfe“ am Ende der Seite. Dort finden Sie die Kontaktinformationen.
                    <br/>
                    Bitte geben Sie zur Zuordnung Ihres Vorgangs folgende Kennung
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
                        {preparationError ?? 'Die Vorbereitung ist noch nicht abgeschlossen. Sie können den Status erneut prüfen. Sie müssen Ihre Angaben dafür nicht erneut übermitteln.'}
                    </Typography>
                    {startedProcessAccessKey.trim().length > 0 && (
                        <Button sx={{mt: 2}} onClick={() => setPreparationAttempt((attempt) => attempt + 1)}>
                            Status erneut prüfen
                        </Button>
                    )}
                </AlertComponent>
            )}
            {paymentRequired && (
                <Box sx={{mt: 4, maxWidth: 800}}>
                    <Typography component="h3" variant="h5">
                        Zahlung
                    </Typography>
                    <Typography variant="body2" sx={{mt: 1, mb: 2}}>
                        {formTaskCompleted
                            ? 'Für diese Übermittlung ist keine weitere Zahlung erforderlich.'
                            : processFailed
                                ? 'Die Zahlung steht wegen des Verarbeitungsfehlers derzeit nicht zur Verfügung.'
                                : paymentReady
                                    ? 'Für diesen Vorgang ist eine Zahlung erforderlich. Sie können jetzt zur Zahlung wechseln.'
                                    : preparationError != null || preparationDelayed
                                        ? 'Bitte prüfen Sie den Status, um festzustellen, ob die Zahlung bereitsteht.'
                                        : 'Die Zahlung für Ihren Vorgang wird vorbereitet. Sobald die Schaltfläche aktiv ist, können Sie zur Zahlung wechseln.'}
                    </Typography>
                    {!formTaskCompleted && (paymentTaskPath != null ? (
                        <Button component="a" variant="contained" href={paymentTaskPath} startIcon={<PaymentOutlinedIcon/>}>
                            Zur Zahlung
                        </Button>
                    ) : (
                        <Button
                            variant="contained"
                            disabled
                            startIcon={!processFailed && preparationError == null && !preparationDelayed
                                ? <CircularProgress color="inherit" size={18} aria-hidden="true"/>
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
                            Angaben als PDF herunterladen
                        </Typography>
                        <Typography
                            sx={{
                                mt: 1,
                                mb: 4,
                            }}
                            variant={'body2'}
                        >
                            Hier können Sie eine Kopie Ihrer übermittelten Angaben als PDF herunterladen.
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
                                    'Angaben als PDF herunterladen'
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
                                Kopie der Angaben per E-Mail erhalten
                            </Typography>

                            <Typography
                                sx={{
                                    mt: 1,
                                    mb: 2.4,
                                }}
                                variant={'body2'}
                            >
                                Sie können sich eine Kopie Ihrer Angaben an die folgende E-Mail-Adresse senden lassen.
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
                                label="Ich bin damit einverstanden, dass eine Kopie meiner Angaben per E-Mail ohne Ende-zu-Ende-Verschlüsselung versandt wird."
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
                                Kopie der Angaben per E-Mail erhalten
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
                        Der Zugriff auf Ihre übermittelten Angaben ist abgelaufen.
                        Wenn Sie eine Kopie Ihrer Angaben oder einen Zahlungsbeleg benötigen, wenden Sie sich bitte
                        an den fachlichen Support. Öffnen Sie „<Link
                        onClick={() => {
                            dispatch(showDialog(HelpDialogId));
                        }}
                    >Hilfe</Link>“ am Ende der Seite. Dort finden Sie die Kontaktinformationen.
                        <br/>
                        Bitte geben Sie zur Zuordnung Ihres Vorgangs folgende Kennung
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
                bei der Verbesserung unserer Formulare und Prozesse. Vielen Dank!
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
                Eine Kopie Ihrer Angaben wurde an die
                angegebene <span style={{whiteSpace: 'nowrap'}}>E-Mail-Adresse</span> versendet.
            </InfoDialog>
        </Container>
    );
}
