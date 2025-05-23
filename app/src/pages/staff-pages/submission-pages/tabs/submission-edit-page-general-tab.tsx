import React, {type FormEvent, useEffect, useMemo, useState} from 'react';
import {Form} from '../../../../models/entities/form';
import {Box, Button, IconButton, Typography} from '@mui/material';
import {format, parseISO} from 'date-fns';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {Api} from '../../../../hooks/use-api';
import {SelectFieldComponentOption} from '../../../../components/select-field/select-field-component-option';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DataObjectOutlinedIcon from '@mui/icons-material/DataObjectOutlined';
import {LegacySystemIdpKey} from '../../../../data/legacy-system-idp-key';
import {Destination} from '../../../../modules/destination/models/destination';
import ReplyOutlinedIcon from '@mui/icons-material/ReplyOutlined';
import {isStringNotNullOrEmpty} from '../../../../utils/string-utils';
import AssignmentReturnedOutlinedIcon from '@mui/icons-material/AssignmentReturnedOutlined';
import AssignmentTurnedInOutlinedIcon from '@mui/icons-material/AssignmentTurnedInOutlined';
import AssignmentIndOutlinedIcon from '@mui/icons-material/AssignmentIndOutlined';
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import {StatusTable} from '../../../../components/status-table/status-table';
import SubdirectoryArrowRightOutlinedIcon from '@mui/icons-material/SubdirectoryArrowRightOutlined';
import {XBezahldienstePaymentStatus, XBezahldienstePaymentStatusLabels} from '../../../../data/xbezahldienste-payment-status';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import {Link} from 'react-router-dom';
import LaunchOutlinedIcon from '@mui/icons-material/LaunchOutlined';
import {SubmissionStatus} from '../../../../modules/submissions/enums/submission-status';
import HourglassEmptyOutlinedIcon from '@mui/icons-material/HourglassEmptyOutlined';
import SentimentNeutralOutlinedIcon from '@mui/icons-material/SentimentNeutralOutlined';
import MoodBadOutlinedIcon from '@mui/icons-material/MoodBadOutlined';
import SentimentDissatisfiedOutlinedIcon from '@mui/icons-material/SentimentDissatisfiedOutlined';
import SentimentSatisfiedAltOutlinedIcon from '@mui/icons-material/SentimentSatisfiedAltOutlined';
import EmojiEmotionsOutlinedIcon from '@mui/icons-material/EmojiEmotionsOutlined';
import {PaymentTransactionResponseDTO} from '../../../../modules/payment/dtos/payment-transaction-response-dto';
import {DepartmentMembershipsApiService} from '../../../../modules/departments/department-memberships-api-service';
import {DestinationsApiService} from '../../../../modules/destination/destinations-api-service';
import {SubmissionDetailsResponseDTO} from '../../../../modules/submissions/dtos/submission-details-response-dto';
import {SubmissionsApiService} from '../../../../modules/submissions/submissions-api-service';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {FlagOutlined} from '@mui/icons-material';
import {determineLabel} from '../../../../utils/submission-state';
import {resolveUserName} from '../../../../modules/users/utils/resolve-user-name';
import {departmentMembershipResponseDTOasUser} from '../../../../modules/departments/dtos/department-membership-response-dto';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {IdentityValue} from '../../../../modules/identity/models/identity-value';
import {IdentityCustomerInputKey} from '../../../../modules/identity/constants/identity-customer-input-key';
import {IdentitySummary} from '../../../../modules/identity/components/identity-summary/identity-summary';

interface SubmissionEditPageGeneralTabProps {
    api: Api;
    form: Form;
    submission: SubmissionDetailsResponseDTO;
    transaction?: PaymentTransactionResponseDTO | null;
    onChangeSubmission: (submission: SubmissionDetailsResponseDTO) => void;
}

