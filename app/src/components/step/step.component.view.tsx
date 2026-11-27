import React from 'react';
import {type StepElement} from '../../models/elements/steps/step-element';
import {type BaseViewProps} from '../../views/base-view';
import Grid from '@mui/material/Grid';
import {ViewDispatcherComponent} from '../view-dispatcher/view-dispatcher.component';

export function StepComponentView(props: BaseViewProps<StepElement, void>) {
    const {
        element,
    } = props;

    const {
        children,
    } = element;

    return (
        <Grid
            container
            spacing={2}
            sx={{mt: 0}}
        >
            {
                children != null &&
                children
                    .map((child) => (
                        <ViewDispatcherComponent
                            {...props}
                            key={child.id}
                            element={child}
                        />
                    ))
            }
        </Grid>

    );
}
