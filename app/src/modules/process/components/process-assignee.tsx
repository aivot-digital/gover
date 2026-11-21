import {Box} from '@mui/material';
import {ProcessEmptyValue} from './process-detail-values';
import {type ReactNode, useEffect, useState} from 'react';
import {UsersApiService} from '../../users/users-api-service';
import {resolveUserName} from '../../users/utils/resolve-user-name';
import {type User} from '../../users/models/user';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectUser} from '../../../slices/user-slice';
import {useHasSystemPermission} from '../../permissions/hooks/use-permissions';
import {Permission} from '../../../data/permissions/permission';
import {type ProcessDeputy} from '../entities/process-deputy';
import {getProcessDeputyLabel} from '../utils/process-deputy-label';

interface ProcessAssigneeProps {
    userId: string | null;
    deputies?: ProcessDeputy[];
}

export function ProcessAssignee({userId, deputies = []}: ProcessAssigneeProps): ReactNode {
    if (deputies.length === 0) return <AssigneeName userId={userId} />;
    return (
        <Box component="span" sx={{display: 'block'}}>
            <AssigneeName userId={userId} />
            <Box component="span" sx={{display: 'block', typography: 'body2', color: 'text.secondary'}}>
                {getProcessDeputyLabel(deputies)}
            </Box>
        </Box>
    );
}

function AssigneeName({userId}: {userId: string | null}): ReactNode {
    const currentUser = useAppSelector(selectUser);
    const canReadUsers = useHasSystemPermission(Permission.USER_READ);
    const [lookup, setLookup] = useState<{id: string; user: User | null} | null>(null);
    const isSelf = userId != null && currentUser?.id === userId;

    useEffect(() => {
        let cancelled = false;
        setLookup(null);
        if (userId != null && !isSelf && canReadUsers) {
            new UsersApiService().retrieve(userId).then(
                (user) => {
                    if (!cancelled) {
                        setLookup({
                            id: userId,
                            user,
                        });
                    }
                },
                () => {
                    if (!cancelled) {
                        setLookup({
                            id: userId,
                            user: null,
                        });
                    }
                },
            );
        }
        return () => {
            cancelled = true;
        };
    }, [userId, isSelf, canReadUsers]);

    if (userId == null) return <ProcessEmptyValue>Nicht zugewiesen</ProcessEmptyValue>;
    if (isSelf) return resolveUserName(currentUser);
    if (!canReadUsers) return <ProcessEmptyValue>Zugewiesen · Name nicht verfügbar</ProcessEmptyValue>;
    if (lookup?.id !== userId) return <ProcessEmptyValue>Name wird geladen …</ProcessEmptyValue>;
    return lookup.user == null ? (
        <ProcessEmptyValue>Zugewiesen · Name nicht verfügbar</ProcessEmptyValue>
    ) : (
        resolveUserName(lookup.user)
    );
}