function createGeneralRows(form: Form, submission: SubmissionDetailsResponseDTO, transaction?: PaymentTransactionResponseDTO | null) {
    const created = parseISO(submission.created);

    const ratingInformation: Record<string, {
        icon: React.ReactElement;
        label: string;
    }> = {
        1: {
            icon: <MoodBadOutlinedIcon />,
            label: 'Sehr Unzufrieden',
        },
        2: {
            icon: <SentimentDissatisfiedOutlinedIcon />,
            label: 'Unzufrieden',
        },
        3: {
            icon: <SentimentNeutralOutlinedIcon />,
            label: 'Neutral',
        },
        4: {
            icon: <SentimentSatisfiedAltOutlinedIcon />,
            label: 'Zufrieden',
        },
        5: {
            icon: <EmojiEmotionsOutlinedIcon />,
            label: 'Sehr Zufrieden',
        },
    };

    const rows = [
        {
            icon: <DescriptionOutlinedIcon />,
            label: 'Formular',
            children: (
                <Box
                    display="flex"
                    alignItems="center"
                >
                    <span>{form.title} - Version {form.version}</span>

                    <IconButton
                        component={Link}
                        to={`/forms/${form.id}`}
                        target="_blank"
                        color="primary"
                        size="small"
                        sx={{ml: 1}}
                    >
                        <LaunchOutlinedIcon
                            fontSize="small"
                            color="primary"
                        />
                    </IconButton>
                </Box>
            ),
        },
        {
            icon: <AssignmentReturnedOutlinedIcon />,
            label: 'Eingang',
            children: (created != null) ? `${format(created, 'dd.MM.yyyy')} ${format(created, 'HH:mm')} Uhr` : '',
        },
        {
            icon: <FlagOutlined />,
            label: 'Status des Antrages',
            children: determineLabel({
                // Create a new empty object here because the determine label function expects a SubmissionListResponseDTO and only requires the status property
                assigneeId: '',
                created: '',
                destinationId: 0,
                destinationSuccess: false,
                fileNumber: '',
                formId: 0,
                id: '',
                isTestSubmission: false,
                status: submission.status,
            }),
        },
        {
            icon: (
                submission.reviewScore != null ?
                    ratingInformation[submission.reviewScore].icon :
                    <SentimentNeutralOutlinedIcon />
            ),
            label: 'Bewertung',
            children: (
                submission.reviewScore != null ?
                    ratingInformation[submission.reviewScore].label :
                    'Keine Bewertung abgegeben'
            ),
        },
    ];

    if (submission.isTestSubmission) {
        rows.push({
            icon: <ScienceOutlinedIcon />,
            label: 'Test-Antrag',
            children: 'Es handelt sich um einen Test-Antrag. Der Antrag wurde von einem unveröffentlichten Formular oder aus dem Formulareditor heraus abgesendet.',
        });
    }

    if (
        transaction != null &&
        transaction.paymentInformation != null &&
        transaction.paymentInformation.status != null
    ) {
        rows.push({
            icon: (
                transaction.paymentInformation.status === XBezahldienstePaymentStatus.Payed ?
                    <CheckCircleOutlineOutlinedIcon color="success" /> :
                    <CancelOutlinedIcon color="error" />
            ),
            label: 'Zahlungsstatus',
            children: XBezahldienstePaymentStatusLabels[transaction.paymentInformation.status],
        });
    }

    return rows;
}

function createDestinationRows(submission: SubmissionDetailsResponseDTO, destination: Destination) {
    const rows = [
        {
            icon: <DataObjectOutlinedIcon />,
            label: 'Schnittstelle',
            children: destination.name,
        },
    ];

    if (submission.status === SubmissionStatus.Pending) {
        rows.push({
            icon: <HourglassEmptyOutlinedIcon />,
            label: 'Status',
            children: 'Wartet auf Zahlung',
        });

        return rows;
    }

    if (submission.status === SubmissionStatus.HasPaymentError) {
        rows.push({
            icon: <CancelOutlinedIcon color="error" />,
            label: 'Status',
            children: 'Fehler bei Zahlung',
        });

        return rows;
    }

    rows.push(
        {
            icon: (
                submission.destinationSuccess ?
                    <CheckCircleOutlineOutlinedIcon color="success" /> :
                    <CancelOutlinedIcon color="error" />
            ),
            label: 'Status',
            children: `Übertragung an Schnittstelle ${submission.destinationSuccess ? 'erfolgreich' : 'fehlgeschlagen'} ${
                (!submission.destinationSuccess && submission.archived != null) ? '(Vorgang in Gover manuell abgeschlossen)' : ''}`,
        },
    );

    if (submission.destinationResult != null &&
        isStringNotNullOrEmpty(submission.destinationResult)) {
        rows.push({
            icon: (
                submission.destinationSuccess ?
                    <ReplyOutlinedIcon color="success" /> :
                    <ReplyOutlinedIcon color="error" />
            ),
            label: submission.destinationSuccess ? 'Erfolgsmeldung' : 'Fehlermeldung',
            children: submission.destinationResult,
        });
    }

    if (submission.destinationSuccess) {
        rows.push({
            icon: <AssignmentOutlinedIcon />,
            label: 'Aktenzeichen',
            children: isStringNotNullOrEmpty(submission.fileNumber) ? submission.fileNumber! : 'Schnittstelle hat kein Aktenzeichen vergeben',
        });
    }

    const archived = submission.archived != null ? parseISO(submission.archived) : undefined;
    if (archived != null) {
        rows.push({
            icon: <AssignmentTurnedInOutlinedIcon />,
            label: submission.destinationSuccess ? 'Übertragen' : 'Manuell Abgeschlossen',
            children: `${format(archived, 'dd.MM.yyyy')} ${format(archived, 'HH:mm')} Uhr`,
        });
    }

    return rows;
}

