import React, {useEffect, useState} from 'react';
import {Box, Tab, Tabs, Typography} from '@mui/material';
import {useParams} from 'react-router-dom';
import {Form} from '../../../models/entities/form';
import {parseISO} from 'date-fns';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import FolderZipOutlinedIcon from '@mui/icons-material/FolderZipOutlined';
import CachedOutlinedIcon from '@mui/icons-material/CachedOutlined';
import {isStringNotNullOrEmpty} from '../../../utils/string-utils';
import {useApi} from '../../../hooks/use-api';
import {SubmissionEditPageAttachmentsTab} from './tabs/submission-edit-page-attachments-tab';
import {SubmissionEditPageSummaryTab} from './tabs/submission-edit-page-summary-tab';
import {SubmissionEditPageGeneralTab} from './tabs/submission-edit-page-general-tab';
import {AppToolbarAction} from '../../../components/app-toolbar/app-toolbar-props';
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import {SubmissionEditPagePaymentTab} from './tabs/submission-edit-page-payment-tab';
import {SubmissionStatus} from '../../../modules/submissions/enums/submission-status';
import {TransactionsApiService} from '../../../modules/payment/transaction-api-service';
import {PaymentProvidersApiService} from '../../../modules/payment/payment-providers-api-service';
import {useLoading} from '../../../hooks/use-loading';
import {PaymentProviderResponseDTO} from '../../../modules/payment/dtos/payment-provider-response-dto';
import {PaymentTransactionResponseDTO} from '../../../modules/payment/dtos/payment-transaction-response-dto';
import {FormsApiService} from '../../../modules/forms/forms-api-service';
import {SubmissionsApiService} from '../../../modules/submissions/submissions-api-service';
import {SubmissionDetailsResponseDTO} from '../../../modules/submissions/dtos/submission-details-response-dto';
import CreditCardOffOutlinedIcon from '@mui/icons-material/CreditCardOffOutlined';
import {ConfirmDialogV2} from '../../../dialogs/confirm-dialog/confirm-dialog-v2';
import {ConfirmDialogOptions} from '../../../hooks/use-confirm-dialog';
import {isApiError} from '../../../models/api-error';

