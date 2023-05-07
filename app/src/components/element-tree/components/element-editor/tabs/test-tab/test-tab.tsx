import {Box, Checkbox, FormControlLabel, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {useSelector} from 'react-redux';
import {format} from 'date-fns';
import {User} from '../../../../../../models/entities/user';
import {UsersService} from '../../../../../../services/users.service';
import {getFunctionStatus} from '../../../../../../utils/function-status-utils';
import {UserRole} from '../../../../../../data/user-role';
import {selectUser} from '../../../../../../slices/user-slice';
import {TestTabProps} from './test-tab-props';
import {AnyElement} from '../../../../../../models/elements/any-element';
import {TestProtocol as TestProtocolModel} from "../../../../../../models/lib/test-protocol";

export function TestTab<T extends AnyElement>({elementModel, onPatch}: TestTabProps<T>) {
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
                    sx={{mt: 1, mb: 1}}
                >
                    Eine Überprüfung der Umsetzung von vorab definierten fachlichen Anforderungen.
                </Typography>
                <FormControlLabel
                    control={
                        <Checkbox
                            checked={elementModel.testProtocolSet?.professionalTest != null}
                            onChange={event => onPatch({
                                ...elementModel.testProtocolSet,
                                professionalTest: user != null && event.target.checked ? {
                                    userId: user.id,
                                    timestamp: new Date().toISOString(),
                                } : undefined
                            })}
                        />
                    }
                    label={elementModel.testProtocolSet?.professionalTest != null ? 'Fachliche Prüfung erfolgreich' : 'Ich habe die Fachliche Prüfung erfolgreich durchgeführt'}
                />

                {
                    elementModel.testProtocolSet?.professionalTest != null &&
                    <TestProtocol {...elementModel.testProtocolSet.professionalTest} />
                }
            </Box>
            {
                getFunctionStatus(elementModel) != null &&
                <Box>
                    <Typography
                        variant="h6"
                        sx={{mt: 4}}
                    >
                        Technische Prüfung
                    </Typography>
                    <Typography
                        variant="body1"
                        sx={{mt: 1, mb: 1}}
                    >
                        Eine Überprüfung der technischen Implementierung von individuellen Funktionen.
                    </Typography>
                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={elementModel.testProtocolSet?.technicalTest != null}
                                onChange={event => onPatch({
                                    ...elementModel.testProtocolSet,
                                    technicalTest: user != null && event.target.checked ? {
                                        userId: user.id,
                                        timestamp: new Date().toISOString(),
                                    } : undefined,
                                })}
                            />
                        }
                        label={elementModel.testProtocolSet?.technicalTest ? 'Technische Prüfung erfolgreich' : 'Ich habe die Technische Prüfung erfolgreich durchgeführt'}
                    />
                    {
                        elementModel.testProtocolSet?.technicalTest  != null &&
                        <TestProtocol {...elementModel.testProtocolSet.technicalTest} />
                    }
                </Box>
            }
        </Box>
    );
}

function TestProtocol(protocol: TestProtocolModel) {
    const [user, setUser] = useState<User>();

    useEffect(() => {
        UsersService.retrieve(protocol.userId)
            .then(user => {
                if (user) {
                    setUser(user);
                } else {
                    setUser({
                        id: protocol.userId,
                        name: 'Unbekannter Nutzer',
                        email: '',
                        active: false,
                        role: UserRole.Editor,
                    });
                }
            });
    }, [protocol.userId]);

    const timestamp = new Date(protocol.timestamp);

    if (user == null) {
        return null;
    }

    return (
        <Typography>
            Geprüft von <Typography
            component="span"
            color="primary"
        >{user.name}</Typography> am {format(timestamp, 'dd.MM.yyyy')} um {format(timestamp, 'HH:mm')} Uhr
        </Typography>
    );
}
