import React from 'react';
import {Alert, Button, Checkbox, List, ListItem, ListItemText, Tooltip, Typography} from '@mui/material';
import {RootElement} from '../../../models/elements/root-element';
import {BaseEditorProps} from '../../_lib/base-editor-props';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faPaperPlane} from '@fortawesome/pro-light-svg-icons';
import {isNullOrEmpty} from '../../../utils/is-null-or-empty';
import {ApplicationStatus} from '../../../data/application-status/application-status';
import {hasUntestedChild} from '../../../utils/has-untested-child';
import {isElementTested} from '../../../utils/is-element-tested';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectUser} from '../../../slices/user-slice';
import {UserRole} from '../../../data/user-role';
import {checkUserRole} from '../../../utils/check-user-role';

export function RootComponentEditorTabPublish({component, onPatch}: BaseEditorProps<RootElement>) {
    const user = useAppSelector(selectUser);
    const published = component.status === ApplicationStatus.Published;

    const checklist: {
        label: string;
        done: boolean;
    }[] = [
        {
            label: 'Schnittstelle eingerichtet',
            done: !isNullOrEmpty(component.interface),
        },

        {
            label: 'Fachlicher Support eingerichtet',
            done: component.legalSupport != null && (typeof component.legalSupport !== 'string' || !isNullOrEmpty(component.legalSupport)),
        },
        {
            label: 'Technischer Support eingerichtet',
            done: component.legalSupport != null && (typeof component.technicalSupport !== 'string' || !isNullOrEmpty(component.technicalSupport)),
        },


        {
            label: 'Impressum eingerichtet',
            done: component.imprint != null && (typeof component.imprint !== 'string' || !isNullOrEmpty(component.imprint)),
        },
        {
            label: 'Datenschutzerkl√§rung eingerichtet',
            done: component.imprint != null && (typeof component.privacy !== 'string' || !isNullOrEmpty(component.privacy)),
        },
        {
            label: 'Barrierefreiheitserkl√§rung eingerichtet',
            done: component.imprint != null && (typeof component.accessibility !== 'string' || !isNullOrEmpty(component.accessibility)),
        },

        {
            label: 'Zust√§ndige und/oder bewirtschaftende Stelle eingerichtet',
            done: (
                component.introductionStep.responsibleDepartment != null && (typeof component.introductionStep.responsibleDepartment !== 'string' || !isNullOrEmpty(component.introductionStep.responsibleDepartment))
            ) || (
                component.introductionStep.managingDepartment != null && (typeof component.introductionStep.managingDepartment !== 'string' || !isNullOrEmpty(component.introductionStep.managingDepartment))
            ),
        },

        {
            label: 'Der Antrag wurde technisch und fachlich gepr√ľft',
            done: (
                isElementTested(component) &&
                isElementTested(component.introductionStep) &&
                isElementTested(component.summaryStep) &&
                isElementTested(component.submitStep) &&
                !hasUntestedChild(component)
            ),
        },
    ];

    const publishDisabled = published || !checkUserRole(UserRole.Publisher, user) || checklist.some(item => !item.done);

    return (
        <>
            <Typography
                variant="h6"
            >
                Antrag Ver√∂ffentlichen
            </Typography>

            <Typography
                variant="body1"
            >
                Bevor ein Antrag ver√∂ffentlicht werden kann, m√ľssen die folgenden Punkte erf√ľllt sein.
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
                published &&
                <Alert
                    severity='success'
                    sx={{mb: 2}}
                >
                    Antrag Ver√∂ffentlicht
                </Alert>
            }

            {
                !published &&
                <Tooltip
                    title={
                        publishDisabled ?
                            'Vor dem Ver√∂ffentlichen m√ľssen alle Kriterien erf√ľllt sein und Sie m√ľssen die notwendigen Berechtigungen haben' :
                            'Jetzt ver√∂ffentlichen'
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
                            onPatch({
                                status: ApplicationStatus.Published,
                            });
                        }}
                    >
                        Antrag Ver√∂ffentlichen
                    </Button>
                </span>
                </Tooltip>
            }
        </>
    );
}
