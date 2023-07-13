import React from 'react';
import {
    Alert,
    AlertTitle,
    Button,
    Checkbox,
    Divider,
    List,
    ListItem,
    ListItemText,
    Tooltip,
    Typography,
} from '@mui/material';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPaperPlane, faPauseCircle } from '@fortawesome/pro-light-svg-icons';
import { type BaseEditorProps } from '../../editors/base-editor';
import { type RootElement } from '../../models/elements/root-element';
import { useAppDispatch } from '../../hooks/use-app-dispatch';
import { useAppSelector } from '../../hooks/use-app-selector';
import { selectMemberships, selectUser } from '../../slices/user-slice';
import { ApplicationStatus } from '../../data/application-status/application-status';
import { isElementTested } from '../../utils/is-element-tested';
import { hasUntestedChild } from '../../utils/has-untested-child';
import { UserRole } from '../../data/user-role';
import { AlertComponent } from '../alert/alert-component';
import { updateAppModel } from '../../slices/app-slice';

export function RootComponentEditorTabPublish(props: BaseEditorProps<RootElement>): JSX.Element {
    const dispatch = useAppDispatch();

    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);

    const isPublished = props.application.status === ApplicationStatus.Published;
    const isRevoked = props.application.status === ApplicationStatus.Revoked;

    const checklist: Array<{
        label: string;
        done: boolean;
    }> = [
        {
            label: 'Fachlicher Support eingerichtet',
            done: props.application.legalSupportDepartment != null,
        },
        {
            label: 'Technischer Support eingerichtet',
            done: props.application.technicalSupportDepartment != null,
        },


        {
            label: 'Impressum eingerichtet',
            done: props.application.imprintDepartment != null,
        },
        {
            label: 'Datenschutzerklärung eingerichtet',
            done: props.application.privacyDepartment != null,
        },
        {
            label: 'Barrierefreiheitserklärung eingerichtet',
            done: props.application.accessibilityDepartment != null,
        },

        {
            label: 'Zuständige und/oder bewirtschaftende Stelle eingerichtet',
            done: props.application.responsibleDepartment != null || props.application.managingDepartment != null,
        },

        {
            label: 'Lösch- und Zugriffsfristen konfiguriert',
            done: (
                props.application.submissionDeletionWeeks != null &&
                props.application.submissionDeletionWeeks >= 0 &&
                props.application.customerAccessHours != null &&
                props.application.customerAccessHours > 0
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
                    mem.department === props.application.developingDepartment &&
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
                sx={ {
                    my: 2,
                } }
            >
                {
                    checklist.map((item) => (
                        <ListItem
                            secondaryAction={
                                <Checkbox
                                    edge="start"
                                    checked={ item.done }
                                />
                            }
                        >
                            <ListItemText
                                primary={ item.label }
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
                        Formular Veröffentlicht
                    </Alert>

                    <Divider
                        sx={ {my: 8} }
                    >
                        Formular zurückziehen
                    </Divider>

                    <Alert
                        severity="warning"
                        sx={ {mb: 2} }
                    >
                        <AlertTitle>
                            Formular zurückziehen
                        </AlertTitle>

                        Sie können einen veröffentlichtes Formular jederzeit zurückziehen.
                        Dieser wird dann nicht mehr ausgespielt.
                        Sie können zurückgezogene Formulare jederzeit wieder veröffentlichen.
                    </Alert>

                    <Tooltip
                        title="Jetzt zurückziehen"
                    >
                        <Button
                            variant="outlined"
                            endIcon={
                                <FontAwesomeIcon icon={ faPauseCircle }/>
                            }
                            color="warning"
                            onClick={ () => {
                                dispatch(updateAppModel({
                                    ...props.application,
                                    status: ApplicationStatus.Revoked,
                                }));
                            } }
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
                    sx={ {
                        mb: 2,
                    } }
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
                            <FontAwesomeIcon icon={ faPaperPlane }/>
                        }
                        onClick={ () => {
                            dispatch(updateAppModel({
                                ...props.application,
                                status: ApplicationStatus.Published,
                            }));
                        } }
                    >
                        Formular Veröffentlichen
                    </Button>
                </Tooltip>
            }
        </>
    );
}
