import React, {PropsWithChildren} from 'react';
import {Alert, AlertTitle, Box, Container, Typography} from '@mui/material';

interface NotFoundPageProps {
    title?: string;
    msg?: string;
}

export function NotFoundPage(props: PropsWithChildren<NotFoundPageProps>): JSX.Element {
    return (
        <Container sx={{display: 'flex', justifyContent: 'center'}}>
            <Alert
                severity="info"
                sx={{
                    mt: 8,
                    width: '100%',
                    maxWidth: 620,
                }}
            >
                <AlertTitle>
                    {
                        props.title != null ? props.title : 'Fehler 404 – Seite nicht gefunden'
                    }
                </AlertTitle>
                <Typography>
                    {
                        props.msg != null ? props.msg : 'Die von Ihnen aufgerufene Seite konnte nicht (mehr) gefunden werden.'
                    }
                </Typography>
            </Alert>

            {
                props.children != null &&
                <Box sx={{mt: 4}}>
                    {props.children}
                </Box>
            }
        </Container>
    );
}
