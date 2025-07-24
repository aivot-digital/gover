import React from 'react';
import {type StepElement} from '../../models/elements/steps/step-element';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {ElementType} from '../../data/element-type/element-type';
import {type BaseViewProps} from '../../views/base-view';

export function StepComponentView({
    allElements,
    element,
    isBusy,
    isDeriving,
    valueOverride,
    errorsOverride,
    visibilitiesOverride,
    overridesOverride,
    scrollContainerRef,
    idPrefix,
    mode,
}: BaseViewProps<StepElement, void>) {
    return (
        <ViewDispatcherComponent
            allElements={allElements}
            element={{
                ...element,
                type: ElementType.Container,
                storeLink: null,
            }}
            isBusy={isBusy}
            isDeriving={isDeriving}
            valueOverride={valueOverride}
            errorsOverride={errorsOverride}
            visibilitiesOverride={visibilitiesOverride}
            overridesOverride={overridesOverride}
            idPrefix={idPrefix}
            scrollContainerRef={scrollContainerRef}
            mode={mode}
        />
    );
}
