import {type BaseViewProps} from './base-view';
import React, {useMemo} from 'react';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {type SummaryLayoutElement} from '../models/elements/form/layout/summary-layout-element';
import {SummaryDispatcherComponent} from '../components/summary-dispatcher.component';
import {Stack} from '@mui/material';

export function SummaryLayoutView(props: BaseViewProps<SummaryLayoutElement, any>) {
    const {
        element,
        isDeriving,
    } = props;

    const {
        children,
    } = element;

    // TODO: Create derivation and busy state
    const pass = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <Stack gap={1}>
            {
                children
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
        </Stack>
    );
}
