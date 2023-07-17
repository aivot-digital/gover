import {Container} from '@mui/material';
import React from 'react';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {AlertComponent} from '../../../components/alert/alert-component';

export function NotFound(): JSX.Element {
    return (
        <>
            <MetaElement
                title="Seite nicht gefunden"
            />

            <Container
                sx={{
                    mt: 5,
                }}
            >
                <AlertComponent
                    color="error"
                    title="Seite nicht gefunden"
                >
                    Die angeforderte Seite konnte nicht gefunden werden.
                </AlertComponent>
            </Container>
        </>
    );
}
