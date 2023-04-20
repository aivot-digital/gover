import {Alert, AlertTitle} from '@mui/material';
import {AlertElement} from '../../models/elements/./form/./content/alert-element';
import {BaseViewProps} from '../_lib/base-view-props';

export function AlertComponentView({element}: BaseViewProps<AlertElement, void>) {
    return (
        <Alert
            severity={element.alertType ?? 'info'}
            sx={{my: 4}}
        >
            {
                element.title &&
                <AlertTitle>
                    {element.title}
                </AlertTitle>
            }
            {
                element.text ?? ''
            }
        </Alert>
    );
}
