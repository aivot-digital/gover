import React, {useMemo} from 'react';
import {AnyElement} from '../models/elements/any-element';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {summaries as Summaries} from '../summaries';
import {BaseSummaryProps} from '../summaries/base-summary';
import {AuthoredElementValues, DerivedRuntimeElementData} from '../models/element-data';
import {resolveOverride, resolveValueForResolvedOverride, resolveVisibility} from '../utils/element-data-utils';

interface DispatcherComponentProps {
    element: AnyElement;
    allowStepNavigation?: boolean;
    showTechnical?: boolean;
    authoredElementValues: AuthoredElementValues;
    derivedData: DerivedRuntimeElementData;
}

export function SummaryDispatcherComponent(props: DispatcherComponentProps) {
    const {
        element: initialElement,
        allowStepNavigation,
        showTechnical,
        authoredElementValues,
        derivedData,
    } = props;

    const element: AnyElement = useMemo(() => {
        return resolveOverride(initialElement, derivedData);
    }, [initialElement, derivedData]);

    const value: any = useMemo(() => {
        return resolveValueForResolvedOverride(element, authoredElementValues, derivedData);
    }, [element, authoredElementValues, derivedData]);

    const Component = useMemo(() => {
        return Summaries[element.type];
    }, [element.type]);

    const isVisible = useMemo(() => {
        if (isAnyInputElement(element) && element.technical && showTechnical !== true) {
            return false;
        }

        return resolveVisibility(element, derivedData);
    }, [element, showTechnical, derivedData]);

    const viewProps: BaseSummaryProps<typeof element, typeof value> = useMemo(() => ({
        model: element,
        value: value,
        allowStepNavigation: allowStepNavigation,
        showTechnical: showTechnical,
        authoredElementValues: authoredElementValues,
        derivedData: derivedData,
    }), [element, value, allowStepNavigation, showTechnical, authoredElementValues, derivedData]);

    if (Component == null || !isVisible) {
        return null;
    }

    return (
        <div id={element.id}>
            <Component {...viewProps} />
        </div>
    );
}
