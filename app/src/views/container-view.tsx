import {type BaseViewProps} from './base-view';
import {type GroupLayout} from '../models/elements/form/layout/group-layout';
import React, {useMemo} from 'react';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import Grid from '@mui/material/Grid';
import {ViewDispatcherComponent} from '../components/view-dispatcher/view-dispatcher.component';

export function ContainerView(props: BaseViewProps<GroupLayout, any>) {
    const {
        element,
        isDeriving,
    } = props;

    const {
        children,
    } = element;

    const pass = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <Grid
            container
            spacing={2}
        >
            {
                children.map((child, index) => (
                    <ViewDispatcherComponent
                        {...props}
                        key={index}
                        element={child}
                        isDeriving={props.isDeriving || pass}
                    />
                ))
            }
        </Grid>
    );
}
