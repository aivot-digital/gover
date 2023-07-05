import React from 'react';
import {Alert, AlertTitle, Container, Typography} from '@mui/material';

export function NotFoundPage() {
    return (
        <Container>
            <Alert
                severity="error"
                sx={{mt: 8}}
            >
                <AlertTitle>
                    Formular nicht gefunden
                </AlertTitle>
                <Typography>
                    Das von Ihnen aufgerufene Formular konnte nicht gefunden werden.
                </Typography>
            </Alert>
        </Container>
    );
}