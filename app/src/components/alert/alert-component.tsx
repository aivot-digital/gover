import {Alert, AlertTitle} from '@mui/material';
import {AlertComponentProps} from "./alert-component-props";

export function AlertComponent({title, text, color}: AlertComponentProps) {
    return (
        <Alert
            severity={color ?? 'info'}
            sx={{
                my: 4,
            }}
        >
            {
                title &&
                <AlertTitle>
                    {title}
                </AlertTitle>
            }
            {text}
        </Alert>
    );
}
