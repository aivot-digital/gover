import {Alert, AlertTitle} from '@mui/material';
import {AlertComponentProps} from "./alert-component-props";
import {PropsWithChildren} from "react";

export function AlertComponent({title, text, color, children}: PropsWithChildren<AlertComponentProps>) {
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
            {children}
        </Alert>
    );
}
