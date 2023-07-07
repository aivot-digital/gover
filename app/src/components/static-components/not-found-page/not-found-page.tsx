import React from 'react';
import { Alert, AlertTitle, Container, Typography } from '@mui/material';

export function NotFoundPage({ title, msg }: { title?: string, msg?: string }): JSX.Element {
    return (
        <Container>
            <Alert
                severity="error"
                sx={ {
                    mt: 8,
                } }
            >
                <AlertTitle>
                    {
                        title != null ? title : 'Formular nicht gefunden'
                    }
                </AlertTitle>
                <Typography>
                    {
                        msg != null ? msg : 'Das von Ihnen aufgerufene Formular konnte nicht gefunden werden.'
                    }
                </Typography>
            </Alert>
        </Container>
    );
}
