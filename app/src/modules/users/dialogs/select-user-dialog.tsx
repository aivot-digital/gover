import {SearchBaseDialog} from '../../../dialogs/search-base-dialog/search-base-dialog';
import {User} from '../models/user';
import {useEffect, useMemo, useState} from 'react';
import {useApi} from '../../../hooks/use-api';
import {UsersApiService} from '../users-api-service';
import {resolveUserName} from '../utils/resolve-user-name';
import {SearchBaseDialogTabProps} from '../../../dialogs/search-base-dialog/search-base-dialog-tab-props';

interface SelectUserDialogProps {
    open: boolean;
    onClose: () => void;
    onSelect: (user: User) => void;
    idsToExclude?: string[];
}

export function SelectUserDialog(props: SelectUserDialogProps) {
    const api = useApi();
    const [users, setUsers] = useState<User[]>([]);

    const {
        open,
        onClose,
        onSelect,
        idsToExclude,
    } = props;

    useEffect(() => {
        new UsersApiService(api)
            .listAll({
                disabledInIdp: false,
                deletedInIdp: false,
            })
            .then((data) => setUsers(data.content));
    }, [api]);

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
    }], [onSelect, excludedUsers]);

    return (
        <SearchBaseDialog
            open={open}
            onClose={onClose}
            title="Mitarbeiter:in auswählen"
            tabs={tabs}
        />
    );
}