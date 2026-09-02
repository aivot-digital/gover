import {SearchBaseDialog} from '../../../dialogs/search-base-dialog/search-base-dialog';
import {User} from '../models/user';
import {useEffect, useMemo, useState} from 'react';
import {UsersApiService} from '../users-api-service';
import {resolveUserName} from '../utils/resolve-user-name';
import {SearchBaseDialogTabProps} from '../../../dialogs/search-base-dialog/search-base-dialog-tab-props';
import {useHasSystemPermission} from '../../permissions/hooks/use-permissions';
import {Permission} from '../../../data/permissions/permission';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {isApiError} from '../../../models/api-error';

interface SelectUserDialogProps {
    open: boolean;
    onClose: () => void;
    onSelect: (user: User) => void;
    idsToExclude?: string[];
}

export function SelectUserDialog(props: SelectUserDialogProps) {
    const dispatch = useAppDispatch();
    const canReadUsers = useHasSystemPermission(Permission.USER_READ);

    const [users, setUsers] = useState<User[]>([]);
    const [isLoadingUsers, setIsLoadingUsers] = useState(false);
    const [usersAccessDenied, setUsersAccessDenied] = useState(false);

    const {
        open,
        onClose,
        onSelect,
        idsToExclude,
    } = props;

    useEffect(() => {
        if (!open) {
            return;
        }

        setUsersAccessDenied(false);

        if (!canReadUsers) {
            setUsers([]);
            setIsLoadingUsers(false);
            setUsersAccessDenied(true);
            return;
        }

        let isCancelled = false;

        setIsLoadingUsers(true);

        new UsersApiService()
            .listAll({
                disabledInIdp: false,
                deletedInIdp: false,
            })
            .then((data) => {
                if (!isCancelled) {
                    setUsers(data.content);
                }
            })
            .catch((err) => {
                if (isCancelled) {
                    return;
                }

                setUsers([]);
                if (isApiError(err) && err.status === 403) {
                    setUsersAccessDenied(true);
                } else {
                    dispatch(showApiErrorSnackbar(err, 'Mitarbeiter:innen konnten nicht geladen werden.'));
                }
            })
            .finally(() => {
                if (!isCancelled) {
                    setIsLoadingUsers(false);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [canReadUsers, dispatch, open]);

    const excludedUsers = useMemo(() => {
        if (idsToExclude == null || idsToExclude.length === 0) {
            return users;
        } else {
            return users.filter((user) => !idsToExclude.includes(user.id));
        }
    }, [users, idsToExclude]);

    const tabs: SearchBaseDialogTabProps<User>[] = useMemo(() => [{
        title: 'Mitarbeiter:innen',
        searchPlaceholder: 'Mitarbeiter:in suchen',
        onSelect: onSelect,
        options: excludedUsers,
        primaryTextKey: resolveUserName,
        secondaryTextKey: 'email',
        searchKeys: ['fullName'],
        getId: (user: User) => user.id,
        noOptionsMessage: usersAccessDenied
            ? 'Keine Berechtigung zur Einsicht von Mitarbeiter:innen.'
            : isLoadingUsers
                ? 'Mitarbeiter:innen werden geladen...'
                : 'Keine Mitarbeiter:innen verfügbar.',
    }], [onSelect, excludedUsers, isLoadingUsers, usersAccessDenied]);

    return (
        <SearchBaseDialog
            open={open}
            onClose={onClose}
            title="Mitarbeiter:in auswählen"
            tabs={tabs}
        />
    );
}
