import React, {type PropsWithChildren} from 'react';
import {type LoadingWrapperProps} from './loading-wrapper-props';
import {Backdrop, CircularProgress} from '@mui/material';
import {alpha} from '@mui/material/styles';

export function LoadingWrapper(props: PropsWithChildren<LoadingWrapperProps>) {
    return (
        <>
            {
                !(props.isLoading ?? false) &&
                props.children
            }

            <Backdrop
                open={props.isLoading ?? false}
                sx={{
                    backgroundColor: (theme) => alpha(theme.palette.background.default, 0.65),
                    zIndex: 999,
                }}
            >
                <CircularProgress color="inherit"/>
            </Backdrop>
        </>
    );
}
