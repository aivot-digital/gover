import React from 'react';
import {type StepElement} from '../../models/elements/steps/step-element';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {type BaseViewProps} from '../../views/base-view';
import Grid from '@mui/material/Grid';

export function StepComponentView(props: BaseViewProps<StepElement, void>) {
    const {
        allElements,
        element,
        isBusy,
        isDeriving,
        scrollContainerRef,
        mode,
        elementData,
        onElementDataChange,
        onElementBlur,
        disableVisibility,
        derivationTriggerIdQueue,
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
                (children ?? []).map((child) => (
                    <ViewDispatcherComponent
                        key={child.id}
                        allElements={allElements}
                        element={child}
                        isBusy={isBusy}
                        isDeriving={isDeriving}
                        scrollContainerRef={scrollContainerRef}
                        mode={mode}
                        elementData={elementData}
                        onElementDataChange={onElementDataChange}
                        onElementBlur={onElementBlur}
                        derivationTriggerIdQueue={derivationTriggerIdQueue}
                        disableVisibility={disableVisibility}
                    />
                ))
            }
        </Grid>

    );
}
