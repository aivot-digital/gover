import React, {useMemo} from 'react';
import {AnyElement} from '../models/elements/any-element';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {summaries as Summaries} from '../summaries';
import {BaseSummaryProps} from '../summaries/base-summary';
import {ElementData} from '../models/element-data';
import {resolveOverride, resolveValueForResolvedOverride, resolveVisibility} from '../utils/element-data-utils';

interface DispatcherComponentProps {
    element: AnyElement;
    allowStepNavigation?: boolean;
    showTechnical?: boolean;
    elementData: ElementData;
}

export function SummaryDispatcherComponent(props: DispatcherComponentProps) {
    const {
        element: initialElement,
        allowStepNavigation,
        showTechnical,
        elementData,
    } = props;

    const element: AnyElement = useMemo(() => {
        return resolveOverride(initialElement, elementData);
    }, [initialElement, elementData]);

    const value: any = useMemo(() => {
        return resolveValueForResolvedOverride(element, elementData);
    }, [element, elementData]);

    const Component = useMemo(() => {
        return Summaries[element.type];
    }, [element.type]);

    const isVisible = useMemo(() => {
        if (isAnyInputElement(element) && element.technical && showTechnical !== true) {
            return false;
        }

        return resolveVisibility(element, elementData);
    }, [element, showTechnical, elementData]);

    const viewProps: BaseSummaryProps<typeof element, typeof value> = useMemo(() => ({
        model: element,
        value: value,
        allowStepNavigation: allowStepNavigation,
        showTechnical: showTechnical,
        elementData: elementData,
    }), [element, value, allowStepNavigation, showTechnical, elementData]);

    if (Component == null || !isVisible) {
        return null;
    }

    return (
        <div id={element.id}>
            <Component {...viewProps} />
        </div>
    );
}
