import {PageWrapperProps} from "./page-wrapper-props";
import React, {PropsWithChildren} from "react";
import {MetaElement} from "../meta-element/meta-element";
import {AppToolbar} from "../app-toolbar/app-toolbar";
import {Backdrop, CircularProgress, Container} from "@mui/material";
import {AppFooter} from "../app-footer/app-footer";
import {AppMode} from "../../data/app-mode";

export function PageWrapper(props: PropsWithChildren<PageWrapperProps>) {
    return (
        <>
            <MetaElement title={props.title}/>

            <AppToolbar
                title={props.title}
                actions={props.toolbarActions}
            />

            <Container
                sx={{
                    mt: 4,
                    mb: 10,
                    minHeight: '100vh',
                }}
            >
                {props.children}
            </Container>

            <Backdrop
                open={props.isLoading ?? false}
                invisible
            >
                <CircularProgress color="inherit"/>
            </Backdrop>

            <AppFooter mode={AppMode.Staff}/>
        </>
    );
}