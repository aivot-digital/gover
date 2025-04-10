import {Box, Button, Checkbox, FormControlLabel, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {useDispatch, useSelector} from 'react-redux';
import {format, parseISO} from 'date-fns';
import {getFullName, type User} from '../../models/entities/user';
import {getFunctionStatus} from '../../utils/function-status-utils';
import {selectUser} from '../../slices/user-slice';
import {type TestTabProps} from './test-tab-props';
import {type AnyElement} from '../../models/elements/any-element';
import {type TestProtocol as TestProtocolModel} from '../../models/lib/test-protocol';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {useApi} from '../../hooks/use-api';
import {useUsersApi} from '../../hooks/use-users-api';
import {isAnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {hasUntestedChild} from '../../utils/has-untested-child';
import {showSuccessSnackbar} from '../../slices/snackbar-slice';
import {isRootElement} from '../../models/elements/root-element';
import {UsersApiService} from '../../modules/users/users-api-service';

export function TestTab<T extends AnyElement>(props: TestTabProps<T>): JSX.Element {
    const dispatch = useDispatch();
    const user = useSelector(selectUser);

    return (
        <Box sx={{p: 4}}>
            <Box>
                <Typography
                    variant="h6"
                >
                    Fachliche Prüfung
                </Typography>

                <Typography
                    variant="body1"
                    sx={{
                        mt: 1,
                        mb: 1,
                    }}
                >
                    Eine Überprüfung der Umsetzung von vorab definierten fachlichen Anforderungen.
                </Typography>

                <CheckboxFieldComponent
                    value={props.elementModel.testProtocolSet?.professionalTest != null}
                    onChange={(checked) => {
                        props.onPatch({
                            ...props.elementModel,
                            testProtocolSet: {
                                ...props.elementModel.testProtocolSet,
                                professionalTest: user != null && checked ?
                                    {
                                        userId: user.id,
                                        timestamp: new Date().toISOString(),
                                    } :
                                    undefined,
                            },
                        });
                    }}
                    label={
                        props.elementModel.testProtocolSet?.professionalTest != null ?
                            'Fachliche Prüfung erfolgreich' :
                            'Ich habe die Fachliche Prüfung erfolgreich durchgeführt'
                    }
                    disabled={!props.editable}
                />

                {
                    props.elementModel.testProtocolSet?.professionalTest != null &&
                    <TestProtocol {...props.elementModel.testProtocolSet.professionalTest} />
                }

                {
                    props.elementModel.testProtocolSet?.professionalTest != null &&
                    isAnyElementWithChildren(props.elementModel) &&
                    <Box>
                        <Button
                            variant="outlined"
                            sx={{
                                mt: 2,
                            }}
                            onClick={() => {
                                if (user != null && isAnyElementWithChildren(props.elementModel)) {
                                    props.onPatch(markAsTestedRecursively<typeof props.elementModel>(user, props.elementModel));
                                    dispatch(showSuccessSnackbar('Alle Kindelemente wurden als geprüft markiert.'));
                                }
                            }}
                            disabled={!hasUntestedChild(props.elementModel)}
                        >
                            {
                                isRootElement(props.elementModel) ?
                                    'Alle Elemente des Formulars als geprüft markieren' :
                                    'Alle Kindelemente ebenfalls als geprüft markieren'
                            }
                        </Button>
                    </Box>
                }
            </Box>


            {
                getFunctionStatus(props.elementModel).length > 0 &&
                <Box>
                    <Typography
                        variant="h6"
                        sx={{mt: 4}}
                    >
                        Technische Prüfung
                    </Typography>
                    <Typography
                        variant="body1"
                        sx={{
                            mt: 1,
                            mb: 1,
                        }}
                    >
                        Eine Überprüfung der technischen Implementierung von individuellen Funktionen.
                    </Typography>
                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={props.elementModel.testProtocolSet?.technicalTest != null}
                                onChange={(event) => {
                                    props.onPatch({
                                        ...props.elementModel,
                                        testProtocolSet: {
                                            ...props.elementModel.testProtocolSet,
                                            technicalTest: user != null && event.target.checked ?
                                                {
                                                    userId: user.id,
                                                    timestamp: new Date().toISOString(),
                                                } :
                                                undefined,
                                        },
                                    });
                                }}
                            />
                        }
                        label={((props.elementModel.testProtocolSet?.technicalTest) != null) ? 'Technische Prüfung erfolgreich' : 'Ich habe die Technische Prüfung erfolgreich durchgeführt'}
                        disabled={!props.editable}
                    />
                    {
                        props.elementModel.testProtocolSet?.technicalTest != null &&
                        <TestProtocol {...props.elementModel.testProtocolSet.technicalTest} />
                    }
                </Box>
            }
        </Box>
    );
}

function TestProtocol(protocol: TestProtocolModel): JSX.Element | null {
    const api = useApi();
    const [user, setUser] = useState<User>();

    useEffect(() => {
        new UsersApiService(api)
            .retrieve(protocol.userId)
            .then(setUser)
            .catch((err) => {
                console.error(err);
            });
    }, [protocol.userId]);

    if (user == null) {
        return null;
    }

    const timestamp = parseISO(protocol.timestamp);

    return (
        <Typography>
            Geprüft von <Typography
            component="span"
            color="primary"
        >{getFullName(user)}</Typography> am {format(timestamp, 'dd.MM.yyyy')} um {format(timestamp, 'HH:mm')} Uhr
        </Typography>
    );
}


function markAsTestedRecursively<T extends AnyElement>(user: User, element: T): T {
    const updatedElement: T = {
        ...element,
        testProtocolSet: {
            ...element.testProtocolSet,
            professionalTest: {
                userId: user.id,
                timestamp: new Date().toISOString(),
            },
        },
    };

    if (getFunctionStatus(updatedElement).length > 0) {
        updatedElement.testProtocolSet = {
            ...updatedElement.testProtocolSet,
            technicalTest: {
                userId: user.id,
                timestamp: new Date().toISOString(),
            },
        };
    }

    if (isRootElement(updatedElement)) {
        updatedElement.introductionStep = {
            ...updatedElement.introductionStep,
            testProtocolSet: {
                ...updatedElement.introductionStep.testProtocolSet,
                professionalTest: {
                    userId: user.id,
                    timestamp: new Date().toISOString(),
                },
            },
        };
        updatedElement.summaryStep = {
            ...updatedElement.summaryStep,
            testProtocolSet: {
                ...updatedElement.summaryStep.testProtocolSet,
                professionalTest: {
                    userId: user.id,
                    timestamp: new Date().toISOString(),
                },
            },
        };
        updatedElement.submitStep = {
            ...updatedElement.submitStep,
            testProtocolSet: {
                ...updatedElement.submitStep.testProtocolSet,
                professionalTest: {
                    userId: user.id,
                    timestamp: new Date().toISOString(),
                },
            },
        };
    }

    if (isRootElement(updatedElement)) {
        updatedElement.children = updatedElement.children.map(child => markAsTestedRecursively(user, child));
    } else if (isAnyElementWithChildren(updatedElement)) {
        updatedElement.children = updatedElement.children.map(child => markAsTestedRecursively(user, child));
    }

    return updatedElement;
}