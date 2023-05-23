import React from 'react';
import {Alert, Button, Checkbox, List, ListItem, ListItemText, Tooltip, Typography} from '@mui/material';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faPaperPlane} from '@fortawesome/pro-light-svg-icons';
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

    const checklist: {
        label: string;
        done: boolean;
    }[] = [
        {
            label: 'Schnittstelle eingerichtet',
            done: element.destination != null,
        },

        {
            label: 'Fachlicher Support eingerichtet',
            done: element.legalSupport != null,
        },
        {
            label: 'Technischer Support eingerichtet',
            done: element.legalSupport != null,
        },


        {
            label: 'Impressum eingerichtet',
            done: element.imprint != null,
        },
        {
            label: 'Datenschutzerklärung eingerichtet',
            done: element.imprint != null,
        },
        {
            label: 'Barrierefreiheitserklärung eingerichtet',
            done: element.imprint != null,
        },

        {
            label: 'Zuständige und/oder bewirtschaftende Stelle eingerichtet',
            done: element.introductionStep.responsibleDepartment != null || element.introductionStep.managingDepartment != null,
        },

        {
            label: 'Der Antrag wurde technisch und fachlich geprüft',
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
                Antrag Veröffentlichen
            </Typography>

            <Typography
                variant="body1"
            >
                Bevor ein Antrag veröffentlicht werden kann, müssen die folgenden Punkte erfüllt sein.
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
                <Alert
                    severity='success'
                    sx={{mb: 2}}
                >
                    Antrag Veröffentlicht
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
                        Antrag Veröffentlichen
                    </Button>
                </span>
                </Tooltip>
            }
        </>
    );
}
