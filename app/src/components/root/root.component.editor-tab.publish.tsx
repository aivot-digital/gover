import React, {useState} from 'react';
import {Alert, AlertTitle, Button, Checkbox, Divider, List, ListItem, ListItemText, Tooltip, Typography} from '@mui/material';
import {type BaseEditorProps} from '../../editors/base-editor';
import {type RootElement} from '../../models/elements/root-element';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectMemberships, selectUser} from '../../slices/user-slice';
import {ApplicationStatus} from '../../data/application-status/application-status';
import {isElementTested} from '../../utils/is-element-tested';
import {hasUntestedChild} from '../../utils/has-untested-child';
import {UserRole} from '../../data/user-role';
import {AlertComponent} from '../alert/alert-component';
import {updateAppModel} from '../../slices/app-slice';
import {Application} from '../../models/entities/application';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import PauseCircleOutlineOutlinedIcon from '@mui/icons-material/PauseCircleOutlineOutlined';
import {ApplicationService} from '../../services/application-service';
import {showErrorSnackbar} from '../../slices/snackbar-slice';

export function RootComponentEditorTabPublish(props: BaseEditorProps<RootElement, Application>): JSX.Element {
    const dispatch = useAppDispatch();

    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);

    const [isPublished, setIsPublished] = useState(props.entity.status === ApplicationStatus.Published);
    const [isRevoked, setIsRevoked] = useState(props.entity.status === ApplicationStatus.Revoked);

    const checklist: Array<{
        label: string;
        done: boolean;
    }> = [
        {
            label: 'Fachlicher Support eingerichtet',
            done: props.entity.legalSupportDepartment != null,
        },
        {
            label: 'Technischer Support eingerichtet',
            done: props.entity.technicalSupportDepartment != null,
        },


        {
            label: 'Impressum eingerichtet',
            done: props.entity.imprintDepartment != null,
        },
        {
            label: 'Datenschutzerklärung eingerichtet',
            done: props.entity.privacyDepartment != null,
        },
        {
            label: 'Barrierefreiheitserklärung eingerichtet',
            done: props.entity.accessibilityDepartment != null,
        },

        {
            label: 'Zuständige und/oder bewirtschaftende Stelle eingerichtet',
            done: props.entity.responsibleDepartment != null || props.entity.managingDepartment != null,
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
    const canPublish =
        (user?.admin ?? false) ||
        (memberships ?? [])
            .some((mem) => (
                    mem.department === props.entity.developingDepartment &&
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

            <List
                sx={{
                    my: 2,
                }}
            >
                {
                    checklist.map((item) => (
                        <ListItem
                            key={item.label}
                            secondaryAction={
                                <Checkbox
                                    edge="start"
                                    checked={item.done}
                                />
                            }
                        >
                            <ListItemText
                                primary={item.label}
                            />
                        </ListItem>
                    ))
                }
            </List>

            {
                isPublished &&
                <>
                    <Alert
                        severity="success"
                    >
                        Formular veröffentlicht
                    </Alert>

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
                                <PauseCircleOutlineOutlinedIcon/>
                            }
                            color="warning"
                            onClick={() => {
                                const updatedAppModel = {
                                    ...props.entity,
                                    status: ApplicationStatus.Revoked,
                                };
                                const orignalApplication = {
                                    ...props.entity,
                                };
                                dispatch(updateAppModel(updatedAppModel));
                                ApplicationService
                                    .update(props.entity.id, updatedAppModel)
                                    .catch((err) => {
                                        if (err.status === 403) {
                                            dispatch(showErrorSnackbar('Sie verfügen nicht über die notwendigen Rechte zum Bearbeiten.'));
                                        } else {
                                            console.error(err);
                                            dispatch(showErrorSnackbar('Das Formular konnte nicht gespeichert werden.'));
                                        }
                                        dispatch(updateAppModel(orignalApplication));
                                        setIsPublished(orignalApplication.status === ApplicationStatus.Published);
                                        setIsRevoked(orignalApplication.status === ApplicationStatus.Revoked);
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
                            <SendOutlinedIcon/>
                        }
                        onClick={() => {
                            const updatedAppModel = {
                                ...props.entity,
                                status: ApplicationStatus.Published,
                            };
                            const orignalApplication = {
                                ...props.entity,
                            };
                            dispatch(updateAppModel(updatedAppModel));
                            ApplicationService
                                .update(props.entity.id, updatedAppModel)
                                .catch((err) => {
                                    if (err.status === 403) {
                                        dispatch(showErrorSnackbar('Sie verfügen nicht über die notwendigen Rechte zum Bearbeiten.'));
                                    } else {
                                        console.error(err);
                                        dispatch(showErrorSnackbar('Das Formular konnte nicht gespeichert werden.'));
                                    }
                                    dispatch(updateAppModel(orignalApplication));
                                    setIsPublished(orignalApplication.status === ApplicationStatus.Published);
                                    setIsRevoked(orignalApplication.status === ApplicationStatus.Revoked);
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