function createWorkedOnRows(submission: SubmissionDetailsResponseDTO, archived: Date, userOptions: SelectFieldComponentOption[]) {
    return [
        {
            icon: <AssignmentOutlinedIcon />,
            label: 'Aktenzeichen',
            children: isStringNotNullOrEmpty(submission.fileNumber) ? submission.fileNumber : 'Kein Aktenzeichen vergeben',
        },
        {
            icon: <AssignmentIndOutlinedIcon />,
            label: 'Zuständige Mitarbeiter:in',
            children: submission.assigneeId != null ? userOptions.find((opt) => opt.value === submission.assigneeId)?.label : 'Keine Mitarbeiter:in zugewiesen',
        },
        {
            icon: <AssignmentTurnedInOutlinedIcon />,
            label: 'Abgeschlossen',
            children: `${format(archived, 'dd.MM.yyyy')} ${format(archived, 'HH:mm')} Uhr`,
        },
    ];
}


export function SubmissionEditPageGeneralTab(props: SubmissionEditPageGeneralTabProps) {
    const {
        submission: originalSubmission,
    } = props;

    const dispatch = useAppDispatch();
    const [isLoading, setIsLoading] = useState(false);
    const [userOptions, setUserOptions] = useState<SelectFieldComponentOption[]>([]);
    const [editedSubmission, setEditedSubmission] = useState<SubmissionDetailsResponseDTO>();
    const [destination, setDestination] = useState<Destination>();

    const submission = useMemo(() => {
        return editedSubmission ?? originalSubmission;
    }, [editedSubmission, originalSubmission]);

    const {hasChanged, dialog: changeBlockerDialog} = useChangeBlocker(originalSubmission, editedSubmission ?? originalSubmission);

    useEffect(() => {
        fetchAssigneeOptions(props)
            .then(setUserOptions)
            .catch((error) => {
                console.error(error);
                dispatch(showErrorSnackbar('Mitarbeiter:innen konnten nicht geladen werden'));
            });
    }, []);

    useEffect(() => {
        if (props.submission.destinationId != null) {
            new DestinationsApiService(props.api)
                .retrieve(props.submission.destinationId)
                .then((destination) => {
                    setDestination(destination);
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Ziel konnte nicht geladen werden'));
                });
        }
    }, []);

    const handleSubmit = (event: FormEvent): void => {
        event.preventDefault();

        if (props.form != null && editedSubmission != null) {
            setIsLoading(true);

            new SubmissionsApiService(props.api)
                .update(editedSubmission.id, {
                    fileNumber: editedSubmission.fileNumber,
                    assigneeId: editedSubmission.assigneeId,
                })
                .then((savedSubmission) => {
                    dispatch(showSuccessSnackbar('Antrag erfolgreich gespeichert'));
                    props.onChangeSubmission(savedSubmission);
                    setEditedSubmission(savedSubmission);
                })
                .catch((err) => {
                    if (err.response?.status === 409) {
                        dispatch(showErrorSnackbar('Der Vorgang wurde bereits archiviert'));
                    } else {
                        console.error(err);
                        dispatch(showErrorSnackbar('Antrag konnte nicht gespeichert werden'));
                    }
                })
                .finally(() => {
                    setIsLoading(false);
                });
        }
    };

    const archived = props.submission.archived != null ? parseISO(props.submission.archived) : undefined;

    return (
        <>
            <StatusTable
                label="Antragsinformationen"
                items={createGeneralRows(props.form, props.submission)}
            />

            <IdentitySummary
                submission={props.submission}
            />

            {
                props.submission.destinationId == null &&
                <>
                    {
                        archived == null &&
                        <form onSubmit={handleSubmit}>
                            <Typography
                                variant="h6"
                                sx={{
                                    mt: 4,
                                    mb: 2,
                                }}
                            >
                                Bearbeitungsinformationen
                            </Typography>

                            <TextFieldComponent
                                label="Aktenzeichen"
                                value={submission.fileNumber ?? undefined}
                                onChange={(val) => {
                                    setEditedSubmission({
                                        ...submission,
                                        fileNumber: val,
                                    });
                                }}
                            />

                            <SelectFieldComponent
                                label="Zuständige Mitarbeiter:in"
                                value={submission.assigneeId ?? undefined}
                                onChange={(val) => {
                                    setEditedSubmission({
                                        ...submission,
                                        assigneeId: val,
                                    });
                                }}
                                options={userOptions}
                            />

                            <Box sx={{mt: 2}}>
                                <Button
                                    type="submit"
                                    disabled={!hasChanged || isLoading}
                                    variant="contained"
                                    color="primary"
                                    startIcon={<SaveOutlinedIcon />}
                                >
                                    Speichern
                                </Button>

                                <Button
                                    type="reset"
                                    color="error"
                                    disabled={!hasChanged || isLoading}
                                    onClick={() => {
                                        setEditedSubmission(undefined);
                                    }}
                                    sx={{ml: 2}}
                                >
                                    Zurücksetzen
                                </Button>
                            </Box>
                        </form>
                    }

                    {
                        archived != null &&
                        userOptions != null &&
                        <StatusTable
                            label="Bearbeitungsinformationen"
                            items={createWorkedOnRows(props.submission, archived, userOptions)}
                        />
                    }
                </>
            }

            {
                props.submission.destinationId !== null &&
                destination != null &&
                <StatusTable
                    label="Schnittstelleninformationen"
                    items={createDestinationRows(props.submission, destination)}
                />
            }

            {changeBlockerDialog}
        </>
    );
}


