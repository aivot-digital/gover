import React, {PropsWithChildren} from 'react';
import {Alert, AlertTitle, Box, Container, Typography} from '@mui/material';

interface NotFoundPageProps {
    title?: string;
    msg?: string;
}

export function NotFoundPage(props: PropsWithChildren<NotFoundPageProps>): JSX.Element {
    return (
        <Container>
            <Alert
                severity="error"
                sx={{
                    mt: 8,
                }}
            >
                <AlertTitle>
                    {
                        props.title != null ? props.title : 'Seite nicht gefunden'
                    }
                </AlertTitle>
                <Typography>
                    {
                        props.msg != null ? props.msg : 'Die von Ihnen aufgerufene Seite konnte nicht gefunden werden.'
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
