import React from 'react';
import {type StepElement} from '../../models/elements/steps/step-element';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {ElementType} from '../../data/element-type/element-type';
import {type BaseViewProps} from '../../views/base-view';

export function StepComponentView({
    allElements,
    element,
}: BaseViewProps<StepElement, void>): JSX.Element {
    return (
        <ViewDispatcherComponent
            allElements={allElements}
            element={{
                ...element,
                type: ElementType.Container,
                storeLink: null,
            }}
        />
    );
}
