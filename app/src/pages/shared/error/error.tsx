import {Container} from '@mui/material';
import React, {useEffect} from 'react';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {AlertComponent} from '../../../components/alert/alert-component';
import {useRouteError} from 'react-router-dom';
import {useAppSelector} from '../../../hooks/use-app-selector';

export function Error() {
    const state = useAppSelector((state) => state);
    const error = useRouteError();

    useEffect(() => {
        console.error(error);
        console.error(state);
    }, [error]);

    return (
        <>
            <MetaElement
                title="Fehler"
            />

            <Container
                sx={{
                    mt: 5,
                }}
            >
                <AlertComponent
                    color="error"
                    title="Es ist ein Fehler aufgetreten"
                >
                    Es ist ein Fehler aufgetreten. Bitte versuchen Sie es später erneut.
                </AlertComponent>
            </Container>
        </>
    );
}
