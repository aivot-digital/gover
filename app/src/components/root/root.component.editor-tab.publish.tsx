import React from 'react';
import {
    Alert,
    AlertTitle,
    Button,
    Checkbox,
    Dialog, Divider,
    List,
    ListItem,
    ListItemText,
    Tooltip,
    Typography
} from '@mui/material';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faPaperPlane, faPauseCircle} from '@fortawesome/pro-light-svg-icons';
import {BaseEditorProps} from "../../editors/base-editor";
import {RootElement} from "../../models/elements/root-element";
import {useAppDispatch} from "../../hooks/use-app-dispatch";
import {useAppSelector} from "../../hooks/use-app-selector";
import {selectUser} from "../../slices/user-slice";
import {selectLoadedApplication, updateAppModel} from "../../slices/app-slice";
import {ApplicationStatus} from "../../data/application-status/application-status";
import {isElementTested} from "../../utils/is-element-tested";
import {hasUntestedChild} from "../../utils/has-untested-child";
import {checkUserRole} from "../../utils/check-user-role";
import {UserRole} from "../../data/user-role";

export function RootComponentEditorTabPublish({element}: BaseEditorProps<RootElement>) {
    const dispatch = useAppDispatch();

    const user = useAppSelector(selectUser);
    const application = useAppSelector(selectLoadedApplication);

    const isPublished = application?.status === ApplicationStatus.Published;
    const isRevoked = application?.status === ApplicationStatus.Revoked;

    const checklist: {
        label: string;
        done: boolean;
    }[] = [
        {
            label: 'Fachlicher Support eingerichtet',
            done: application?.legalSupportDepartment != null,
        },
        {
            label: 'Technischer Support eingerichtet',
            done: application?.technicalSupportDepartment != null,
        },


        {
            label: 'Impressum eingerichtet',
            done: application?.imprintDepartment != null,
        },
        {
            label: 'Datenschutzerklärung eingerichtet',
            done: application?.privacyDepartment != null,
        },
        {
            label: 'Barrierefreiheitserklärung eingerichtet',
            done: application?.accessibilityDepartment != null,
        },

        {
            label: 'Zuständige und/oder bewirtschaftende Stelle eingerichtet',
            done: application?.responsibleDepartment != null || application?.managingDepartment != null,
        },

        {
            label: 'Das Formular wurde technisch und fachlich geprüft',
            done: (
                isElementTested(element) &&
                isElementTested(element.introductionStep) &&
                isElementTested(element.summaryStep) &&
                isElementTested(element.submitStep) &&
                !hasUntestedChild(element)
            ),
        },
    ];

    const publishDisabled = isPublished || !checkUserRole(UserRole.Publisher, user) || checklist.some(item => !item.done);

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

            <List sx={{my: 2}}>
                {
                    checklist.map(item => (
                        <ListItem
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
                        severity='success'
                    >
                        Formular Veröffentlicht
                    </Alert>

                    <Divider
                        sx={{my: 8}}
                    >
                        Formular zurückziehen
                    </Divider>

                    <Alert
                        severity='warning'
                        sx={{mb: 2}}
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
                                <FontAwesomeIcon icon={faPauseCircle}/>
                            }
                            color="warning"
                            onClick={() => {
                                if (application != null) {
                                    dispatch(updateAppModel({
                                        ...application,
                                        status: ApplicationStatus.Revoked,
                                    }));
                                }
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
                    severity='warning'
                    sx={{mb: 2}}
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
                <Tooltip
                    title={
                        publishDisabled ?
                            'Vor dem Veröffentlichen müssen alle Kriterien erfüllt sein und Sie müssen die notwendigen Berechtigungen haben' :
                            'Jetzt veröffentlichen'
                    }
                >
                <span>
                    <Button
                        variant="contained"
                        endIcon={
                            <FontAwesomeIcon icon={faPaperPlane}/>
                        }
                        disabled={publishDisabled}
                        onClick={() => {
                            if (application != null) {
                                dispatch(updateAppModel({
                                    ...application,
                                    status: ApplicationStatus.Published,
                                }));
                            }
                        }}
                    >
                        Formular Veröffentlichen
                    </Button>
                </span>
                </Tooltip>
            }
        </>
    );
}
