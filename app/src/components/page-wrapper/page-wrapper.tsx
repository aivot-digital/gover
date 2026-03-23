import {type PageWrapperProps} from './page-wrapper-props';
import React, {type PropsWithChildren} from 'react';
import {MetaElement} from '../meta-element/meta-element';
import {Box, Container} from '@mui/material';
import {AlertComponent} from '../alert/alert-component';
import {LoadingWrapper} from '../loading-wrapper/loading-wrapper';

export function PageWrapper(props: PropsWithChildren<PageWrapperProps>) {
    return (
        <Box
            sx={{
                backgroundColor: props.background ? '#F6F6F6' : 'transparent',
            }}
        >
            <LoadingWrapper isLoading={props.isLoading}>
                <MetaElement title={props.title} />

                <Container
                    sx={{
                        pt: props.fullHeight ? undefined : 2,
                        pb: props.fullHeight ? undefined : 10,
                    }}
                    maxWidth={props.fullWidth ? false : 'lg'}
                    disableGutters={props.fullWidth}
                >
                    {
                        (props.is404 ?? false) &&
                        <AlertComponent
                            title="Seite nicht gefunden"
                            text="Die aufgerufene Seite scheint nicht zu existieren."
                            color="error"
                        />
                    }

                    {
                        props.error != null &&
                        <AlertComponent
                            title="Es ist ein Fehler aufgetreten"
                            text={props.error}
                            color="error"
                        />
                    }

                    {
                        !(props.is404 ?? false) &&
                        props.error == null &&
                        props.children
                    }
                </Container>
            </LoadingWrapper>
        </Box>
    );
}
