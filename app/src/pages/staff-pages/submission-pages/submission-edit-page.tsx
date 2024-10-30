import React, {useEffect, useState} from 'react';
import {Tab, Tabs} from '@mui/material';
import {useParams} from 'react-router-dom';
import {type Form as Application} from '../../../models/entities/form';
import {parseISO} from 'date-fns';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import FolderZipOutlinedIcon from '@mui/icons-material/FolderZipOutlined';
import CachedOutlinedIcon from '@mui/icons-material/CachedOutlined';
import {isStringNotNullOrEmpty} from '../../../utils/string-utils';
import {useApi} from '../../../hooks/use-api';
import {useFormsApi} from '../../../hooks/use-forms-api';
import {useSubmissionsApi} from '../../../hooks/use-submissions-api';
import {Submission} from '../../../models/entities/submission';
import {SubmissionEditPageAttachmentsTab} from './tabs/submission-edit-page-attachments-tab';
import {SubmissionEditPageSummaryTab} from './tabs/submission-edit-page-summary-tab';
import {SubmissionEditPageGeneralTab} from './tabs/submission-edit-page-general-tab';
import {AppToolbarAction} from '../../../components/app-toolbar/app-toolbar-props';
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import {SubmissionEditPagePaymentTab} from './tabs/submission-edit-page-payment-tab';
import {SubmissionStatus} from '../../../data/submission-status';

export function SubmissionEditPage(): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();

    const {
        id,
    } = useParams();

    const [form, setForm] = useState<Application>();
    const [submission, setSubmission] = useState<Submission>();

    const [confirmArchive, setConfirmArchive] = useState<() => void>();
    const [confirmCancelDestination, setConfirmCancelDestination] = useState<() => void>();

    const [isLoading, setIsLoading] = useState(false);
    const [isNotFound, setIsNotFound] = useState(false);

    const [currentTab, setCurrentTab] = useState(0);

    useEffect(() => {
        if (id != null && api != null) {
            setIsLoading(true);
            setIsNotFound(false);

            useSubmissionsApi(api)
                .retrieve(id)
                .then((res) => {
                    setSubmission(res);
                })
                .catch((err) => {
                    console.error(err);
                    setIsNotFound(true);
                })
                .finally(() => {
                    setIsLoading(false);
                });
        }
    }, [id, api]);

    useEffect(() => {
        if (submission == null) {
            return;
        }

        useFormsApi(api)
            .retrieve(submission.formId)
            .then((res) => {
                setForm(res);
            });
    }, [submission, api]);

    const handleArchive = (): void => {
        if (form != null && submission != null) {
            setConfirmArchive(() => () => {
                const archivedSubmission = {
                    ...submission,
                    archived: new Date().toISOString(),
                };

                setIsLoading(true);
                useSubmissionsApi(api)
                    .save(archivedSubmission)
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
                        setIsLoading(false);
                    });
            });
        }
    };

    const handleResendDestination = (): void => {
        if (form != null && submission != null) {
            setIsLoading(true);

            useSubmissionsApi(api)
                .resentDestination(submission.id)
                .then((resentSubmission) => {
                    setSubmission(resentSubmission);
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Erneutes Übertragen des Antrags fehlgeschlagen'));
                })
                .finally(() => {
                    setIsLoading(false);
                });
        }
    };

    const handleCancelDestination = (): void => {
        if (form != null && submission != null) {
            setConfirmCancelDestination(() => () => {

                setIsLoading(true);
                useSubmissionsApi(api)
                    .save({
                        ...submission,
                        destinationId: null,
                        destinationSuccess: null,
                        destinationResult: null,
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
                        setIsLoading(false);
                    });
            });
        }
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
        actions.push({
            icon: <FolderZipOutlinedIcon />,
            tooltip: 'Vorgang abschließen',
            onClick: handleArchive,
        });

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
                    form != null &&
                    form.paymentProvider != null &&
                    isStringNotNullOrEmpty(form.paymentProvider) &&
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
                form.paymentProvider != null &&
                isStringNotNullOrEmpty(form.paymentProvider) &&
                <SubmissionEditPagePaymentTab
                    form={form}
                    submission={submission}
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
        </PageWrapper>
    );
}
