import { Box, Checkbox, FormControlLabel, Typography } from '@mui/material';
import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { format, parseISO } from 'date-fns';
import { type User } from '../../../../../../models/entities/user';
import { UsersService } from '../../../../../../services/users-service';
import { getFunctionStatus } from '../../../../../../utils/function-status-utils';
import { selectUser } from '../../../../../../slices/user-slice';
import { type TestTabProps } from './test-tab-props';
import { type AnyElement } from '../../../../../../models/elements/any-element';
import { type TestProtocol as TestProtocolModel } from '../../../../../../models/lib/test-protocol';
import { CheckboxFieldComponent } from '../../../../../checkbox-field/checkbox-field-component';

export function TestTab<T extends AnyElement>(props: TestTabProps<T>): JSX.Element {
    const user = useSelector(selectUser);

    return (
        <Box sx={ {p: 4} }>
            <Box>
                <Typography
                    variant="h6"
                >
                    Fachliche Prüfung
                </Typography>

                <Typography
                    variant="body1"
                    sx={ {
                        mt: 1,
                        mb: 1,
                    } }
                >
                    Eine Überprüfung der Umsetzung von vorab definierten fachlichen Anforderungen.
                </Typography>

                <CheckboxFieldComponent
                    value={ props.elementModel.testProtocolSet?.professionalTest != null }
                    onChange={ (checked) => {
                        props.onPatch({
                            ...props.elementModel.testProtocolSet,
                            professionalTest: user != null && checked ?
                                {
                                    userId: user.id,
                                    timestamp: new Date().toISOString(),
                                } :
                                undefined,
                        });
                    } }
                    label={
                        props.elementModel.testProtocolSet?.professionalTest != null ?
                            'Fachliche Prüfung erfolgreich' :
                            'Ich habe die Fachliche Prüfung erfolgreich durchgeführt'
                    }
                    disabled={ !props.editable }
                />

                {
                    props.elementModel.testProtocolSet?.professionalTest != null &&
                    <TestProtocol { ...props.elementModel.testProtocolSet.professionalTest } />
                }
            </Box>
            {
                getFunctionStatus(props.elementModel).length > 0 &&
                <Box>
                    <Typography
                        variant="h6"
                        sx={ {mt: 4} }
                    >
                        Technische Prüfung
                    </Typography>
                    <Typography
                        variant="body1"
                        sx={ {
                            mt: 1,
                            mb: 1,
                        } }
                    >
                        Eine Überprüfung der technischen Implementierung von individuellen Funktionen.
                    </Typography>
                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={ props.elementModel.testProtocolSet?.technicalTest != null }
                                onChange={ (event) => {
                                    props.onPatch({
                                        ...props.elementModel.testProtocolSet,
                                        technicalTest: user != null && event.target.checked ?
                                            {
                                                userId: user.id,
                                                timestamp: new Date().toISOString(),
                                            } :
                                            undefined,
                                    });
                                } }
                            />
                        }
                        label={ ((props.elementModel.testProtocolSet?.technicalTest) != null) ? 'Technische Prüfung erfolgreich' : 'Ich habe die Technische Prüfung erfolgreich durchgeführt' }
                    />
                    {
                        props.elementModel.testProtocolSet?.technicalTest != null &&
                        <TestProtocol { ...props.elementModel.testProtocolSet.technicalTest } />
                    }
                </Box>
            }
        </Box>
    );
}

function TestProtocol(protocol: TestProtocolModel): JSX.Element | null {
    const [user, setUser] = useState<User>();

    useEffect(() => {
        UsersService
            .retrieve(protocol.userId)
            .then(setUser)
            .catch((err) => {
                console.error(err);
                setUser({
                    id: protocol.userId,
                    name: 'Inaktiver Nutzer',
                    email: '',
                    password: '',
                    active: false,
                    admin: false,
                    created: '',
                    updated: '',
                });
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
        >{ user.name }</Typography> am { format(timestamp, 'dd.MM.yyyy') } um { format(timestamp, 'HH:mm') } Uhr
        </Typography>
    );
}
