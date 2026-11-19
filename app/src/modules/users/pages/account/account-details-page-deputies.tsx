import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import {selectUser} from '../../../../slices/user-slice';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {RepresentedUsersList} from '../../components/represented-users-list';

export function AccountDetailsPageDeputies() {
    const user = useAppSelector(selectUser);

    return (
        <Box sx={{pt: 2}}>
            <Typography
                variant="h5"
                sx={{mb: 1}}
            >
                Von Ihnen vertretene Mitarbeiter:innen
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Eine Übersicht der Mitarbeiter:innen, für die Sie als Stellvertreter:in eingetragen sind. Während einer
                aktiven Vertretung übernehmen Sie deren Aufgaben und erhalten deren Berechtigungen.
            </Typography>

            {user != null && (
                <RepresentedUsersList
                    deputyUserId={user.id}
                    emptyDescription="Sie sind derzeit für niemanden als Stellvertreter:in eingetragen."
                />
            )}
        </Box>
    );
}
