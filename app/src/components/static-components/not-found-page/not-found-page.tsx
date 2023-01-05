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
                    Antrag nicht gefunden
                </AlertTitle>
                <Typography>
                    Der von Ihnen aufgerufene Antrag konnte nicht gefunden werden.
                </Typography>
            </Alert>
        </Container>
    );
}