import {type BaseViewProps} from './base-view';
import {type GroupLayout} from '../models/elements/form/layout/group-layout';
import React, {useMemo} from 'react';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {ViewDispatcherComponent} from '../components/view-dispatcher.component';
import Grid from '@mui/material/Grid';

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
                        rootElement={props.rootElement}
                        allElements={props.allElements}
                        key={index}
                        element={child}
                        isBusy={props.isBusy}
                        isDeriving={props.isDeriving || pass}
                        mode={props.mode}
                        elementData={props.elementData}
                        onElementDataChange={props.onElementDataChange}
                        onElementBlur={props.onElementBlur}
                        scrollContainerRef={props.scrollContainerRef}
                        disableVisibility={props.disableVisibility}
                        derivationTriggerIdQueue={props.derivationTriggerIdQueue}
                    />
                ))
            }
        </Grid>
    );
}
