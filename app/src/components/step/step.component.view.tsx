import React from 'react';
import {type StepElement} from '../../models/elements/steps/step-element';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {type BaseViewProps} from '../../views/base-view';
import Grid from '@mui/material/Grid';

export function StepComponentView(props: BaseViewProps<StepElement, void>) {
    const {
        rootElement,
        allElements,
        element,
        isBusy,
        isDeriving,
        scrollContainerRef,
        mode,
        authoredElementValues,
        derivedData,
        onAuthoredElementValuesChange,
        onElementBlur,
        onDerivedDataChange,
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
                        rootElement={rootElement}
                        key={child.id}
                        allElements={allElements}
                        element={child}
                        isBusy={isBusy}
                        isDeriving={isDeriving}
                        scrollContainerRef={scrollContainerRef}
                        mode={mode}
                        authoredElementValues={authoredElementValues}
                        derivedData={derivedData}
                        onAuthoredElementValuesChange={onAuthoredElementValuesChange}
                        onElementBlur={onElementBlur}
                        onDerivedDataChange={onDerivedDataChange}
                        derivationTriggerIdQueue={derivationTriggerIdQueue}
                        disableVisibility={disableVisibility}
                    />
                ))
            }
        </Grid>

    );
}
