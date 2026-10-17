import {type BaseSummaryProps} from './base-summary';
import {SummaryDispatcherComponent} from '../components/summary-dispatcher.component';
import {SummaryLayoutElement} from '../models/elements/form/layout/summary-layout-element';
import {normalizeSummaryLayoutElement} from '../utils/normalize-summary-layout-elements';

export function SummaryLayoutSummary(props: BaseSummaryProps<SummaryLayoutElement, void>) {
    const {
        model,
        showTechnical,
        allowStepNavigation,
        authoredElementValues,
        derivedData,
    } = props;

    const {
        children,
    } = model;

    const normalizedChildren = children.map(normalizeSummaryLayoutElement);

    return (
        <>
            {
                (normalizedChildren ?? [])
                    .map((model) => (
                        <SummaryDispatcherComponent
                            key={model.id}
                            element={model}
                            showTechnical={showTechnical}
                            allowStepNavigation={allowStepNavigation}
                            authoredElementValues={authoredElementValues}
                            derivedData={derivedData}
                        />
                    ))
            }
        </>
    );
}