async function fetchAssigneeOptions(props: SubmissionEditPageGeneralTabProps): Promise<SelectFieldComponentOption[]> {
    const departmentIds = [];
    if (props.form.managingDepartmentId != null) {
        departmentIds.push(props.form.managingDepartmentId);
    }

    if (props.form.responsibleDepartmentId != null) {
        departmentIds.push(props.form.responsibleDepartmentId);
    }

    if (props.form.managingDepartmentId == null && props.form.responsibleDepartmentId == null) {
        departmentIds.push(props.form.developingDepartmentId);
    }

    const apiService = new DepartmentMembershipsApiService(props.api);

    const allOptions: SelectFieldComponentOption[] = (await apiService
        .listAll({
            userEnabled: true,
            userDeletedInIdp: false,
            departmentIds: departmentIds,
        }))
        .content
        .map(option => ({
            value: option.userId,
            label: resolveUserName(departmentMembershipResponseDTOasUser(option)),
            subLabel: option.userEmail,
        }));

    const hasSeparateAssignee = (
        props.submission.assigneeId != null &&
        isStringNotNullOrEmpty(props.submission.assigneeId) &&
        allOptions.every(opt => opt.value !== props.submission.assigneeId)
    );
    if (hasSeparateAssignee) {
        const assigneeOption = await apiService
            .listAll({
                userId: props.submission.assigneeId!,
            });
        if (assigneeOption.content.length > 0) {
            allOptions.push({
                value: assigneeOption.content[0].userId,
                label: resolveUserName(departmentMembershipResponseDTOasUser(assigneeOption.content[0])),
                subLabel: assigneeOption.content[0].userEmail,
            });
        }
    }

    return allOptions;
}