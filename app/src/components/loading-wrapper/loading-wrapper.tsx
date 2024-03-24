import React, {type PropsWithChildren} from 'react';
import {type LoadingWrapperProps} from './loading-wrapper-props';
import {Backdrop, CircularProgress} from '@mui/material';

export function LoadingWrapper(props: PropsWithChildren<LoadingWrapperProps>): JSX.Element {
    return (
        <>
            {
                !(props.isLoading ?? false) &&
                props.children
            }

            <Backdrop
                open={props.isLoading ?? false}
                sx={{
                    backgroundColor: '#ffffff55',
                    zIndex: 999,
                }}
            >
                <CircularProgress color="inherit"/>
            </Backdrop>
        </>
    );
}
