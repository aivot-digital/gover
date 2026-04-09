import {type BaseViewProps} from './base-view';
import React, {useMemo} from 'react';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {type SummaryLayoutElement} from '../models/elements/form/layout/summary-layout-element';
import {SummaryDispatcherComponent} from '../components/summary-dispatcher.component';
import {normalizeSummaryLayoutElement} from '../utils/normalize-summary-layout-elements';

export function SummaryLayoutView(props: BaseViewProps<SummaryLayoutElement, any>) {
    const {
        element,
        isDeriving,
    } = props;

    const {
        children,
    } = element;

    const normalizedChildren = useMemo(() => {
        return children.map(normalizeSummaryLayoutElement);
    }, [children]);

    // TODO: Create derivation and busy state
    const pass = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <>
            {
                normalizedChildren
                    .map((child) => (
                        <SummaryDispatcherComponent
                            key={child.id}
                            element={child}
                            allowStepNavigation={false}
                            showTechnical={false}
                            authoredElementValues={props.authoredElementValues}
                            derivedData={props.derivedData}
                        />
                    ))
            }
        </>
    );
}
