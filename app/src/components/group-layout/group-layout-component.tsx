import React from 'react';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {Grid} from '@mui/material';
import {type GroupLayoutComponentProps} from './group-layout-component-props';

export function GroupLayoutComponent(props: GroupLayoutComponentProps): JSX.Element {
    return (
        <Grid
            container
            spacing={2}
        >
            {
                props.children.map((child, index) => (
                    <ViewDispatcherComponent
                        allElements={props.allElements}
                        key={index}
                        element={child}
                        idPrefix={props.idPrefix}
                        isBusy={props.isBusy}
                        isDeriving={props.isDeriving}
                        valueOverride={props.valueOverride}
                        errorsOverride={props.errorsOverride}
                        mode={props.mode}
                    />
                ))
            }
        </Grid>
    );
}

