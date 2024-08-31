import {type ElementEditorContentProps} from '../element-editor-content/element-editor-content-props';
import {Alert, AlertTitle, Box, Button, Divider, Tooltip, Typography} from '@mui/material';
import React, {useState} from 'react';
import {Form as Application} from '../../models/entities/form';
import {type RootElement} from '../../models/elements/root-element';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectMemberships, selectUser} from '../../slices/user-slice';
import {ApplicationStatus} from '../../data/application-status';
import {type ChecklistItem} from '../checklist/checklist-props';
import {isElementTested} from '../../utils/is-element-tested';
import {hasUntestedChild} from '../../utils/has-untested-child';
import {UserRole} from '../../data/user-role';
import {Checklist} from '../checklist/checklist';
import PauseCircleOutlineOutlinedIcon from '@mui/icons-material/PauseCircleOutlineOutlined';
import {updateLoadedForm} from '../../slices/app-slice';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {AlertComponent} from '../alert/alert-component';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import {useApi} from '../../hooks/use-api';
import {useFormsApi} from '../../hooks/use-forms-api';

export function ApplicationInternalPublishTab<T extends RootElement, E extends Application>(props: ElementEditorContentProps<T, E>): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();

    const memberships = useAppSelector(selectMemberships);

    const [isPublished, setIsPublished] = useState(props.entity.status === ApplicationStatus.Published);
    const [isRevoked, setIsRevoked] = useState(props.entity.status === ApplicationStatus.Revoked);

    const checklist: ChecklistItem[] = [
        {
            label: 'Öffentlicher Titel & Überschrift hinterlegt',
            done: props.element.headline != null,
        },


        {
            label: 'Fachlicher Support eingerichtet',
            done: props.entity.legalSupportDepartmentId != null,
        },
        {
            label: 'Technischer Support eingerichtet',
            done: props.entity.technicalSupportDepartmentId != null,
        },


        {
            label: 'Impressum eingerichtet',
            done: props.entity.imprintDepartmentId != null,
        },
        {
            label: 'Datenschutzerklärung eingerichtet',
            done: props.entity.privacyDepartmentId != null,
        },
        {
            label: 'Barrierefreiheitserklärung eingerichtet',
            done: props.entity.accessibilityDepartmentId != null,
        },

        {
            label: 'Zuständige und/oder bewirtschaftende Stelle eingerichtet',
            done: props.entity.responsibleDepartmentId != null || props.entity.managingDepartmentId != null,
        },

        {
            label: 'Lösch- und Zugriffsfristen konfiguriert',
            done: (
                props.entity.submissionDeletionWeeks != null &&
                props.entity.submissionDeletionWeeks >= 0 &&
                props.entity.customerAccessHours != null &&
                props.entity.customerAccessHours > 0
            ),
        },

        {
            label: 'Das Formular wurde technisch und fachlich geprüft',
            done: (
                isElementTested(props.element) &&
                isElementTested(props.element.introductionStep) &&
                isElementTested(props.element.summaryStep) &&
                isElementTested(props.element.submitStep) &&
                !hasUntestedChild(props.element)
            ),
        },
    ];

    const allChecksDone = checklist.every((item) => item.done);
    const canPublish = (memberships ?? [])
        .some((mem) => (
                mem.departmentId === props.entity.developingDepartmentId &&
                (mem.role === UserRole.Admin || mem.role === UserRole.Publisher)
            ),
        );

    return (
        <>
            <Typography
                variant="h6"
            >
                Formular Veröffentlichen
            </Typography>

            <Typography
                variant="body1"
            >
                Bevor ein Formular veröffentlicht werden kann, müssen die folgenden Punkte erfüllt sein.
            </Typography>

            <Box
                sx={{
                    my: 2,
                }}
            >
                <Checklist
                    items={checklist}
                />
            </Box>

            {
                isPublished &&
                <>
                    <Alert
                        severity="success"
                    >
                        Formular veröffentlicht
                    </Alert>
                </>
            }

            {
                isPublished &&
                canPublish &&
                <>
                    <Divider
                        sx={{my: 8}}
                    >
                        Formular zurückziehen
                    </Divider>

                    <Alert
                        severity="warning"
                        sx={{mb: 2}}
                    >
                        <AlertTitle>
                            Formular zurückziehen
                        </AlertTitle>

                        Sie können ein veröffentlichtes Formular jederzeit zurückziehen.
                        Dieses steht dann nicht mehr öffentlich zur Verfügung.
                        Sie können zurückgezogene Formulare jederzeit wieder veröffentlichen.
                    </Alert>

                    <Tooltip
                        title="Jetzt zurückziehen"
                    >
                        <Button
                            variant="outlined"
                            endIcon={
                                <PauseCircleOutlineOutlinedIcon />
                            }
                            color="warning"
                            onClick={() => {
                                const updatedAppModel = {
                                    ...props.entity,
                                    status: ApplicationStatus.Revoked,
                                    root: {
                                        ...props.element,
                                    },
                                };
                                const originalApplication = {
                                    ...props.entity,
                                };
                                dispatch(updateLoadedForm(updatedAppModel));
                                useFormsApi(api)
                                    .save(updatedAppModel)
                                    .catch((err) => {
                                        if (err.status === 403) {
                                            dispatch(showErrorSnackbar('Sie verfügen nicht über die notwendigen Rechte zum Bearbeiten.'));
                                        } else {
                                            console.error(err);
                                            dispatch(showErrorSnackbar('Das Formular konnte nicht gespeichert werden.'));
                                        }
                                        dispatch(updateLoadedForm(originalApplication));
                                        setIsPublished(originalApplication.status === ApplicationStatus.Published);
                                        setIsRevoked(originalApplication.status === ApplicationStatus.Revoked);
                                    });
                                setIsRevoked(true);
                                setIsPublished(false);
                            }}
                        >
                            Formular Zurückziehen
                        </Button>
                    </Tooltip>
                </>
            }

            {
                isRevoked &&
                <Alert
                    severity="warning"
                    sx={{
                        mb: 2,
                    }}
                >
                    <AlertTitle>
                        Formular zurückgezogen
                    </AlertTitle>

                    Sie haben das Formular zurückgezogen.
                    Sie können dieses Formular jederzeit wieder veröffentlichen.
                </Alert>
            }

            {
                !isPublished &&
                !allChecksDone &&
                <AlertComponent color="info">
                    Vor dem Veröffentlichen müssen alle Kriterien erfüllt sein und Sie müssen die notwendigen
                    Berechtigungen haben.
                </AlertComponent>
            }

            {
                !isPublished &&
                allChecksDone &&
                !canPublish &&
                <AlertComponent color="info">
                    Sie verfügen nicht über die notwendigen Berechtigungen, um ein Formular in diesem Fachbereich zu
                    veröffentlichen.
                </AlertComponent>
            }

            {
                !isPublished &&
                allChecksDone &&
                canPublish &&
                <Tooltip
                    title="Jetzt veröffentlichen"
                >
                    <Button
                        variant="contained"
                        endIcon={
                            <SendOutlinedIcon />
                        }
                        onClick={() => {
                            const updatedAppModel = {
                                ...props.entity,
                                status: ApplicationStatus.Published,
                                root: {
                                    ...props.element,
                                },
                            };
                            const originalApplication = {
                                ...props.entity,
                            };

                            dispatch(updateLoadedForm(updatedAppModel));
                            useFormsApi(api)
                                .save(updatedAppModel)
                                .catch((err) => {
                                    if (err.status === 403) {
                                        dispatch(showErrorSnackbar('Sie verfügen nicht über die notwendigen Rechte zum Bearbeiten.'));
                                    }if (err.status === 406) {
                                        dispatch(showErrorSnackbar('Das Formular kann nicht veröffentlicht werden, da es ein global deaktiviertes Nutzer- oder Unternehmenskonto verwendet.'));
                                    } else {
                                        console.error(err);
                                        dispatch(showErrorSnackbar('Das Formular konnte nicht gespeichert werden.'));
                                    }
                                    dispatch(updateLoadedForm(originalApplication));
                                    setIsPublished(originalApplication.status === ApplicationStatus.Published);
                                    setIsRevoked(originalApplication.status === ApplicationStatus.Revoked);
                                });
                            setIsPublished(true);
                            setIsRevoked(false);
                        }}
                    >
                        Formular Veröffentlichen
                    </Button>
                </Tooltip>
            }
        </>
    );
}