export function SubmissionEditPage() {
    const api = useApi();
    const dispatch = useAppDispatch();

    const {
        id,
    } = useParams();

    const [form, setForm] = useState<Form>();
    const [submission, setSubmission] = useState<SubmissionDetailsResponseDTO>();
    const [paymentProvider, setPaymentProvider] = useState<PaymentProviderResponseDTO>();
    const [transaction, setTransaction] = useState<PaymentTransactionResponseDTO>();

    const [confirmArchive, setConfirmArchive] = useState<() => void>();
    const [confirmCancelDestination, setConfirmCancelDestination] = useState<() => void>();
    const [confirmCancelPayment, setConfirmCancelPayment] = useState<() => void>();

    const [confirmOptions, setConfirmOptions] = useState<ConfirmDialogOptions<any>>();

    const {
        isLoading,
        setLoadingFlag,
        clearLoadingFlag,
    } = useLoading<'submission' | 'form' | 'transaction' | 'paymentProvider'>();

    const [isNotFound, setIsNotFound] = useState(false);

    const [currentTab, setCurrentTab] = useState(0);

    useEffect(() => {
        if (id != null && api != null) {
            setLoadingFlag('submission');
            setIsNotFound(false);

            new SubmissionsApiService(api)
                .retrieve(id)
                .then((res) => {
                    setSubmission(res);
                })
                .catch((err) => {
                    console.error(err);
                    setIsNotFound(true);
                })
                .finally(() => {
                    clearLoadingFlag('submission');
                });
        }
    }, [id, api]);

    useEffect(() => {
        if (submission == null) {
            return;
        }

        setLoadingFlag('form');
        new FormsApiService(api)
            .retrieve(submission.formId)
            .then((res) => {
                setForm(res);
            })
            .finally(() => {
                clearLoadingFlag('form');
            });

        if (submission.paymentTransactionKey != null) {
            setLoadingFlag('transaction');
            new TransactionsApiService(api)
                .retrieve(submission.paymentTransactionKey)
                .then(setTransaction)
                .finally(() => {
                    clearLoadingFlag('transaction');
                });
        }
    }, [submission, api]);

    useEffect(() => {
        if (transaction == null) {
            return;
        }

        setLoadingFlag('paymentProvider');
        new PaymentProvidersApiService(api)
            .retrieve(transaction.paymentProviderKey)
            .then(setPaymentProvider)
            .finally(() => {
                clearLoadingFlag('paymentProvider');
            });
    }, [transaction, api]);


    const handleArchive = (): void => {
        if (form != null && submission != null) {
            setConfirmArchive(() => () => {
                const archivedSubmission = {
                    ...submission,
                    archived: new Date().toISOString(),
                };

                setLoadingFlag('submission');
                new SubmissionsApiService(api)
                    .update(archivedSubmission.id, {
                        assigneeId: archivedSubmission.assigneeId,
                        fileNumber: archivedSubmission.fileNumber,
                        archived: true,
                        canceled: false,
                    })
                    .then(() => {
                        setSubmission(archivedSubmission);
                        setConfirmArchive(undefined);
                        dispatch(showSuccessSnackbar('Vorgang erfolgreich archiviert'));
                    })
                    .catch((err) => {
                        if (err.status === 409) {
                            dispatch(showErrorSnackbar('Der Vorgang wurde bereits archiviert'));
                            setConfirmArchive(undefined);
                        } else {
                            console.error(err);
                            dispatch(showErrorSnackbar('Vorgang konnte nicht archiviert werden'));
                        }
                    })
                    .finally(() => {
                        clearLoadingFlag('submission');
                    });
            });
        }
    };

    const handleResendDestination = (): void => {
        if (form != null && submission != null) {
            setLoadingFlag('submission');

            new SubmissionsApiService(api)
                .resendDestination(submission.id)
                .then((resentSubmission) => {
                    setSubmission(resentSubmission);
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Erneutes Übertragen des Antrages fehlgeschlagen'));
                })
                .finally(() => {
                    clearLoadingFlag('submission');
                });
        }
    };

    const handleCancelDestination = (): void => {
        if (form != null && submission != null) {
            setConfirmCancelDestination(() => () => {

                setLoadingFlag('submission');
                new SubmissionsApiService(api)
                    .update(submission!.id, {
                        assigneeId: submission.assigneeId,
                        fileNumber: submission.fileNumber,
                        archived: submission.archived != null,
                        canceled: true,
                    })
                    .then((res) => {
                        setSubmission(res);
                        setConfirmCancelDestination(undefined);
                    })
                    .catch((err) => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Übertragung abbrechen fehlgeschlagen'));
                    })
                    .finally(() => {
                        clearLoadingFlag('submission');
                    });
            });
        }
    };

    const handleRestartPayment = () => {
        if (form == null || submission == null || api == null) {
            return;
        }

        setConfirmOptions({
            title: 'Zahlung erneut starten',
            state: {},
            onRender: (state, setState) => {
                return (
                    <Typography>
                        Sind Sie sicher, dass Sie die Zahlung erneut starten möchten?
                        Dadurch wird ein neuer Zahlungslink erstellt, den Sie der antragstellenden Person senden müssen.
                        Der Status des Antrags ändert sich zurück zu „Verarbeitung durch System“, bis die Zahlung erfolgt ist.
                    </Typography>
                );
            },
            onCancel: () => {
                setConfirmOptions(undefined);
            },
            onConfirm: () => {
                setLoadingFlag('submission');
                new SubmissionsApiService(api)
                    .restartPayment(submission.id)
                    .then((res) => {
                        setSubmission(res);
                    })
                    .catch((err) => {
                        if (isApiError(err) && isStringNotNullOrEmpty(err.details.message)) {
                            dispatch(showErrorSnackbar(err.details.message));
                        } else {
                            console.error(err);
                            dispatch(showErrorSnackbar('Zahlung erneut starten fehlgeschlagen'));
                        }
                    })
                    .finally(() => {
                        setConfirmOptions(undefined);
                        clearLoadingFlag('submission');
                    });
            },
        });
    };

    const handleCancelPayment = (): void => {
        if (form == null || submission == null || api == null) {
            return;
        }

        setConfirmOptions({
            title: 'Zahlung abbrechen',
            state: {},
            onRender: (state, setState) => {
                return (
                    <Typography>
                        Sind Sie sicher, dass Sie den Zahlungsvorgang endgültig abbrechen wollen?
                        Dies kann nicht rückgängig gemacht werden.
                        Der Antrag wird gegebenenfalls an eine hinterlegte Schnittstelle übertragen oder in die manuelle Bearbeitung überführt.
                        Sie können die Zahlung nicht erneut starten.
                    </Typography>
                );
            },
            onCancel: () => {
                setConfirmOptions(undefined);
            },
            onConfirm: () => {
                setLoadingFlag('submission');
                new SubmissionsApiService(api)
                    .cancelPayment(submission!.id)
                    .then((res) => {
                        setSubmission(res);
                        setConfirmOptions(undefined);
                    })
                    .catch((err) => {
                        console.error(err);
                        dispatch(showErrorSnackbar('"Zahlung Abbrechen" fehlgeschlagen'));
                    })
                    .finally(() => {
                        clearLoadingFlag('submission');
                    });
            },
        });
    };

    const archived = submission?.archived != null ? parseISO(submission.archived) : undefined;

    let title = 'Antrag';
    if (form != null) {
        title += ` - ${form.title} - ${form.version}`;
        if (isStringNotNullOrEmpty(submission?.fileNumber)) {
            title += ` - ${submission?.fileNumber ?? ''}`;
        }
    }

    const actions: AppToolbarAction[] = [];

    if (archived == null) {
        if (
            submission?.status === SubmissionStatus.OpenForManualWork ||
            submission?.status === SubmissionStatus.ManualWorkingOn
        ) {
            actions.push({
                icon: <FolderZipOutlinedIcon />,
                tooltip: 'Vorgang abschließen',
                onClick: handleArchive,
            });
        }

        if (submission?.status === SubmissionStatus.HasPaymentError) {
            actions.push({
                icon: <CachedOutlinedIcon />,
                tooltip: 'Zahlung erneut starten',
                onClick: handleRestartPayment,
            });

            actions.push({
                icon: <CreditCardOffOutlinedIcon />,
                tooltip: 'Zahlung abbrechen',
                onClick: handleCancelPayment,
            });
        }

        if (submission?.status === SubmissionStatus.HasDestinationError) {
            actions.push({
                icon: <CachedOutlinedIcon />,
                tooltip: 'Erneut übertragen',
                onClick: handleResendDestination,
            });

            actions.push({
                icon: <CancelOutlinedIcon />,
                tooltip: 'Übertragung abbrechen',
                onClick: handleCancelDestination,
            });
        }
    }

    return (
        <PageWrapper
            title={isLoading ? 'Lade...' : (isNotFound ? 'Nicht gefunden' : title)}
            isLoading={isLoading}
            is404={isNotFound}
            toolbarActions={actions}
        >
            <Box
                sx={{
                    mt: -4,
                    '&::after': {
                        position: 'absolute',
                        content: '""',
                        display: 'block',
                        width: '100%',
                        left: 0,
                        right: 0,
                        height: '1px',
                        backgroundColor: 'divider',
                    },
                }}
            >
                <Tabs
                    value={currentTab}
                    onChange={(_, newValue) => {
                        setCurrentTab(newValue);
                    }}
                    variant="scrollable"
                    scrollButtons="auto"
                >
                    <Tab
                        value={0}
                        label="Allgemeine Informationen"
                    />
                    <Tab
                        value={1}
                        label="Antragsdaten"
                    />
                    {
                        transaction != null &&
                        <Tab
                            value={2}
                            label="Zahlungsinformationen"
                        />
                    }
                    <Tab
                        value={3}
                        label="Anlagen"
                    />
                </Tabs>
            </Box>

            {
                currentTab === 0 &&
                form != null &&
                submission != null &&
                <SubmissionEditPageGeneralTab
                    api={api}
                    form={form}
                    submission={submission}
                    onChangeSubmission={setSubmission}
                />
            }

            {
                currentTab === 1 &&
                form != null &&
                submission != null &&
                <SubmissionEditPageSummaryTab
                    form={form}
                    submission={submission}
                />
            }


            {
                currentTab === 2 &&
                form != null &&
                submission != null &&
                transaction != null &&
                paymentProvider != null &&
                <SubmissionEditPagePaymentTab
                    form={form}
                    submission={submission}
                    transaction={transaction}
                    paymentProvider={paymentProvider}
                />
            }

            {
                currentTab === 3 &&
                form != null &&
                submission != null &&
                <SubmissionEditPageAttachmentsTab
                    api={api}
                    form={form}
                    submission={submission}
                />
            }

            <ConfirmDialog
                title="Vorgang abschließen"
                onConfirm={confirmArchive}
                onCancel={() => {
                    setConfirmArchive(undefined);
                }}
            >
                Sind Sie sicher, dass Sie den Vorgang abschließen wollen? Der Vorgang kann danach nicht wieder in
                Bearbeitung genommen werden.
            </ConfirmDialog>

            <ConfirmDialog
                title="Übertragung abbrechen"
                onConfirm={confirmCancelDestination}
                onCancel={() => {
                    setConfirmCancelDestination(undefined);
                }}
            >
                Sind Sie sicher, dass Sie die Übertragung abbrechen wollen?
                Der Vorgang kann danach nicht erneut übertragen werden und muss manuell bearbeitet werden.
            </ConfirmDialog>

            <ConfirmDialog
                title="Zahlung abbrechen"
                onConfirm={confirmCancelPayment}
                onCancel={() => {
                    setConfirmCancelPayment(undefined);
                }}
            >
                Sind Sie sicher, dass Sie die Zahlung abbrechen wollen?
                Die Zahhlung kann danach nicht erneut gestartet werden.
            </ConfirmDialog>

            <ConfirmDialogV2
                options={confirmOptions}
            />
        </PageWrapper>
    );
}
