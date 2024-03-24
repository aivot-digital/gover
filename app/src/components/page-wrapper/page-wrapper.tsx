import {type PageWrapperProps} from './page-wrapper-props';
import React, {type PropsWithChildren, useState} from 'react';
import {MetaElement} from '../meta-element/meta-element';
import {AppToolbar} from '../app-toolbar/app-toolbar';
import {Container} from '@mui/material';
import {AppFooter} from '../app-footer/app-footer';
import {AppMode} from '../../data/app-mode';
import {AlertComponent} from '../alert/alert-component';
import {LoadingWrapper} from '../loading-wrapper/loading-wrapper';

export function PageWrapper(props: PropsWithChildren<PageWrapperProps>): JSX.Element {
    const [toolbarHeight, setToolbarHeight] = useState<number>(0);
    const updateToolbarHeight = (height: number) => {
        setToolbarHeight(height);
    };
    return (
        <LoadingWrapper isLoading={props.isLoading}>
            <MetaElement title={props.title}/>

            <AppToolbar
                title={props.title}
                actions={props.toolbarActions}
                updateToolbarHeight={updateToolbarHeight}
            />

            <Container
                sx={{
                    mt: 4,
                    mb: 10,
                    minHeight: 'calc(100vh - ' + toolbarHeight + 'px)',
                }}
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

            <AppFooter mode={AppMode.Staff}/>
        </LoadingWrapper>
    );
}
