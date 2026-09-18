import {AlertComponent} from './alert/alert-component';
import {Typography} from '@mui/material';

interface InsufficientPermissionAlertProps {
    message: string;
    requiredPermissions: string[];
}

export function InsufficientPermissionAlert(props: InsufficientPermissionAlertProps) {
    const {
        requiredPermissions,
        message,
    } = props;

    return (
        <AlertComponent color="warning">
            <Typography
                variant="body2"
                color="textSecondary"
            >
                {message}
                Bitte wenden Sie sich an Ihre Administrator:in um die notwendigen Berechtigungen zu erhalten.
            </Typography>
            <Typography>
                Benötigte Berechtigungen:
                <ul>
                    {
                        requiredPermissions.map(permission => (
                            <li key={permission}>
                                <code>{permission}</code>
                            </li>
                        ))
                    }
                </ul>
            </Typography>
        </AlertComponent>
    );
}