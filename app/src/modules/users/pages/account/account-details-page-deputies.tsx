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
                Personen, die Sie vertreten
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Während einer aktiven Stellvertretung erhalten Sie die Berechtigungen der vertretenen Person und können
                deren Aufgaben bearbeiten.
            </Typography>

            {user != null && (
                <RepresentedUsersList deputyUserId={user.id}/>
            )}
        </Box>
    );
}
