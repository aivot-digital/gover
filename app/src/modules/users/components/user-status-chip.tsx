import Chip from '@mui/material/Chip';

interface UserStatusChipProps {
    userDeletedInIdp: boolean;
    userEnabled: boolean;
}

export function UserStatusChip(props: UserStatusChipProps) {
    if (props.userDeletedInIdp) {
        return (
            <Chip
                label="Gelöscht"
                color="error"
                variant="outlined"
                size="small"
                title="Diese Mitarbeiter:in wurde im Identity Provider gelöscht und kann sich nicht anmelden."
            />
        );
    }

    if (props.userEnabled) {
        return (
            <Chip
                label="Aktiv"
                variant="outlined"
                size="small"
            />
        );
    }

    return (
        <Chip
            label="Inaktiv"
            color="warning"
            variant="outlined"
            size="small"
            title="Diese Mitarbeiter:in ist inaktiv und kann sich nicht anmelden."
        />
    );
}
