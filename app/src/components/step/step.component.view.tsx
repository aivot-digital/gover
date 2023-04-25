import {StepElement} from '../../models/elements/steps/step-element';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {ElementType} from '../../data/element-type/element-type';
import {BaseViewProps} from '../_lib/base-view-props';

export function StepComponentView({element}: BaseViewProps<StepElement, void>) {
    return (
        <ViewDispatcherComponent
            element={{...element, type: ElementType.Container}}
        />
    );
}
